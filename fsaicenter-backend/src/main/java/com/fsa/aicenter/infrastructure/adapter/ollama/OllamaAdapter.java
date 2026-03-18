package com.fsa.aicenter.infrastructure.adapter.ollama;

import cn.hutool.core.util.StrUtil;
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
 * Ollama适配器实现
 * <p>
 * 支持Ollama本地部署的AI模型，包括非流式和流式调用。
 * Ollama API兼容OpenAI格式，无需认证。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class OllamaAdapter implements AiProviderAdapter {

    private static final String DEFAULT_OLLAMA_API_URL = "http://localhost:11434/v1/chat/completions";
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

    public OllamaAdapter(@Qualifier("aiOkHttpClient") OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LOCAL;  // Ollama是本地部署
    }

    @Override
    public String getProviderCode() {
        return "ollama";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("Ollama非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

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
                log.debug("Ollama响应: {}", bodyString);

                return parseResponse(bodyString);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("Ollama调用失败", e);
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<AiStreamChunk> callStream(AiModel model, AiRequest request) {
        return Flux.create(sink -> {
            log.debug("Ollama流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, request, true);
            Call call = httpClient.newCall(httpRequest);

            // 注册取消回调
            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("Ollama流式调用已取消");
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
                                        log.warn("解析SSE数据���失败: {}", data, e);
                                        // 继续处理下一个chunk
                                    }
                                }
                            }

                            // 如果循环正常结束（没有遇到[DONE]），手动完成
                            if (!sink.isCancelled()) {
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Ollama流式调用失败", e);
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("Ollama流式调用网络失败", e);
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

    // ========== 私有辅助方法 ==========

    /**
     * 获取Ollama API URL
     * <p>
     * 支持通过系统属性或环境变量自定义Ollama服务器地址
     * </p>
     *
     * @return Ollama API URL
     */
    private String getOllamaApiUrl() {
        // 优先从系统属性获取
        String url = System.getProperty("ollama.api.url");
        if (StrUtil.isNotBlank(url)) {
            log.debug("使用自定义Ollama API URL: {}", url);
            return url;
        }

        // 从环境变量获取
        url = System.getenv("OLLAMA_API_URL");
        if (StrUtil.isNotBlank(url)) {
            log.debug("使用环境变量Ollama API URL: {}", url);
            return url;
        }

        // 使用默认值
        return DEFAULT_OLLAMA_API_URL;
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

        String json = JSONUtil.toJsonStr(body);
        log.debug("Ollama请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        // Ollama不需要Authorization header
        return new Request.Builder()
                .url(getOllamaApiUrl())
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

            // 提取choices[0]
            JSONObject choice = json.getJSONArray(CHOICES_FIELD).getJSONObject(0);
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
        } catch (Exception e) {
            log.error("解析Ollama响应失败: {}", body, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * 解析流式数据块
     */
    private AiStreamChunk parseChunk(String data) {
        try {
            JSONObject json = JSONUtil.parseObj(data);

            // 提取choices[0]
            JSONObject choice = json.getJSONArray(CHOICES_FIELD).getJSONObject(0);
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
                // 流式响应的最后一个chunk通常没有usage信息
                // 某些情况下会在最后一个chunk包含usage
                JSONObject usage = json.getJSONObject("usage");
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析Ollama流式数据块失败: {}", data, e);
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
            // 解析失败，使用原始body
            log.warn("解析Ollama错误响应失败", e);
        }

        log.error("Ollama API错误: code={}, message={}", code, errorMessage);

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
                    // 简单提取，实际可能需要更复杂的逻辑
                    return "unknown";
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
}
