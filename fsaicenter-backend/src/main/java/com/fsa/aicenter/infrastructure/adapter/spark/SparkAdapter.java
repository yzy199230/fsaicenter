package com.fsa.aicenter.infrastructure.adapter.spark;

import cn.hutool.core.collection.CollUtil;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 讯飞星火适配器实现
 * <p>
 * 支持讯飞星火大模型的API调用，包括非流式和流式调用。
 * 星火API使用WebSocket协议，但也提供HTTP兼容接口。
 * </p>
 * <p>
 * 支持的模型：
 * <ul>
 *   <li>spark-lite: 轻量版</li>
 *   <li>spark-pro: 专业版</li>
 *   <li>spark-max: 旗舰版</li>
 *   <li>spark-ultra: 超级版</li>
 * </ul>
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class SparkAdapter implements AiProviderAdapter {

    private static final String SPARK_API_HOST = "spark-api.xf-yun.com";
    private static final String SPARK_API_PATH = "/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data:";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String EMPTY_RESPONSE = "空响应";
    private static final String ERROR_FIELD = "error";
    private static final String MESSAGE_FIELD = "message";
    private static final String CHOICES_FIELD = "choices";
    private static final String DELTA_FIELD = "delta";
    private static final String CONTENT_FIELD = "content";

    private final OkHttpClient httpClient;
    private final ModelApiKeySelector modelApiKeySelector;

    public SparkAdapter(
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
        return "spark";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("星火非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, selectedKey, request, false);

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
                log.debug("星火响应: {}", bodyString);

                AiResponse aiResponse = parseResponse(bodyString);

                // 记录成功并消费配额
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("星火调用失败", e);
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
            log.debug("星火流式调用开始: model={}, messages={}, keyName={}",
                    model.getCode(), request.getMessages().size(), selectedKey.getKeyName());

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, selectedKey, request, true);
            Call call = httpClient.newCall(httpRequest);

            // Token计数器
            final int[] totalTokens = {0};

            // 注册取消回调
            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("星火流式调用已取消");
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

                    // 确保Response被关闭
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

                                    // 检查是否是结束标记
                                    if (SSE_DONE_MARKER.equals(data)) {
                                        recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                        sink.complete();
                                        break;
                                    }

                                    try {
                                        AiStreamChunk chunk = parseChunk(data);
                                        if (chunk != null) {
                                            // 累计token
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

                            // 如果循环正常结束（没有遇到[DONE]），手动完成
                            if (!sink.isCancelled()) {
                                recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("星火流式调用失败", e);
                        recordKeyFailure(selectedKey.getId());
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("星火流式调用网络失败", e);
                    recordKeyFailure(selectedKey.getId());
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
     * 构建HTTP请求
     * <p>
     * 星火API使用Bearer Token认证，格式为：APIKey:APISecret
     * </p>
     */
    private Request buildHttpRequest(AiModel model, ModelApiKey selectedKey, AiRequest request, boolean stream) {
        String apiKey = selectedKey.getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key not configured");
        }

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
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("星火请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        // 星火API使用Bearer Token认证
        return new Request.Builder()
                .url("https://" + SPARK_API_HOST + SPARK_API_PATH)
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
     * 解析响应
     */
    private AiResponse parseResponse(String bodyString) {
        try {
            JSONObject json = JSONUtil.parseObj(bodyString);

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
            JSONObject message = choice.getJSONObject(MESSAGE_FIELD);
            String content = message.getStr(CONTENT_FIELD);
            String finishReason = choice.getStr("finish_reason");

            // 提取usage信息
            JSONObject usage = json.getJSONObject("usage");
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;

            if (usage != null) {
                promptTokens = usage.getInt("prompt_tokens");
                completionTokens = usage.getInt("completion_tokens");
                totalTokens = usage.getInt("total_tokens");
            }

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
            log.error("解析星火响应失败: {}", bodyString, e);
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
                // 提取usage信息
                JSONObject usage = json.getJSONObject("usage");
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析星火流式数据块失败: {}", data, e);
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
            log.warn("解析星火错误响应失败", e);
        }

        log.error("星火 API错误: code={}, message={}", code, errorMessage);

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
