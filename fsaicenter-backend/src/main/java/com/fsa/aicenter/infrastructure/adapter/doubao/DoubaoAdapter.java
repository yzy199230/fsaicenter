package com.fsa.aicenter.infrastructure.adapter.doubao;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fsa.aicenter.application.service.ModelApiKeySelector;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 豆包（火山引擎）适配器实现
 * <p>
 * 支持豆包大模型的API，包括Chat、图像生成等功能。
 * API格式与OpenAI兼容。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class DoubaoAdapter implements AiProviderAdapter {

    /**
     * 豆包API基础地址（火山引擎）
     */
    private static final String DOUBAO_CHAT_API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String DOUBAO_IMAGE_API_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String EMPTY_RESPONSE = "空响应";
    private static final String ERROR_FIELD = "error";
    private static final String MESSAGE_FIELD = "message";
    private static final String CHOICES_FIELD = "choices";
    private static final String DELTA_FIELD = "delta";
    private static final String CONTENT_FIELD = "content";
    private static final String DATA_FIELD = "data";

    private final OkHttpClient httpClient;
    private final ModelApiKeySelector modelApiKeySelector;

    public DoubaoAdapter(
            @Qualifier("aiOkHttpClient") OkHttpClient httpClient,
            ModelApiKeySelector modelApiKeySelector) {
        this.httpClient = httpClient;
        this.modelApiKeySelector = modelApiKeySelector;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.REMOTE;
    }

    @Override
    public String getProviderCode() {
        return "doubao";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("豆包非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建HTTP请求
            Request httpRequest = buildChatHttpRequest(model, selectedKey.getApiKey(), request, false);

            // 同步调用（在Reactor线程池中）
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(getProviderCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("豆包响应: {}", bodyString);

                AiResponse aiResponse = parseChatResponse(bodyString);

                // 记录成功并消费配额
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("豆包调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<AiStreamChunk> callStream(AiModel model, AiRequest request) {
        // 在Flux创建前选择Key
        ModelApiKey selectedKey = selectApiKey(model);

        return Flux.create(sink -> {
            log.debug("豆包流式调用开始: model={}, messages={}, keyName={}",
                    model.getCode(), request.getMessages().size(), selectedKey.getKeyName());

            // 构建HTTP请求
            Request httpRequest = buildChatHttpRequest(model, selectedKey.getApiKey(), request, true);
            Call call = httpClient.newCall(httpRequest);

            // Token计数器
            final int[] totalTokens = {0};

            // 注册取消回调
            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("豆包流式调用已取消");
                }
            });

            // 异步调用
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        try {
                            recordKeyFailure(selectedKey.getId());
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

                    try (Response r = response) {
                        ResponseBody body = r.body();
                        if (body == null) {
                            recordKeyFailure(selectedKey.getId());
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

                                    if (SSE_DONE_MARKER.equals(data)) {
                                        recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                        sink.complete();
                                        break;
                                    }

                                    try {
                                        AiStreamChunk chunk = parseStreamChunk(data);
                                        if (chunk != null) {
                                            if (chunk.getTotalTokens() != null) {
                                                totalTokens[0] = chunk.getTotalTokens();
                                            }
                                            sink.next(chunk);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析SSE数据块失败: {}", data, e);
                                    }
                                }
                            }

                            if (!sink.isCancelled()) {
                                recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("豆包流式调用失败", e);
                        recordKeyFailure(selectedKey.getId());
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("豆包流式调用网络失败", e);
                    recordKeyFailure(selectedKey.getId());
                    sink.error(AiProviderException.networkError(
                            getProviderCode(), "Network error: " + e.getMessage(), e));
                }
            });
        });
    }

    @Override
    public Mono<AiResponse> generateImage(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("豆包图像生成开始: model={}, prompt={}", model.getCode(), request.getPrompt());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建图像生成请求
            Request httpRequest = buildImageHttpRequest(model, selectedKey.getApiKey(), request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(getProviderCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("豆包图像生成响应: {}", bodyString);

                AiResponse aiResponse = parseImageResponse(bodyString);

                recordKeySuccess(selectedKey.getId(), null);

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("豆包图像生成失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
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

        return body;
    }

    @Override
    public AiResponse convertResponse(Object rawResponse) {
        if (rawResponse instanceof String) {
            return parseChatResponse((String) rawResponse);
        }
        throw new IllegalArgumentException("Unsupported response type: " + rawResponse.getClass());
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建Chat HTTP请求
     */
    private Request buildChatHttpRequest(AiModel model, String apiKey, AiRequest request, boolean stream) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key not configured");
        }

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

        String json = JSONUtil.toJsonStr(body);
        log.debug("豆包请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        return new Request.Builder()
                .url(DOUBAO_CHAT_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * 构建图像生成HTTP请求
     */
    private Request buildImageHttpRequest(AiModel model, String apiKey, AiRequest request) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key not configured");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());
        body.put("prompt", request.getPrompt());

        // 图像参数
        if (request.getN() != null) {
            body.put("n", request.getN());
        } else {
            body.put("n", 1);
        }
        if (request.getSize() != null) {
            body.put("size", request.getSize());
        }
        if (request.getQuality() != null) {
            body.put("quality", request.getQuality());
        }
        if (request.getStyle() != null) {
            body.put("style", request.getStyle());
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("豆包图像生成请求: model={}, prompt={}", model.getCode(), request.getPrompt());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        return new Request.Builder()
                .url(DOUBAO_IMAGE_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * 选择API Key
     */
    private ModelApiKey selectApiKey(AiModel model) {
        Optional<ModelApiKey> keyOpt = modelApiKeySelector.selectKey(model.getId());

        if (keyOpt.isEmpty()) {
            throw AiProviderException.authError(getProviderCode(),
                    String.format("模型 %s 没有可用的API Key", model.getCode()));
        }

        ModelApiKey selectedKey = keyOpt.get();
        log.debug("为模型 {} 选择了Key: {}", model.getCode(), selectedKey.getKeyName());

        return selectedKey;
    }

    /**
     * 记录Key使用成功
     */
    private void recordKeySuccess(Long keyId, Integer tokens) {
        try {
            modelApiKeySelector.recordSuccess(keyId);
            if (tokens != null && tokens > 0) {
                modelApiKeySelector.consumeQuota(keyId, tokens);
            }
            log.debug("记录Key {} 使用成功, tokens={}", keyId, tokens);
        } catch (Exception e) {
            log.error("记录Key使用成功失败: keyId={}", keyId, e);
        }
    }

    /**
     * 记录Key使用失败
     */
    private void recordKeyFailure(Long keyId) {
        try {
            modelApiKeySelector.recordFailure(keyId);
            log.warn("记录Key {} 使用失败", keyId);
        } catch (Exception e) {
            log.error("记录Key使用失败失败: keyId={}", keyId, e);
        }
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
     * 解析Chat响应
     */
    private AiResponse parseChatResponse(String body) {
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
            log.error("解析豆包响应失败: {}", body, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * 解析图像生成响应
     */
    private AiResponse parseImageResponse(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            // 提取data数组中的图像URL
            JSONArray dataArray = json.getJSONArray(DATA_FIELD);
            List<String> imageUrls = new ArrayList<>();

            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String url = item.getStr("url");
                    if (StrUtil.isNotBlank(url)) {
                        imageUrls.add(url);
                    }
                    // 有些API返回b64_json
                    String b64Json = item.getStr("b64_json");
                    if (StrUtil.isNotBlank(b64Json)) {
                        imageUrls.add("data:image/png;base64," + b64Json);
                    }
                }
            }

            return AiResponse.builder()
                    .id(json.getStr("id"))
                    .imageUrls(imageUrls)
                    .created(json.getLong("created"))
                    .build();
        } catch (Exception e) {
            log.error("解析豆包图像生成响应失败: {}", body, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse image response: " + e.getMessage());
        }
    }

    /**
     * 解析流式数据块
     */
    private AiStreamChunk parseStreamChunk(String data) {
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
                JSONObject usage = json.getJSONObject("usage");
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析豆包流式数据块失败: {}", data, e);
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
            log.warn("解析豆包错误响应失败", e);
        }

        log.error("豆包 API错误: code={}, message={}", code, errorMessage);

        // 根据HTTP状态码分类错误
        return switch (code) {
            case 401, 403 -> AiProviderException.authError(getProviderCode(), errorMessage);
            case 429 -> AiProviderException.rateLimitError(getProviderCode(), errorMessage);
            case 400 -> AiProviderException.invalidRequest(getProviderCode(), errorMessage);
            case 404 -> AiProviderException.invalidRequest(getProviderCode(), errorMessage);
            default -> code >= 500
                    ? AiProviderException.serverError(getProviderCode(), errorMessage, code)
                    : AiProviderException.networkError(getProviderCode(), errorMessage, null);
        };
    }
}
