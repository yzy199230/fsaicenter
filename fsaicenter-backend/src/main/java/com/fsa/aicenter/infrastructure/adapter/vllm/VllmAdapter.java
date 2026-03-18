package com.fsa.aicenter.infrastructure.adapter.vllm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import com.fsa.aicenter.infrastructure.adapter.common.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * vLLM适配器实现
 * <p>
 * 支持vLLM本地部署的AI模型，包括非流式和流式调用。
 * vLLM API兼容OpenAI格式，支持高性能推理。
 * </p>
 * <p>
 * vLLM特点：
 * <ul>
 *   <li>高吞吐量：使用PagedAttention技术</li>
 *   <li>连续批处理：支持动态批处理</li>
 *   <li>OpenAI兼容：API格式与OpenAI一致</li>
 *   <li>多模型支持：支持Llama、Mistral、Qwen等开源模型</li>
 * </ul>
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class VllmAdapter implements AiProviderAdapter {

    private static final String DEFAULT_VLLM_API_URL = "http://localhost:8000/v1/chat/completions";
    private static final String DEFAULT_VLLM_EMBEDDING_URL = "http://localhost:8000/v1/embeddings";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String EMPTY_RESPONSE = "空响应";
    private static final String ERROR_FIELD = "error";
    private static final String MESSAGE_FIELD = "message";
    private static final String MODEL_FIELD = "model";
    private static final String CHOICES_FIELD = "choices";
    private static final String DELTA_FIELD = "delta";
    private static final String CONTENT_FIELD = "content";

    private final OkHttpClient httpClient;

    public VllmAdapter(@Qualifier("aiOkHttpClient") OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LOCAL;  // vLLM是本地部署
    }

    @Override
    public String getProviderCode() {
        return "vllm";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("vLLM非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, request, false);

            // 同步调用（在Reactor线程池中）
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw handleError(response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw AiProviderException.serverError(getProviderCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("vLLM响应: {}", bodyString);

                return parseResponse(bodyString);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("vLLM调用失败", e);
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<AiStreamChunk> callStream(AiModel model, AiRequest request) {
        return Flux.create(sink -> {
            log.debug("vLLM流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, request, true);
            Call call = httpClient.newCall(httpRequest);

            // 注册取消回调
            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("vLLM流式调用已取消");
                }
            });

            // 异步调用
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        try {
                            AiProviderException exception = handleError(response);
                            sink.error(exception);
                        } catch (IOException e) {
                            sink.error(AiProviderException.networkError(
                                    getProviderCode(), "Failed to read error response", e));
                        } finally {
                            response.close();
                        }
                        return;
                    }

                    // 确保Response被关闭
                    try (Response r = response) {
                        ResponseBody body = r.body();
                        if (body == null) {
                            sink.error(AiProviderException.serverError(getProviderCode(), "响应体为空", r.code()));
                            return;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) {
                                    continue;
                                }

                                if (line.startsWith(SSE_DATA_PREFIX)) {
                                    String data = line.substring(SSE_DATA_PREFIX.length()).trim();

                                    // 检查是否是结束标记
                                    if (SSE_DONE_MARKER.equals(data)) {
                                        sink.complete();
                                        break;
                                    }

                                    try {
                                        AiStreamChunk chunk = parseChunk(data);
                                        if (chunk != null) {
                                            sink.next(chunk);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析SSE数据块失败: {}", data, e);
                                    }
                                }
                            }

                            // 如果循环正常结束（没有遇到[DONE]），手动完成
                            if (!sink.isCancelled()) {
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("vLLM流式调用失败", e);
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("vLLM流式调用网络失败", e);
                    sink.error(AiProviderException.networkError(
                            getProviderCode(), "Network error: " + e.getMessage(), e));
                }
            });
        });
    }

    @Override
    public Object convertRequest(AiRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", convertMessages(request.getMessages()));
        body.put("stream", request.getStream() != null && request.getStream());

        // 可选参数
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        return body;
    }

    @Override
    public AiResponse convertResponse(Object rawResponse) {
        if (rawResponse instanceof String) {
            return parseResponse((String) rawResponse);
        }
        throw new IllegalArgumentException("Unsupported response type: " + rawResponse.getClass());
    }

    /**
     * 向量嵌入
     * <p>
     * vLLM支持向量嵌入功能，使用OpenAI兼容的API格式
     * </p>
     */
    @Override
    public Mono<AiResponse> embedding(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("vLLM向量嵌入调用开始: model={}", model.getCode());

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", model.getCode());
            body.put("input", request.getInput());

            String json = JSONUtil.toJsonStr(body);
            RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

            Request httpRequest = new Request.Builder()
                    .url(getVllmEmbeddingUrl())
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw handleError(response);
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw AiProviderException.serverError(getProviderCode(), "响应体为空", response.code());
                }

                String bodyString = responseBody.string();
                log.debug("vLLM向量嵌入响应: {}", bodyString);

                return parseEmbeddingResponse(bodyString);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("vLLM向量嵌入调用失败", e);
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取vLLM API URL
     */
    private String getVllmApiUrl() {
        // 优先从系统属性获取
        String url = System.getProperty("vllm.api.url");
        if (StrUtil.isNotBlank(url)) {
            log.debug("使用自定义vLLM API URL: {}", url);
            return url;
        }

        // 从环境变量获取
        url = System.getenv("VLLM_API_URL");
        if (StrUtil.isNotBlank(url)) {
            log.debug("使用环境变量vLLM API URL: {}", url);
            return url;
        }

        // 使用默认值
        return DEFAULT_VLLM_API_URL;
    }

    /**
     * 获取vLLM Embedding API URL
     */
    private String getVllmEmbeddingUrl() {
        // 优先从系统属性获取
        String url = System.getProperty("vllm.embedding.url");
        if (StrUtil.isNotBlank(url)) {
            return url;
        }

        // 从环境变量获取
        url = System.getenv("VLLM_EMBEDDING_URL");
        if (StrUtil.isNotBlank(url)) {
            return url;
        }

        // 使用默认值
        return DEFAULT_VLLM_EMBEDDING_URL;
    }

    /**
     * 构建HTTP请求
     */
    private Request buildHttpRequest(AiModel model, AiRequest request, boolean stream) {
        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());
        body.put("messages", convertMessages(request.getMessages()));
        body.put("stream", stream);

        // 可选参数
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        // vLLM特有参数
        // best_of: 生成多个候选并返回最佳
        // use_beam_search: 使用束搜索
        // top_k: Top-K采样
        // ignore_eos: 忽略结束标记
        // skip_special_tokens: 跳过特殊token

        String json = JSONUtil.toJsonStr(body);
        log.debug("vLLM请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        // vLLM不需要Authorization header（本地部署）
        return new Request.Builder()
                .url(getVllmApiUrl())
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * 转换消息列表
     */
    private List<Map<String, String>> convertMessages(List<Message> messages) {
        return messages.stream()
                .map(msg -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析响应
     */
    private AiResponse parseResponse(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            // 检查错误
            if (json.containsKey(ERROR_FIELD)) {
                JSONObject error = json.getJSONObject(ERROR_FIELD);
                String errorMsg = error.getStr(MESSAGE_FIELD, "Unknown error");
                throw AiProviderException.serverError(getProviderCode(), errorMsg, 200);
            }

            // 提取choices[0]
            JSONArray choices = json.getJSONArray(CHOICES_FIELD);
            if (CollUtil.isEmpty(choices)) {
                throw AiProviderException.serverError(getProviderCode(), "响应choices为空", 200);
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getStr(CONTENT_FIELD);
            String finishReason = choice.getStr("finish_reason");

            // 提取usage信息
            JSONObject usage = json.getJSONObject("usage");
            Integer promptTokens = usage != null ? usage.getInt("prompt_tokens") : null;
            Integer completionTokens = usage != null ? usage.getInt("completion_tokens") : null;
            Integer totalTokens = usage != null ? usage.getInt("total_tokens") : null;

            return AiResponse.builder()
                    .id(json.getStr("id"))
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .finishReason(finishReason)
                    .model(json.getStr("model"))
                    .created(json.getLong("created"))
                    .build();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析vLLM响应失败: {}", body, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * 解析向量嵌入响应
     */
    private AiResponse parseEmbeddingResponse(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            // 检查错误
            if (json.containsKey(ERROR_FIELD)) {
                JSONObject error = json.getJSONObject(ERROR_FIELD);
                String errorMsg = error.getStr(MESSAGE_FIELD, "Unknown error");
                throw AiProviderException.serverError(getProviderCode(), errorMsg, 200);
            }

            // 提取data[0].embedding
            JSONArray data = json.getJSONArray("data");
            if (CollUtil.isEmpty(data)) {
                throw AiProviderException.serverError(getProviderCode(), "响应data为空", 200);
            }

            JSONObject firstData = data.getJSONObject(0);
            JSONArray embeddingArray = firstData.getJSONArray("embedding");

            List<Double> embedding = embeddingArray.stream()
                    .map(obj -> ((Number) obj).doubleValue())
                    .collect(Collectors.toList());

            // 提取usage信息
            JSONObject usage = json.getJSONObject("usage");
            Integer promptTokens = usage != null ? usage.getInt("prompt_tokens") : null;
            Integer totalTokens = usage != null ? usage.getInt("total_tokens") : null;

            return AiResponse.builder()
                    .id(json.getStr("id"))
                    .embedding(embedding)
                    .promptTokens(promptTokens)
                    .totalTokens(totalTokens)
                    .model(json.getStr("model"))
                    .build();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析vLLM向量嵌入响应失败: {}", body, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse embedding response: " + e.getMessage());
        }
    }

    /**
     * 解析流式数据块
     */
    private AiStreamChunk parseChunk(String data) {
        try {
            JSONObject json = JSONUtil.parseObj(data);

            // 提取choices[0]
            JSONArray choices = json.getJSONArray(CHOICES_FIELD);
            if (CollUtil.isEmpty(choices)) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.getJSONObject(DELTA_FIELD);

            // 提取增量内容
            String content = delta != null ? delta.getStr(CONTENT_FIELD) : null;
            String finishReason = choice.getStr("finish_reason");

            // 检查是否完成
            boolean done = finishReason != null;

            AiStreamChunk.AiStreamChunkBuilder builder = AiStreamChunk.builder()
                    .id(json.getStr("id"))
                    .model(json.getStr("model"))
                    .created(json.getLong("created"))
                    .done(done);

            if (content != null) {
                builder.delta(content);
            }

            if (done) {
                builder.finishReason(finishReason);
                // 流式响应的最后一个chunk可能包含usage
                JSONObject usage = json.getJSONObject("usage");
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析vLLM流式数据块失败: {}", data, e);
            return null;
        }
    }

    /**
     * 处理错误响应
     */
    private AiProviderException handleError(Response response) throws IOException {
        int code = response.code();

        ResponseBody body = response.body();
        String bodyString = body != null ? body.string() : EMPTY_RESPONSE;

        // 尝试解析错误信息
        String errorMessage = bodyString;
        try {
            if (StrUtil.isNotBlank(bodyString) && !EMPTY_RESPONSE.equals(bodyString)) {
                JSONObject json = JSONUtil.parseObj(bodyString);
                JSONObject error = json.getJSONObject(ERROR_FIELD);
                if (error != null) {
                    errorMessage = error.getStr(MESSAGE_FIELD, bodyString);
                }
            }
        } catch (Exception e) {
            log.warn("解析vLLM错误响应失败", e);
        }

        log.error("vLLM API错误: code={}, message={}", code, errorMessage);

        // 根据HTTP状态码分类错误
        return switch (code) {
            case 401, 403 -> AiProviderException.authError(getProviderCode(), errorMessage);
            case 429 -> AiProviderException.rateLimitError(getProviderCode(), errorMessage);
            case 400 -> AiProviderException.invalidRequest(getProviderCode(), errorMessage);
            case 404 -> {
                // 尝试提取模型名称
                String modelName = extractModelFromError(bodyString);
                yield modelName != null
                        ? AiProviderException.modelNotFound(getProviderCode(), modelName)
                        : AiProviderException.invalidRequest(getProviderCode(), errorMessage);
            }
            default -> code >= 500
                    ? AiProviderException.serverError(getProviderCode(), errorMessage, code)
                    : AiProviderException.networkError(getProviderCode(), errorMessage, null);
        };
    }

    /**
     * 从错误信息中提取模型名称
     */
    private String extractModelFromError(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);
            JSONObject error = json.getJSONObject(ERROR_FIELD);
            if (error != null) {
                String message = error.getStr(MESSAGE_FIELD);
                if (message != null && message.contains(MODEL_FIELD)) {
                    return "unknown";
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
}
