package com.fsa.aicenter.infrastructure.adapter.generic;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fsa.aicenter.application.service.ModelApiKeySelector;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
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

@Slf4j
@Component
public class GenericOpenAiAdapter implements AiProviderAdapter {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String CHOICES_FIELD = "choices";
    private static final String DELTA_FIELD = "delta";
    private static final String CONTENT_FIELD = "content";
    private static final String MESSAGE_FIELD = "message";
    private static final String ERROR_FIELD = "error";

    // 协议类型常量
    private static final String PROTOCOL_OPENAI_COMPATIBLE = "openai_compatible";
    private static final String PROTOCOL_OPENAI_LIKE = "openai_like";
    private static final String PROTOCOL_CUSTOM_HTTP = "custom_http";

    // 支持的协议类型集合
    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of(
            PROTOCOL_OPENAI_COMPATIBLE, PROTOCOL_OPENAI_LIKE, PROTOCOL_CUSTOM_HTTP
    );

    private final OkHttpClient httpClient;
    private final ModelApiKeySelector modelApiKeySelector;
    private final ProviderRepository providerRepository;

    public GenericOpenAiAdapter(
            @Qualifier("aiOkHttpClient") OkHttpClient httpClient,
            ModelApiKeySelector modelApiKeySelector,
            ProviderRepository providerRepository) {
        this.httpClient = httpClient;
        this.modelApiKeySelector = modelApiKeySelector;
        this.providerRepository = providerRepository;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.REMOTE;
    }

    @Override
    public String getProviderCode() {
        return "__generic__";
    }

    public boolean supports(Provider provider) {
        if (provider == null || provider.getProtocolType() == null) {
            return false;
        }
        return SUPPORTED_PROTOCOLS.contains(provider.getProtocolType().toLowerCase());
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            Provider provider = getProvider(model);
            log.debug("通用适配器调用: provider={}, model={}", provider.getCode(), model.getCode());

            ModelApiKey selectedKey = selectApiKey(model);
            Request httpRequest = buildHttpRequest(provider, model, selectedKey.getApiKey(), request, false);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(provider, response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(provider.getCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("通用适配器响应: length={}", bodyString.length());

                AiResponse aiResponse = parseResponse(provider, bodyString);
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通用适配器调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(provider.getCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<AiStreamChunk> callStream(AiModel model, AiRequest request) {
        Provider provider = getProvider(model);
        ModelApiKey selectedKey = selectApiKey(model);

        return Flux.create(sink -> {
            log.debug("通用适配器流式调用: provider={}, model={}", provider.getCode(), model.getCode());

            Request httpRequest = buildHttpRequest(provider, model, selectedKey.getApiKey(), request, true);
            Call call = httpClient.newCall(httpRequest);

            final int[] totalTokens = {0};
            final boolean[] doneReceived = {false};
            final boolean[] successRecorded = {false};  // 防止重复记录成功

            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("通用适配器流式调用已取消");
                }
            });

            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        try {
                            recordKeyFailure(selectedKey.getId());
                            sink.error(handleError(provider, response));
                        } catch (IOException e) {
                            sink.error(AiProviderException.networkError(provider.getCode(), e.getMessage(), e));
                        } finally {
                            response.close();
                        }
                        return;
                    }

                    try (Response r = response) {
                        ResponseBody body = r.body();
                        if (body == null) {
                            recordKeyFailure(selectedKey.getId());
                            sink.error(AiProviderException.serverError(provider.getCode(), "响应体为空", r.code()));
                            return;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) continue;

                                if (line.startsWith(SSE_DATA_PREFIX)) {
                                    String data = line.substring(SSE_DATA_PREFIX.length()).trim();

                                    if (SSE_DONE_MARKER.equals(data)) {
                                        doneReceived[0] = true;
                                        if (!successRecorded[0]) {
                                            successRecorded[0] = true;
                                            recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                        }
                                        sink.complete();
                                        break;
                                    }

                                    try {
                                        AiStreamChunk chunk = parseChunk(provider, data);
                                        if (chunk != null) {
                                            if (chunk.getTotalTokens() != null) {
                                                totalTokens[0] = chunk.getTotalTokens();
                                            }
                                            sink.next(chunk);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析SSE数据块失败", e);
                                    }
                                }
                            }

                            // 流结束但没有收到[DONE]标记的情况（某些提供商可能不发送）
                            if (!sink.isCancelled() && !successRecorded[0]) {
                                successRecorded[0] = true;
                                recordKeySuccess(selectedKey.getId(), totalTokens[0]);
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("通用适配器流式调用失败", e);
                        recordKeyFailure(selectedKey.getId());
                        sink.error(AiProviderException.networkError(provider.getCode(), e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("通用适配器流式调用网络失败", e);
                    recordKeyFailure(selectedKey.getId());
                    sink.error(AiProviderException.networkError(provider.getCode(), e.getMessage(), e));
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
            return parseResponse(null, (String) rawResponse);
        }
        throw new IllegalArgumentException("Unsupported response type: " + rawResponse.getClass());
    }

    @Override
    public Mono<AiResponse> embedding(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            Provider provider = getProvider(model);
            log.debug("通用适配器Embedding调用: provider={}, model={}", provider.getCode(), model.getCode());

            ModelApiKey selectedKey = selectApiKey(model);
            Request httpRequest = buildEmbeddingRequest(provider, model, selectedKey.getApiKey(), request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(provider, response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(provider.getCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("通用适配器Embedding响应: length={}", bodyString.length());

                EmbeddingResponse embeddingResponse = parseEmbeddingResponse(provider, bodyString);

                if (embeddingResponse.getUsage() != null) {
                    recordKeySuccess(selectedKey.getId(), embeddingResponse.getUsage().getTotalTokens());
                }

                return convertEmbeddingResponse(embeddingResponse);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通用适配器Embedding调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(provider.getCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<AiResponse> generateImage(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            Provider provider = getProvider(model);
            log.debug("通用适配器Image生成调用: provider={}, model={}", provider.getCode(), model.getCode());

            ModelApiKey selectedKey = selectApiKey(model);
            Request httpRequest = buildImageRequest(provider, model, selectedKey.getApiKey(), request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(provider, response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(provider.getCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("通用适配器Image生成响应: length={}", bodyString.length());

                ImageResponse imageResponse = parseImageResponse(provider, bodyString);

                recordKeySuccess(selectedKey.getId(), null);

                return convertImageResponse(imageResponse);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通用适配器Image生成调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(provider.getCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AiResponse> generateVideo(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            Provider provider = getProvider(model);
            log.debug("通用适配器Video生成调用: provider={}, model={}", provider.getCode(), model.getCode());

            ModelApiKey selectedKey = selectApiKey(model);
            Request httpRequest = buildVideoRequest(provider, model, selectedKey.getApiKey(), request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    recordKeyFailure(selectedKey.getId());
                    throw handleError(provider, response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    recordKeyFailure(selectedKey.getId());
                    throw AiProviderException.serverError(provider.getCode(), "响应体为空", response.code());
                }

                String bodyString = body.string();
                log.debug("通用适配器Video生成响应: length={}", bodyString.length());

                VideoResponse videoResponse = parseVideoResponse(provider, bodyString);

                recordKeySuccess(selectedKey.getId(), null);

                return convertVideoResponse(videoResponse);
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通用适配器Video生成调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(provider.getCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Provider getProvider(AiModel model) {
        Long providerId = model.getProviderId();
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalStateException("Provider not found: " + providerId));
    }

    private Request buildHttpRequest(Provider provider, AiModel model, String apiKey, AiRequest request, boolean stream) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(provider.getCode(), "API Key not configured");
        }

        log.debug("为模型 {} 选择了Key", model.getCode());

        String baseUrl = provider.getBaseUrl();
        String endpoint = provider.getChatEndpointOrDefault();
        String url = baseUrl.endsWith("/") ? baseUrl + endpoint.substring(1) : baseUrl + endpoint;

        String json;

        // 如果是 custom_http 类型且配置了请求模板，使用模板变量替换
        if (PROTOCOL_CUSTOM_HTTP.equals(provider.getProtocolType())
                && StrUtil.isNotBlank(provider.getRequestTemplate())) {
            Map<String, Object> variables = buildTemplateVariables(model, request, stream);
            json = applyRequestTemplate(provider.getRequestTemplate(), variables);
            log.debug("使用请求模板构建请求体");
        } else {
            json = buildOpenAiRequestBody(model, request, stream);
        }

        log.debug("通用适配器请求: url={}, model={}", url, model.getCode());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody);

        String authHeader = provider.getAuthHeader() != null ? provider.getAuthHeader() : "Authorization";
        String authPrefix = provider.getAuthPrefixOrDefault();
        builder.header(authHeader, authPrefix + apiKey);

        if (StrUtil.isNotBlank(provider.getExtraHeaders())) {
            try {
                JSONObject extraHeaders = JSONUtil.parseObj(provider.getExtraHeaders());
                for (String key : extraHeaders.keySet()) {
                    builder.header(key, extraHeaders.getStr(key));
                }
            } catch (Exception e) {
                log.warn("解析额外请求头失败: {}", provider.getExtraHeaders(), e);
            }
        }

        return builder.build();
    }

    private ModelApiKey selectApiKey(AiModel model) {
        Optional<ModelApiKey> keyOpt = modelApiKeySelector.selectKey(model.getId());

        if (keyOpt.isEmpty()) {
            throw AiProviderException.authError("__generic__",
                    String.format("模型 %s 没有可用的API Key", model.getCode()));
        }

        ModelApiKey selectedKey = keyOpt.get();
        log.debug("为模型 {} 选择了Key: {}", model.getCode(), selectedKey.getKeyName());

        return selectedKey;
    }

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

    private void recordKeyFailure(Long keyId) {
        try {
            modelApiKeySelector.recordFailure(keyId);
            log.warn("记录Key {} 使用失败", keyId);
        } catch (Exception e) {
            log.error("记录Key使用失败失败: keyId={}", keyId, e);
        }
    }

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

    private String buildOpenAiRequestBody(AiModel model, AiRequest request, boolean stream) {
        // 如果有原始请求体，基于它透传所有参数，只覆盖model和stream
        if (StrUtil.isNotBlank(request.getRawRequestBody())) {
            JSONObject body = JSONUtil.parseObj(request.getRawRequestBody());
            body.set("model", model.getCode());
            body.set("stream", stream);
            // 流式请求需要包含stream_options以获取usage数据
            if (stream) {
                JSONObject streamOptions = new JSONObject();
                streamOptions.set("include_usage", true);
                body.set("stream_options", streamOptions);
            }
            // 移除max_completion_tokens，统一用max_tokens（部分上游不支持新参数）
            Object maxCompletionTokens = body.get("max_completion_tokens");
            if (maxCompletionTokens != null) {
                body.remove("max_completion_tokens");
                if (!body.containsKey("max_tokens") || body.get("max_tokens") == null) {
                    body.set("max_tokens", maxCompletionTokens);
                }
            }
            return body.toString();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());
        body.put("messages", convertMessages(request.getMessages()));
        body.put("stream", stream);

        // 流式请求需要包含stream_options以获取usage数据
        if (stream) {
            Map<String, Object> streamOptions = new HashMap<>();
            streamOptions.put("include_usage", true);
            body.put("stream_options", streamOptions);
        }

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

        return JSONUtil.toJsonStr(body);
    }

    private Map<String, Object> buildTemplateVariables(AiModel model, AiRequest request, boolean stream) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("model", model.getCode());
        variables.put("messages", convertMessages(request.getMessages()));
        variables.put("stream", stream);
        variables.put("temperature", request.getTemperature());
        variables.put("maxTokens", request.getMaxTokens());
        variables.put("topP", request.getTopP());
        variables.put("frequencyPenalty", request.getFrequencyPenalty());
        variables.put("presencePenalty", request.getPresencePenalty());
        variables.put("stop", request.getStop());
        variables.put("user", request.getUser());
        return variables;
    }

    /**
     * 应用请求模板变量替换
     * 支持格式: ${model}, ${messages}, ${temperature} 等
     *
     * 注意：
     * - null值会被替换为JSON的null
     * - String值会被正确引用
     * - 对象/数组值会被序列化为JSON
     */
    private String applyRequestTemplate(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            Object value = entry.getValue();

            String replacement;
            if (value == null) {
                // null值替换为JSON null
                replacement = "null";
            } else if (value instanceof String) {
                // 字符串值需要加引号（如果模板中已有引号则直接替换）
                String strValue = (String) value;
                // 检查模板中占位符是否已被引号包围
                int placeholderIndex = result.indexOf(placeholder);
                if (placeholderIndex > 0 && result.charAt(placeholderIndex - 1) == '"') {
                    // 模板中已有引号，直接替换值
                    replacement = strValue;
                } else {
                    // 模板中没有引号，需要加引号
                    replacement = "\"" + strValue + "\"";
                }
            } else {
                // 其他类型（数字、布尔、数组、对象）序列化为JSON
                replacement = JSONUtil.toJsonStr(value);
            }
            result = result.replace(placeholder, replacement);
        }
        return result;
    }

    private AiResponse parseResponse(Provider provider, String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            if (provider != null && StrUtil.isNotBlank(provider.getResponseMapping())) {
                return parseResponseWithMapping(json, provider.getResponseMapping());
            }

            JSONArray choices = json.getJSONArray(CHOICES_FIELD);
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("响应中没有choices字段");
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject(MESSAGE_FIELD);
            String content = message != null ? message.getStr(CONTENT_FIELD) : null;
            String finishReason = choice.getStr("finish_reason");

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
                    .rawResponseBody(body)
                    .build();
        } catch (Exception e) {
            String providerCode = provider != null ? provider.getCode() : "__generic__";
            log.error("解析响应失败", e);
            throw AiProviderException.invalidRequest(providerCode, "Failed to parse response: " + e.getMessage());
        }
    }

    private AiResponse parseResponseWithMapping(JSONObject json, String mappingConfig) {
        JSONObject mapping = JSONUtil.parseObj(mappingConfig);

        String contentPath = mapping.getStr("content_path", "choices[0].message.content");
        String content = getValueByPath(json, contentPath);

        String idPath = mapping.getStr("id_path", "id");
        String id = getValueByPath(json, idPath);

        String modelPath = mapping.getStr("model_path", "model");
        String model = getValueByPath(json, modelPath);

        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        JSONObject usageMapping = mapping.getJSONObject("usage");
        if (usageMapping != null) {
            String promptPath = usageMapping.getStr("prompt_tokens", "usage.prompt_tokens");
            String completionPath = usageMapping.getStr("completion_tokens", "usage.completion_tokens");
            String totalPath = usageMapping.getStr("total_tokens", "usage.total_tokens");

            promptTokens = getIntValueByPath(json, promptPath);
            completionTokens = getIntValueByPath(json, completionPath);
            totalTokens = getIntValueByPath(json, totalPath);
        } else {
            promptTokens = getIntValueByPath(json, "usage.prompt_tokens");
            completionTokens = getIntValueByPath(json, "usage.completion_tokens");
            totalTokens = getIntValueByPath(json, "usage.total_tokens");
        }

        String finishReasonPath = mapping.getStr("finish_reason_path", "choices[0].finish_reason");
        String finishReason = getValueByPath(json, finishReasonPath);

        return AiResponse.builder()
                .id(id)
                .content(content)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .finishReason(finishReason)
                .model(model)
                .build();
    }

    private String getValueByPath(JSONObject json, String path) {
        try {
            Object value = getObjectByPath(json, path);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("获取路径值失败: path={}", path, e);
            return null;
        }
    }

    private Integer getIntValueByPath(JSONObject json, String path) {
        try {
            Object value = getObjectByPath(json, path);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return value != null ? Integer.parseInt(value.toString()) : null;
        } catch (Exception e) {
            log.debug("获取整数路径值失败: path={}", path, e);
            return null;
        }
    }

    private Object getObjectByPath(JSONObject json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;

        for (String part : parts) {
            if (current == null) return null;

            if (part.contains("[")) {
                int bracketStart = part.indexOf('[');
                int bracketEnd = part.indexOf(']');
                String fieldName = part.substring(0, bracketStart);
                int index = Integer.parseInt(part.substring(bracketStart + 1, bracketEnd));

                if (current instanceof JSONObject) {
                    JSONArray array = ((JSONObject) current).getJSONArray(fieldName);
                    current = array != null ? array.get(index) : null;
                } else {
                    return null;
                }
            } else {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(part);
                } else {
                    return null;
                }
            }
        }

        return current;
    }

    private AiStreamChunk parseChunk(Provider provider, String data) {
        try {
            JSONObject json = JSONUtil.parseObj(data);

            JSONArray choices = json.getJSONArray(CHOICES_FIELD);

            // 检查是否是独立的usage chunk（OpenAI协议中，usage数据可能在choices为空的最后一个chunk中）
            JSONObject usage = json.getJSONObject("usage");
            if ((choices == null || choices.isEmpty()) && usage != null) {
                return AiStreamChunk.builder()
                        .id(json.getStr("id"))
                        .model(json.getStr("model"))
                        .created(json.getLong("created"))
                        .done(true)
                        .promptTokens(usage.getInt("prompt_tokens"))
                        .completionTokens(usage.getInt("completion_tokens"))
                        .totalTokens(usage.getInt("total_tokens"))
                        .rawData(data)
                        .build();
            }

            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.getJSONObject(DELTA_FIELD);

            String content = delta != null ? delta.getStr(CONTENT_FIELD) : null;
            String finishReason = choice.getStr("finish_reason");

            boolean done = finishReason != null;

            AiStreamChunk.AiStreamChunkBuilder builder = AiStreamChunk.builder()
                    .id(json.getStr("id"))
                    .model(json.getStr("model"))
                    .created(json.getLong("created"))
                    .done(done)
                    .rawData(data);

            if (content != null) {
                builder.delta(content);
            }

            if (done) {
                builder.finishReason(finishReason);
                if (usage != null) {
                    builder.promptTokens(usage.getInt("prompt_tokens"))
                            .completionTokens(usage.getInt("completion_tokens"))
                            .totalTokens(usage.getInt("total_tokens"));
                }
            }

            return builder.build();
        } catch (Exception e) {
            String providerCode = provider != null ? provider.getCode() : "__generic__";
            log.error("解析流式数据块失败", e);
            return null;
        }
    }

    private Request buildEmbeddingRequest(Provider provider, AiModel model, String apiKey, AiRequest request) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(provider.getCode(), "API Key not configured");
        }

        String baseUrl = provider.getBaseUrl();
        String endpoint = provider.getEmbeddingEndpointOrDefault();
        String url = baseUrl.endsWith("/") ? baseUrl + endpoint.substring(1) : baseUrl + endpoint;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());

        if (request.getInput() != null) {
            body.put("input", request.getInput());
        } else {
            throw AiProviderException.invalidRequest(provider.getCode(), "Embedding请求必须提供input参数");
        }

        if (request.getEncodingFormat() != null) {
            body.put("encoding_format", request.getEncodingFormat());
        }
        if (request.getDimensions() != null) {
            body.put("dimensions", request.getDimensions());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("通用适配器Embedding请求: url={}, model={}", url, model.getCode());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody);

        String authHeader = provider.getAuthHeader() != null ? provider.getAuthHeader() : "Authorization";
        String authPrefix = provider.getAuthPrefixOrDefault();
        builder.header(authHeader, authPrefix + apiKey);

        if (StrUtil.isNotBlank(provider.getExtraHeaders())) {
            try {
                JSONObject extraHeaders = JSONUtil.parseObj(provider.getExtraHeaders());
                for (String key : extraHeaders.keySet()) {
                    builder.header(key, extraHeaders.getStr(key));
                }
            } catch (Exception e) {
                log.warn("解析额外请求头失败: {}", provider.getExtraHeaders(), e);
            }
        }

        return builder.build();
    }

    private EmbeddingResponse parseEmbeddingResponse(Provider provider, String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            if (provider != null && StrUtil.isNotBlank(provider.getResponseMapping())) {
                return parseEmbeddingResponseWithMapping(json, provider.getResponseMapping());
            }

            String id = json.getStr("id");
            String model = json.getStr("model");

            JSONArray dataArray = json.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                throw new IllegalStateException("Embedding响应中没有data字段");
            }

            List<EmbeddingResponse.EmbeddingData> embeddingDataList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObj = dataArray.getJSONObject(i);
                Integer index = dataObj.getInt("index");

                JSONArray embeddingArray = dataObj.getJSONArray("embedding");
                List<Float> embedding = new ArrayList<>();
                if (embeddingArray != null) {
                    for (int j = 0; j < embeddingArray.size(); j++) {
                        embedding.add(embeddingArray.getFloat(j));
                    }
                }

                EmbeddingResponse.EmbeddingData embeddingData = EmbeddingResponse.EmbeddingData.builder()
                        .index(index)
                        .embedding(embedding)
                        .build();
                embeddingDataList.add(embeddingData);
            }

            JSONObject usage = json.getJSONObject("usage");
            EmbeddingResponse.Usage usageObj = null;
            if (usage != null) {
                usageObj = EmbeddingResponse.Usage.builder()
                        .promptTokens(usage.getInt("prompt_tokens"))
                        .totalTokens(usage.getInt("total_tokens"))
                        .build();
            }

            return EmbeddingResponse.builder()
                    .id(id)
                    .model(model)
                    .data(embeddingDataList)
                    .usage(usageObj)
                    .build();
        } catch (Exception e) {
            String providerCode = provider != null ? provider.getCode() : "__generic__";
            log.error("解析Embedding响应失败", e);
            throw AiProviderException.invalidRequest(providerCode, "Failed to parse embedding response: " + e.getMessage());
        }
    }

    private EmbeddingResponse parseEmbeddingResponseWithMapping(JSONObject json, String mappingConfig) {
        JSONObject mapping = JSONUtil.parseObj(mappingConfig);

        String idPath = mapping.getStr("id_path", "id");
        String id = getValueByPath(json, idPath);

        String modelPath = mapping.getStr("model_path", "model");
        String model = getValueByPath(json, modelPath);

        String dataPath = mapping.getStr("data_path", "data");
        JSONArray dataArray = (JSONArray) getObjectByPath(json, dataPath);

        if (dataArray == null || dataArray.isEmpty()) {
            throw new IllegalStateException("Embedding响应中没有data字段");
        }

        List<EmbeddingResponse.EmbeddingData> embeddingDataList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObj = dataArray.getJSONObject(i);
            Integer index = dataObj.getInt("index");

            String embeddingPath = mapping.getStr("embedding_path", "embedding");
            JSONArray embeddingArray = (JSONArray) getObjectByPath(dataObj, embeddingPath);

            List<Float> embedding = new ArrayList<>();
            if (embeddingArray != null) {
                for (int j = 0; j < embeddingArray.size(); j++) {
                    embedding.add(embeddingArray.getFloat(j));
                }
            }

            EmbeddingResponse.EmbeddingData embeddingData = EmbeddingResponse.EmbeddingData.builder()
                    .index(index)
                    .embedding(embedding)
                    .build();
            embeddingDataList.add(embeddingData);
        }

        Integer promptTokens = null;
        Integer totalTokens = null;

        JSONObject usageMapping = mapping.getJSONObject("usage");
        if (usageMapping != null) {
            String promptPath = usageMapping.getStr("prompt_tokens", "usage.prompt_tokens");
            String totalPath = usageMapping.getStr("total_tokens", "usage.total_tokens");

            promptTokens = getIntValueByPath(json, promptPath);
            totalTokens = getIntValueByPath(json, totalPath);
        } else {
            promptTokens = getIntValueByPath(json, "usage.prompt_tokens");
            totalTokens = getIntValueByPath(json, "usage.total_tokens");
        }

        EmbeddingResponse.Usage usageObj = null;
        if (promptTokens != null || totalTokens != null) {
            usageObj = EmbeddingResponse.Usage.builder()
                    .promptTokens(promptTokens)
                    .totalTokens(totalTokens)
                    .build();
        }

        return EmbeddingResponse.builder()
                .id(id)
                .model(model)
                .data(embeddingDataList)
                .usage(usageObj)
                .build();
    }

    private AiResponse convertEmbeddingResponse(EmbeddingResponse embeddingResponse) {
        if (embeddingResponse.getData() == null || embeddingResponse.getData().isEmpty()) {
            throw new IllegalStateException("Embedding响应中没有数据");
        }

        EmbeddingResponse.EmbeddingData firstData = embeddingResponse.getData().get(0);
        List<Float> embedding = firstData.getEmbedding();

        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("Embedding数据为空");
        }

        List<Double> doubleEmbedding = embedding.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList());

        Integer promptTokens = null;
        Integer totalTokens = null;
        if (embeddingResponse.getUsage() != null) {
            promptTokens = embeddingResponse.getUsage().getPromptTokens();
            totalTokens = embeddingResponse.getUsage().getTotalTokens();
        }

        return AiResponse.builder()
                .id(embeddingResponse.getId())
                .model(embeddingResponse.getModel())
                .embedding(doubleEmbedding)
                .promptTokens(promptTokens)
                .totalTokens(totalTokens)
                .build();
    }

    private Request buildImageRequest(Provider provider, AiModel model, String apiKey, AiRequest request) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(provider.getCode(), "API Key not configured");
        }

        String baseUrl = provider.getBaseUrl();
        String endpoint = provider.getImageEndpointOrDefault();
        String url = baseUrl.endsWith("/") ? baseUrl + endpoint.substring(1) : baseUrl + endpoint;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());

        if (request.getPrompt() != null) {
            body.put("prompt", request.getPrompt());
        } else {
            throw AiProviderException.invalidRequest(provider.getCode(), "Image请求必须提供prompt参数");
        }

        if (request.getNegativePrompt() != null) {
            body.put("negative_prompt", request.getNegativePrompt());
        }

        if (request.getN() != null) {
            body.put("n", request.getN());
        }

        if (request.getSize() != null) {
            body.put("size", request.getSize());
        }

        if (request.getResponseFormat() != null) {
            body.put("response_format", request.getResponseFormat());
        }

        if (request.getStyle() != null) {
            body.put("style", request.getStyle());
        }

        if (request.getQuality() != null) {
            body.put("quality", request.getQuality());
        }

        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("通用适配器Image请求: url={}, model={}", url, model.getCode());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody);

        String authHeader = provider.getAuthHeader() != null ? provider.getAuthHeader() : "Authorization";
        String authPrefix = provider.getAuthPrefixOrDefault();
        builder.header(authHeader, authPrefix + apiKey);

        if (StrUtil.isNotBlank(provider.getExtraHeaders())) {
            try {
                JSONObject extraHeaders = JSONUtil.parseObj(provider.getExtraHeaders());
                for (String key : extraHeaders.keySet()) {
                    builder.header(key, extraHeaders.getStr(key));
                }
            } catch (Exception e) {
                log.warn("解析额外请求头失败: {}", provider.getExtraHeaders(), e);
            }
        }

        return builder.build();
    }

    private Request buildVideoRequest(Provider provider, AiModel model, String apiKey, AiRequest request) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(provider.getCode(), "API Key not configured");
        }

        String baseUrl = provider.getBaseUrl();
        String endpoint = provider.getVideoEndpointOrDefault();
        String url = baseUrl.endsWith("/") ? baseUrl + endpoint.substring(1) : baseUrl + endpoint;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());

        if (request.getPrompt() != null) {
            body.put("prompt", request.getPrompt());
        } else {
            throw AiProviderException.invalidRequest(provider.getCode(), "Video请求必须提供prompt参数");
        }

        if (request.getImage() != null) {
            body.put("image", request.getImage());
        }

        if (request.getN() != null) {
            body.put("n", request.getN());
        }

        if (request.getDuration() != null) {
            body.put("duration", request.getDuration());
        }

        if (request.getSize() != null) {
            body.put("size", request.getSize());
        }

        if (request.getAspectRatio() != null) {
            body.put("aspect_ratio", request.getAspectRatio());
        }

        if (request.getResponseFormat() != null) {
            body.put("response_format", request.getResponseFormat());
        }

        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("通用适配器Video请求: url={}, model={}", url, model.getCode());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody);

        String authHeader = provider.getAuthHeader() != null ? provider.getAuthHeader() : "Authorization";
        String authPrefix = provider.getAuthPrefixOrDefault();
        builder.header(authHeader, authPrefix + apiKey);

        if (StrUtil.isNotBlank(provider.getExtraHeaders())) {
            try {
                JSONObject extraHeaders = JSONUtil.parseObj(provider.getExtraHeaders());
                for (String key : extraHeaders.keySet()) {
                    builder.header(key, extraHeaders.getStr(key));
                }
            } catch (Exception e) {
                log.warn("解析额外请求头失败: {}", provider.getExtraHeaders(), e);
            }
        }

        return builder.build();
    }

    private ImageResponse parseImageResponse(Provider provider, String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            if (provider != null && StrUtil.isNotBlank(provider.getResponseMapping())) {
                return parseImageResponseWithMapping(json, provider.getResponseMapping());
            }

            Long created = json.getLong("created");

            JSONArray dataArray = json.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                throw new IllegalStateException("Image响应中没有data字段");
            }

            List<ImageResponse.ImageData> imageDataList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObj = dataArray.getJSONObject(i);

                ImageResponse.ImageData.ImageDataBuilder builder = ImageResponse.ImageData.builder();

                String url = dataObj.getStr("url");
                if (StrUtil.isNotBlank(url)) {
                    builder.url(url);
                }

                String b64Json = dataObj.getStr("b64_json");
                if (StrUtil.isNotBlank(b64Json)) {
                    builder.b64Json(b64Json);
                }

                String revisedPrompt = dataObj.getStr("revised_prompt");
                if (StrUtil.isNotBlank(revisedPrompt)) {
                    builder.revisedPrompt(revisedPrompt);
                }

                imageDataList.add(builder.build());
            }

            return ImageResponse.builder()
                    .created(created)
                    .data(imageDataList)
                    .build();
        } catch (Exception e) {
            String providerCode = provider != null ? provider.getCode() : "__generic__";
            log.error("解析Image响应失败", e);
            throw AiProviderException.invalidRequest(providerCode, "Failed to parse image response: " + e.getMessage());
        }
    }

    private ImageResponse parseImageResponseWithMapping(JSONObject json, String mappingConfig) {
        JSONObject mapping = JSONUtil.parseObj(mappingConfig);

        Long created = null;
        String createdPath = mapping.getStr("created_path", "created");
        Object createdObj = getObjectByPath(json, createdPath);
        if (createdObj instanceof Number) {
            created = ((Number) createdObj).longValue();
        }

        String dataPath = mapping.getStr("data_path", "data");
        JSONArray dataArray = (JSONArray) getObjectByPath(json, dataPath);

        if (dataArray == null || dataArray.isEmpty()) {
            throw new IllegalStateException("Image响应中没有data字段");
        }

        List<ImageResponse.ImageData> imageDataList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObj = dataArray.getJSONObject(i);

            ImageResponse.ImageData.ImageDataBuilder builder = ImageResponse.ImageData.builder();

            String urlPath = mapping.getStr("url_path", "url");
            String url = getValueByPath(dataObj, urlPath);
            if (StrUtil.isNotBlank(url)) {
                builder.url(url);
            }

            String b64JsonPath = mapping.getStr("b64_json_path", "b64_json");
            String b64Json = getValueByPath(dataObj, b64JsonPath);
            if (StrUtil.isNotBlank(b64Json)) {
                builder.b64Json(b64Json);
            }

            String revisedPromptPath = mapping.getStr("revised_prompt_path", "revised_prompt");
            String revisedPrompt = getValueByPath(dataObj, revisedPromptPath);
            if (StrUtil.isNotBlank(revisedPrompt)) {
                builder.revisedPrompt(revisedPrompt);
            }

            imageDataList.add(builder.build());
        }

        return ImageResponse.builder()
                .created(created)
                .data(imageDataList)
                .build();
    }

    private AiResponse convertImageResponse(ImageResponse imageResponse) {
        if (imageResponse.getData() == null || imageResponse.getData().isEmpty()) {
            throw new IllegalStateException("Image响应中没有数据");
        }

        List<String> imageUrls = new ArrayList<>();
        for (ImageResponse.ImageData data : imageResponse.getData()) {
            if (StrUtil.isNotBlank(data.getUrl())) {
                imageUrls.add(data.getUrl());
            }
            if (StrUtil.isNotBlank(data.getB64Json())) {
                imageUrls.add("data:image/png;base64," + data.getB64Json());
            }
        }

        return AiResponse.builder()
                .imageUrls(imageUrls)
                .created(imageResponse.getCreated())
                .build();
    }

    private VideoResponse parseVideoResponse(Provider provider, String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);

            if (provider != null && StrUtil.isNotBlank(provider.getResponseMapping())) {
                return parseVideoResponseWithMapping(json, provider.getResponseMapping());
            }

            String id = json.getStr("id");
            String model = json.getStr("model");
            Long created = json.getLong("created");

            JSONArray dataArray = json.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                throw new IllegalStateException("Video响应中没有data字段");
            }

            List<VideoResponse.VideoData> videoDataList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObj = dataArray.getJSONObject(i);

                VideoResponse.VideoData.VideoDataBuilder builder = VideoResponse.VideoData.builder();

                String url = dataObj.getStr("url");
                if (StrUtil.isNotBlank(url)) {
                    builder.url(url);
                }

                String b64Json = dataObj.getStr("b64_json");
                if (StrUtil.isNotBlank(b64Json)) {
                    builder.b64Json(b64Json);
                }

                String revisedPrompt = dataObj.getStr("revised_prompt");
                if (StrUtil.isNotBlank(revisedPrompt)) {
                    builder.revisedPrompt(revisedPrompt);
                }

                Integer duration = dataObj.getInt("duration");
                if (duration != null) {
                    builder.duration(duration);
                }

                String size = dataObj.getStr("size");
                if (StrUtil.isNotBlank(size)) {
                    builder.size(size);
                }

                String aspectRatio = dataObj.getStr("aspect_ratio");
                if (StrUtil.isNotBlank(aspectRatio)) {
                    builder.aspectRatio(aspectRatio);
                }

                videoDataList.add(builder.build());
            }

            return VideoResponse.builder()
                    .id(id)
                    .model(model)
                    .created(created)
                    .data(videoDataList)
                    .build();
        } catch (Exception e) {
            String providerCode = provider != null ? provider.getCode() : "__generic__";
            log.error("解析Video响应失败", e);
            throw AiProviderException.invalidRequest(providerCode, "Failed to parse video response: " + e.getMessage());
        }
    }

    private VideoResponse parseVideoResponseWithMapping(JSONObject json, String mappingConfig) {
        JSONObject mapping = JSONUtil.parseObj(mappingConfig);

        String idPath = mapping.getStr("id_path", "id");
        String id = getValueByPath(json, idPath);

        String modelPath = mapping.getStr("model_path", "model");
        String model = getValueByPath(json, modelPath);

        Long created = null;
        String createdPath = mapping.getStr("created_path", "created");
        Object createdObj = getObjectByPath(json, createdPath);
        if (createdObj instanceof Number) {
            created = ((Number) createdObj).longValue();
        }

        String dataPath = mapping.getStr("data_path", "data");
        JSONArray dataArray = (JSONArray) getObjectByPath(json, dataPath);

        if (dataArray == null || dataArray.isEmpty()) {
            throw new IllegalStateException("Video响应中没有data字段");
        }

        List<VideoResponse.VideoData> videoDataList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObj = dataArray.getJSONObject(i);

            VideoResponse.VideoData.VideoDataBuilder builder = VideoResponse.VideoData.builder();

            String urlPath = mapping.getStr("url_path", "url");
            String url = getValueByPath(dataObj, urlPath);
            if (StrUtil.isNotBlank(url)) {
                builder.url(url);
            }

            String b64JsonPath = mapping.getStr("b64_json_path", "b64_json");
            String b64Json = getValueByPath(dataObj, b64JsonPath);
            if (StrUtil.isNotBlank(b64Json)) {
                builder.b64Json(b64Json);
            }

            String revisedPromptPath = mapping.getStr("revised_prompt_path", "revised_prompt");
            String revisedPrompt = getValueByPath(dataObj, revisedPromptPath);
            if (StrUtil.isNotBlank(revisedPrompt)) {
                builder.revisedPrompt(revisedPrompt);
            }

            String durationPath = mapping.getStr("duration_path", "duration");
            Integer duration = getIntValueByPath(dataObj, durationPath);
            if (duration != null) {
                builder.duration(duration);
            }

            String sizePath = mapping.getStr("size_path", "size");
            String size = getValueByPath(dataObj, sizePath);
            if (StrUtil.isNotBlank(size)) {
                builder.size(size);
            }

            String aspectRatioPath = mapping.getStr("aspect_ratio_path", "aspect_ratio");
            String aspectRatio = getValueByPath(dataObj, aspectRatioPath);
            if (StrUtil.isNotBlank(aspectRatio)) {
                builder.aspectRatio(aspectRatio);
            }

            videoDataList.add(builder.build());
        }

        return VideoResponse.builder()
                .id(id)
                .model(model)
                .created(created)
                .data(videoDataList)
                .build();
    }

    private AiResponse convertVideoResponse(VideoResponse videoResponse) {
        if (videoResponse.getData() == null || videoResponse.getData().isEmpty()) {
            throw new IllegalStateException("Video响应中没有数据");
        }

        List<String> videoUrls = new ArrayList<>();
        for (VideoResponse.VideoData data : videoResponse.getData()) {
            if (StrUtil.isNotBlank(data.getUrl())) {
                videoUrls.add(data.getUrl());
            }
            if (StrUtil.isNotBlank(data.getB64Json())) {
                videoUrls.add("data:video/mp4;base64," + data.getB64Json());
            }
        }

        return AiResponse.builder()
                .id(videoResponse.getId())
                .model(videoResponse.getModel())
                .videoUrls(videoUrls)
                .created(videoResponse.getCreated())
                .build();
    }

    private AiProviderException handleError(Provider provider, Response response) throws IOException {
        int code = response.code();
        String providerCode = provider.getCode();

        ResponseBody body = response.body();
        String bodyString = body != null ? body.string() : "空响应";

        String errorMessage = bodyString;
        try {
            if (StrUtil.isNotBlank(bodyString) && !"空响应".equals(bodyString)) {
                JSONObject json = JSONUtil.parseObj(bodyString);
                JSONObject error = json.getJSONObject(ERROR_FIELD);
                if (error != null) {
                    errorMessage = error.getStr(MESSAGE_FIELD, bodyString);
                }
            }
        } catch (Exception e) {
            log.warn("解析错误响应失败", e);
        }

        log.error("通用适配器API错误: provider={}, code={}, message={}", providerCode, code, errorMessage);

        return switch (code) {
            case 401, 403 -> AiProviderException.authError(providerCode, errorMessage);
            case 429 -> AiProviderException.rateLimitError(providerCode, errorMessage);
            case 400 -> AiProviderException.invalidRequest(providerCode, errorMessage);
            case 404 -> AiProviderException.modelNotFound(providerCode, errorMessage);
            default -> code >= 500
                    ? AiProviderException.serverError(providerCode, errorMessage, code)
                    : AiProviderException.networkError(providerCode, errorMessage, null);
        };
    }
}