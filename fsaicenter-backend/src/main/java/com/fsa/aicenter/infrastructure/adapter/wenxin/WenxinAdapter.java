package com.fsa.aicenter.infrastructure.adapter.wenxin;

import cn.hutool.core.util.StrUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文心一言适配器实现
 * <p>
 * 支持文心一言的API，包括非流式和流式调用。
 * 文心一言需要两步认证：先获取Access Token，然后使用Token调用API。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class WenxinAdapter implements AiProviderAdapter {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String CHAT_API_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data:";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String EMPTY_RESPONSE = "空响应";
    private static final String ERROR_FIELD = "error";
    private static final String ERROR_CODE_FIELD = "error_code";
    private static final String ERROR_MSG_FIELD = "error_msg";
    private static final String MESSAGE_FIELD = "message";
    private static final String RESULT_FIELD = "result";
    private static final String USAGE_FIELD = "usage";
    private static final String IS_TRUNCATED_FIELD = "is_truncated";
    private static final String SENTENCE_ID_FIELD = "sentence_id";
    private static final String IS_END_FIELD = "is_end";

    private final OkHttpClient httpClient;
    private final ModelApiKeySelector modelApiKeySelector;

    // Token缓存（简化版，生产环境建议使用Redis）
    // TODO: 生产环境建议使用Redis实现Token缓存，并支持多Key的Token缓存
    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime;

    public WenxinAdapter(
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
        return "wenxin";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("文心一言非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, selectedKey, request, false);

            // 同步调用（在Reactor线程池中）
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    // 记录失败
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(getProviderCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("文心一言响应: {}", bodyString);

                AiResponse aiResponse = parseResponse(bodyString);

                // 记录成功并消费配额
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("文心一言调用失败", e);
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
            log.debug("文心一言流式调用开始: model={}, messages={}, keyName={}",
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
                    log.debug("文心一言流式调用已取消");
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
                                        // 流式完成，记录成功
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
                                            // 检查文心一言的is_end标记
                                            if (chunk.isDone()) {
                                                recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                                sink.complete();
                                                break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析SSE数据块失败: {}", data, e);
                                        // 继续处理下一个chunk
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
                        log.error("文心一言流式调用失败", e);
                        recordKeyFailure(selectedKey.getId());
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("文心一言流式调用网络失败", e);
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
        body.put("messages", convertMessages(request.getMessages()));
        body.put("stream", request.getStream() != null && request.getStream());

        // 可选参数
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_output_tokens", request.getMaxTokens()); // 文心一言使用max_output_tokens
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getUser() != null) {
            body.put("user_id", request.getUser()); // 文心一言使用user_id
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
     * 获取Access Token（带缓存）
     * <p>
     * 文心一言需要先使用API Key和Secret Key获取Access Token，然后使用Token调用API。
     * Token默认有效期30天，这里实现了简单的内存缓存。
     * </p>
     * <p>
     * TODO: 生产环境建议使用Redis实现Token缓存，支持分布式环境和多Key的Token管理
     * </p>
     * <p>
     * 整个方法synchronized修饰，确保在高并发场景下的线程安全性。
     * </p>
     *
     * @param selectedKey 选中的ModelApiKey
     * @return Access Token
     */
    private synchronized String getAccessToken(ModelApiKey selectedKey) {
        // 在synchronized内部再次检查缓存，确保原子性
        // TODO: 当前实现只缓存一个Token，未来应该为每个Key缓存独立的Token
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            log.debug("使用缓存的Access Token");
            return cachedAccessToken;
        }

        log.info("获取新的文心一言Access Token，keyName={}", selectedKey.getKeyName());

        // 从ModelApiKey中解析API Key和Secret Key
        // apiKey字段格式: "API_KEY:SECRET_KEY"
        String apiKeyStr = selectedKey.getApiKey();
        if (StrUtil.isBlank(apiKeyStr)) {
            throw AiProviderException.authError(getProviderCode(), "API Key为空");
        }

        String[] parts = apiKeyStr.split(":", 2);
        if (parts.length != 2) {
            // 如果格式不对，尝试降级到环境变量
            log.warn("ModelApiKey格式不正确(应为API_KEY:SECRET_KEY)，尝试降级到环境变量");
            return getAccessTokenFromEnv();
        }

        String apiKey = parts[0].trim();
        String secretKey = parts[1].trim();

        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(secretKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key或Secret Key为空");
        }

        // 构建token请求
        String tokenUrl = String.format(
                "%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                TOKEN_URL, apiKey, secretKey
        );

        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create("", null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw AiProviderException.authError(getProviderCode(),
                        "获取Access Token失败: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw AiProviderException.serverError(getProviderCode(), "Token响应体为空", response.code());
            }

            String bodyString = body.string();
            JSONObject json = JSONUtil.parseObj(bodyString);

            // 检查错误
            String error = json.getStr(ERROR_FIELD);
            if (StrUtil.isNotBlank(error)) {
                String errorDesc = json.getStr("error_description", error);
                throw AiProviderException.authError(getProviderCode(),
                        "获取Access Token失败: " + errorDesc);
            }

            // 提取token和过期时间
            cachedAccessToken = json.getStr("access_token");
            Integer expiresIn = json.getInt("expires_in", 2592000); // 默认30天

            if (StrUtil.isBlank(cachedAccessToken)) {
                throw AiProviderException.authError(getProviderCode(),
                        "响应中未包含access_token: " + bodyString);
            }

            // 设置过期时间（在synchronized块内，确保原子性）
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

            log.info("成功获取文心一言Access Token，有效期{}秒", expiresIn);
            return cachedAccessToken;

        } catch (IOException e) {
            throw AiProviderException.networkError(getProviderCode(), "获取Access Token失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从环境变量获取Access Token（降级方案）
     * <p>
     * 当ModelApiKey池中的Key格式不正确时，降级到环境变量方式
     * </p>
     */
    private synchronized String getAccessTokenFromEnv() {
        // 检查缓存
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            log.debug("使用缓存的Access Token(环境变量)");
            return cachedAccessToken;
        }

        log.info("从环境变量获取文心一言Access Token");

        // 从环境变量提取
        String apiKey = System.getProperty("wenxin.api.key");
        if (StrUtil.isBlank(apiKey)) {
            apiKey = System.getenv("WENXIN_API_KEY");
        }

        String secretKey = System.getProperty("wenxin.secret.key");
        if (StrUtil.isBlank(secretKey)) {
            secretKey = System.getenv("WENXIN_SECRET_KEY");
        }

        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(secretKey)) {
            throw AiProviderException.authError(getProviderCode(), "环境变量中未配置API Key或Secret Key");
        }

        // 构建token请求
        String tokenUrl = String.format(
                "%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                TOKEN_URL, apiKey, secretKey
        );

        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create("", null))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw AiProviderException.authError(getProviderCode(),
                        "获取Access Token失败: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw AiProviderException.serverError(getProviderCode(), "Token响应体为空", response.code());
            }

            String bodyString = body.string();
            JSONObject json = JSONUtil.parseObj(bodyString);

            // 检查错误
            String error = json.getStr(ERROR_FIELD);
            if (StrUtil.isNotBlank(error)) {
                String errorDesc = json.getStr("error_description", error);
                throw AiProviderException.authError(getProviderCode(),
                        "获取Access Token失败: " + errorDesc);
            }

            // 提取token和过期时间
            cachedAccessToken = json.getStr("access_token");
            Integer expiresIn = json.getInt("expires_in", 2592000); // 默认30天

            if (StrUtil.isBlank(cachedAccessToken)) {
                throw AiProviderException.authError(getProviderCode(),
                        "响应中未包含access_token: " + bodyString);
            }

            // 设置过期时间
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

            log.info("成功获取文心一言Access Token(环境变量)，有效期{}秒", expiresIn);
            return cachedAccessToken;

        } catch (IOException e) {
            throw AiProviderException.networkError(getProviderCode(), "获取Access Token失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建HTTP请求
     */
    private Request buildHttpRequest(AiModel model, ModelApiKey selectedKey, AiRequest request, boolean stream) {
        // 获取Access Token
        String accessToken = getAccessToken(selectedKey);

        // 调用 convertRequest 构建请求体
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) convertRequest(request);

        // 设置流式参数
        body.put("stream", stream);

        // 文心一言的URL需要拼接access_token
        String url = String.format("%s?access_token=%s", CHAT_API_URL, accessToken);

        String json = JSONUtil.toJsonStr(body);
        log.debug("文心一言请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        return new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * 选择API Key
     * <p>
     * 优先从ModelApiKey池选择，如果池中没有可用Key则抛出异常
     * </p>
     *
     * @param model AI模型
     * @return 选中的ModelApiKey
     */
    private ModelApiKey selectApiKey(AiModel model) {
        Optional<ModelApiKey> keyOpt = modelApiKeySelector.selectKey(model.getId());

        if (keyOpt.isEmpty()) {
            // 如果Key池中没有可用Key，抛出异常
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
            String errorCode = json.getStr(ERROR_CODE_FIELD);
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMsg = json.getStr(ERROR_MSG_FIELD, "未知错误");
                throw AiProviderException.serverError(getProviderCode(),
                        String.format("文心一言返回错误: code=%s, msg=%s", errorCode, errorMsg), 200);
            }

            // 文心一言的响应格式：直接的 result 字段
            String content = json.getStr(RESULT_FIELD);
            if (StrUtil.isBlank(content)) {
                throw AiProviderException.serverError(getProviderCode(), "响应result为空", 200);
            }

            // Token统计
            JSONObject usage = json.getJSONObject(USAGE_FIELD);
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;

            if (usage != null) {
                promptTokens = usage.getInt("prompt_tokens");
                completionTokens = usage.getInt("completion_tokens");
                totalTokens = usage.getInt("total_tokens");
            }

            // is_truncated 表示是否因为长度限制而截断
            Boolean isTruncated = json.getBool(IS_TRUNCATED_FIELD, false);
            String finishReason = isTruncated ? "length" : "stop";

            return AiResponse.builder()
                    .id(json.getStr("id"))
                    .content(content)
                    .finishReason(finishReason)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .build();
        } catch (Exception e) {
            log.error("解析文心一言响应失败: {}", bodyString, e);
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

            // 检查错误
            String errorCode = json.getStr(ERROR_CODE_FIELD);
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMsg = json.getStr(ERROR_MSG_FIELD, "未知错误");
                log.error("文心一言流式响应错误: code={}, msg={}", errorCode, errorMsg);
                return null;
            }

            // 提取增量内容
            String content = json.getStr(RESULT_FIELD);
            Boolean isEnd = json.getBool(IS_END_FIELD, false);

            AiStreamChunk.AiStreamChunkBuilder builder = AiStreamChunk.builder()
                    .id(json.getStr("id"))
                    .created(json.getLong("created"))
                    .done(isEnd);

            if (content != null) {
                builder.delta(content);
            }

            if (isEnd) {
                // 文心一言的 is_truncated 字段
                Boolean isTruncated = json.getBool(IS_TRUNCATED_FIELD, false);
                builder.finishReason(isTruncated ? "length" : "stop");

                // 提取usage信息
                JSONObject usage = json.getJSONObject(USAGE_FIELD);
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析文心一言流式数据块失败: {}", data, e);
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

                // 文心一言错误格式：error_code + error_msg
                String errorCode = json.getStr(ERROR_CODE_FIELD);
                String errorMsg = json.getStr(ERROR_MSG_FIELD);

                if (StrUtil.isNotBlank(errorCode) || StrUtil.isNotBlank(errorMsg)) {
                    errorMessage = String.format("code=%s, msg=%s",
                            StrUtil.nullToDefault(errorCode, "unknown"),
                            StrUtil.nullToDefault(errorMsg, "unknown"));
                }
            }
        } catch (Exception e) {
            // 解析失败，使用原始body
            log.warn("解析文心一言错误响应失败", e);
        }

        log.error("文心一言 API错误: code={}, message={}", code, errorMessage);

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
