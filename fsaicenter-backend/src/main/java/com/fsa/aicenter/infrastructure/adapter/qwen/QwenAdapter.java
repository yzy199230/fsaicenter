package com.fsa.aicenter.infrastructure.adapter.qwen;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.omni.*;
import com.alibaba.dashscope.audio.qwen_tts_realtime.*;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.fsa.aicenter.application.service.ModelApiKeySelector;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import com.fsa.aicenter.infrastructure.adapter.common.*;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 通义千问适配器实现
 * <p>
 * 支持通义千问的API，包括非流式和流式调用
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class QwenAdapter implements AiProviderAdapter {

    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final String QWEN_VL_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String SSE_DATA_PREFIX = "data:";
    private static final String SSE_DONE_MARKER = "[DONE]";

    // JSON字段常量
    private static final String EMPTY_RESPONSE = "空响应";
    private static final String ERROR_FIELD = "error";
    private static final String MESSAGE_FIELD = "message";
    private static final String CHOICES_FIELD = "choices";
    private static final String CONTENT_FIELD = "content";
    private static final String INPUT_FIELD = "input";
    private static final String PARAMETERS_FIELD = "parameters";
    private static final String MESSAGES_FIELD = "messages";
    private static final String OUTPUT_FIELD = "output";

    private final OkHttpClient httpClient;
    private final ModelApiKeySelector modelApiKeySelector;

    public QwenAdapter(
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
        return "qwen";
    }

    @Override
    public Mono<AiResponse> call(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("通义千问非流式调用开始: model={}, messages={}", model.getCode(), request.getMessages().size());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, selectedKey.getApiKey(), request, false);

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
                log.debug("通义千问响应: {}", bodyString);

                AiResponse aiResponse = parseResponse(bodyString);

                // 记录成功并消费配额
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通义千问调用失败", e);
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
            log.debug("通义千问流式调用开始: model={}, messages={}, keyName={}",
                    model.getCode(), request.getMessages().size(), selectedKey.getKeyName());

            // 构建HTTP请求
            Request httpRequest = buildHttpRequest(model, selectedKey.getApiKey(), request, true);
            Call call = httpClient.newCall(httpRequest);

            // Token计数器
            final int[] totalTokens = {0};

            // 注册取消回调
            sink.onDispose(() -> {
                if (!call.isCanceled()) {
                    call.cancel();
                    log.debug("通义千问流式调用已取消");
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
                        log.error("通义千问流式调用失败", e);
                        recordKeyFailure(selectedKey.getId());
                        sink.error(AiProviderException.networkError(
                                getProviderCode(), "Stream reading error: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("通义千问流式调用网络失败", e);
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

        // 通义千问使用 input.messages 结构
        Map<String, Object> input = new HashMap<>();
        input.put(MESSAGES_FIELD, convertMessages(request.getMessages()));
        body.put(INPUT_FIELD, input);

        // 参数放在 parameters 对象中
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message"); // 固定使用message格式

        // 可选参数
        if (request.getTemperature() != null) {
            parameters.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            parameters.put("top_p", request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            parameters.put("max_tokens", request.getMaxTokens());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            parameters.put("stop", request.getStop());
        }

        body.put(PARAMETERS_FIELD, parameters);

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
     */
    private Request buildHttpRequest(AiModel model, String apiKey, AiRequest request, boolean stream) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key not configured");
        }

        // 调用 convertRequest 构建请求体
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) convertRequest(request);

        // 添加模型参数
        body.put("model", model.getCode());

        // 流式时添加增量输出参数
        if (stream) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) body.get(PARAMETERS_FIELD);
            parameters.put("incremental_output", true);
        }

        String json = JSONUtil.toJsonStr(body);
        log.debug("通义千问请求: model={}, messages={}", model.getCode(), request.getMessages().size());

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request.Builder builder = new Request.Builder()
                .url(QWEN_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody);

        // 流式调用需要添加SSE header
        if (stream) {
            builder.header("X-DashScope-SSE", "enable");
        }

        return builder.build();
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

            // 通义千问响应格式：output.choices[0].message.content
            JSONObject output = json.getJSONObject(OUTPUT_FIELD);
            if (output == null) {
                throw AiProviderException.serverError(getProviderCode(), "响应output为空", 200);
            }

            JSONArray choices = output.getJSONArray(CHOICES_FIELD);
            if (CollUtil.isEmpty(choices)) {
                throw AiProviderException.serverError(getProviderCode(), "响应choices为空", 200);
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject(MESSAGE_FIELD);
            String content = message.getStr(CONTENT_FIELD);
            String finishReason = choice.getStr("finish_reason");

            // Token统计：usage.input_tokens / usage.output_tokens
            JSONObject usage = json.getJSONObject("usage");
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;

            if (usage != null) {
                inputTokens = usage.getInt("input_tokens");
                outputTokens = usage.getInt("output_tokens");
                if (inputTokens != null && outputTokens != null) {
                    totalTokens = inputTokens + outputTokens;
                }
            }

            return AiResponse.builder()
                    .id(json.getStr("request_id"))
                    .content(content)
                    .finishReason(finishReason)
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .model(json.getStr("model"))
                    .build();
        } catch (Exception e) {
            log.error("解析通义千问响应失败: {}", bodyString, e);
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

            // 通义千问流式响应格式：output.choices[0].message.content
            JSONObject output = json.getJSONObject(OUTPUT_FIELD);
            if (output == null) {
                return null;
            }

            JSONArray choices = output.getJSONArray(CHOICES_FIELD);
            if (CollUtil.isEmpty(choices)) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject(MESSAGE_FIELD);

            // 提取增量内容
            String content = message != null ? message.getStr(CONTENT_FIELD) : null;
            String finishReason = choice.getStr("finish_reason");

            // 检查是否完成
            boolean done = finishReason != null;

            AiStreamChunk.AiStreamChunkBuilder builder = AiStreamChunk.builder()
                    .id(json.getStr("request_id"))
                    .model(json.getStr("model"))
                    .done(done);

            if (content != null) {
                builder.delta(content);
            }

            if (done) {
                builder.finishReason(finishReason);
                // 提取usage信息
                JSONObject usage = json.getJSONObject("usage");
                if (usage != null) {
                    Integer inputTokens = usage.getInt("input_tokens");
                    Integer outputTokens = usage.getInt("output_tokens");
                    builder.promptTokens(inputTokens)
                            .completionTokens(outputTokens)
                            .totalTokens(inputTokens != null && outputTokens != null
                                    ? inputTokens + outputTokens : null);
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("解析通义千问流式数据块失败: {}", data, e);
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

                // 通义千问错误格式可能是 error.message 或 message
                JSONObject error = json.getJSONObject(ERROR_FIELD);
                if (error != null) {
                    errorMessage = error.getStr(MESSAGE_FIELD, bodyString);
                } else {
                    errorMessage = json.getStr(MESSAGE_FIELD, bodyString);
                }
            }
        } catch (Exception e) {
            // 解析失败，使用原始body
            log.warn("解析通义千问错误响应失败", e);
        }

        log.error("通义千问 API错误: code={}, message={}", code, errorMessage);

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

    // ========== 图像识别 ==========

    /**
     * 图像识别/视觉分析
     * <p>
     * 使用通义千问VL系列模型进行图像识别，支持qwen-vl-plus、qwen-vl-max等模型
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用image字段和messages）
     * @return 响应结果的Mono（包含content字段）
     */
    @Override
    public Mono<AiResponse> imageRecognition(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("通义千问图像识别调用开始: model={}", model.getCode());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            // 构建多模态HTTP请求
            Request httpRequest = buildVisionHttpRequest(model, selectedKey.getApiKey(), request);

            // 同步调用
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
                log.debug("通义千问图像识别响应: {}", bodyString);

                AiResponse aiResponse = parseVisionResponse(bodyString);

                // 记录成功并消费配额
                recordKeySuccess(selectedKey.getId(), aiResponse.getTotalTokens());

                return aiResponse;
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                log.error("通义千问图像识别调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 构建视觉模型HTTP请求
     */
    private Request buildVisionHttpRequest(AiModel model, String apiKey, AiRequest request) {
        if (StrUtil.isBlank(apiKey)) {
            throw AiProviderException.authError(getProviderCode(), "API Key not configured");
        }

        // 构建多模态请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());

        // 构建多模态消息
        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = buildVisionMessages(request);
        input.put(MESSAGES_FIELD, messages);
        body.put(INPUT_FIELD, input);

        // 参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        if (request.getMaxTokens() != null) {
            parameters.put("max_tokens", request.getMaxTokens());
        }
        body.put(PARAMETERS_FIELD, parameters);

        String json = JSONUtil.toJsonStr(body);
        log.debug("通义千问图像识别请求: {}", json);

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

        return new Request.Builder()
                .url(QWEN_VL_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    /**
     * 构建视觉模型消息
     * <p>
     * 通义千问VL模型消息格式：
     * content: [{"image": "url"}, {"text": "prompt"}]
     * </p>
     */
    private List<Map<String, Object>> buildVisionMessages(AiRequest request) {
        List<Map<String, Object>> messages = new java.util.ArrayList<>();

        // 构建用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        // content是一个数组，包含图片和文字
        List<Map<String, String>> contentParts = new java.util.ArrayList<>();

        // 添加图片
        if (StrUtil.isNotBlank(request.getImage())) {
            Map<String, String> imagePart = new HashMap<>();
            imagePart.put("image", request.getImage());
            contentParts.add(imagePart);
        }

        // 添加文本提示
        String textPrompt = "请描述这张图片的内容";
        if (CollUtil.isNotEmpty(request.getMessages())) {
            // 使用用户消息中的文本
            textPrompt = request.getMessages().stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .map(Message::getContent)
                    .findFirst()
                    .orElse(textPrompt);
        }
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", textPrompt);
        contentParts.add(textPart);

        userMessage.put("content", contentParts);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 解析视觉模型响应
     */
    private AiResponse parseVisionResponse(String bodyString) {
        try {
            JSONObject json = JSONUtil.parseObj(bodyString);

            // 通义千问VL响应格式与普通格式一致
            JSONObject output = json.getJSONObject(OUTPUT_FIELD);
            if (output == null) {
                throw AiProviderException.serverError(getProviderCode(), "响应output为空", 200);
            }

            JSONArray choices = output.getJSONArray(CHOICES_FIELD);
            if (CollUtil.isEmpty(choices)) {
                throw AiProviderException.serverError(getProviderCode(), "响应choices为空", 200);
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject(MESSAGE_FIELD);

            // VL模型的content可能是数组格式
            String content = extractVisionContent(message);
            String finishReason = choice.getStr("finish_reason");

            // Token统计
            JSONObject usage = json.getJSONObject("usage");
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;

            if (usage != null) {
                inputTokens = usage.getInt("input_tokens");
                outputTokens = usage.getInt("output_tokens");
                if (inputTokens != null && outputTokens != null) {
                    totalTokens = inputTokens + outputTokens;
                }
            }

            return AiResponse.builder()
                    .id(json.getStr("request_id"))
                    .content(content)
                    .finishReason(finishReason)
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .model(json.getStr("model"))
                    .build();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析通义千问图像识别响应失败: {}", bodyString, e);
            throw AiProviderException.invalidRequest(getProviderCode(),
                    "Failed to parse vision response: " + e.getMessage());
        }
    }

    /**
     * 提取视觉模型响应内容
     * <p>
     * VL模型的content可能是字符串或数组格式
     * </p>
     */
    private String extractVisionContent(JSONObject message) {
        if (message == null) {
            return null;
        }

        Object contentObj = message.get(CONTENT_FIELD);
        if (contentObj == null) {
            return null;
        }

        // 如果是字符串，直接返回
        if (contentObj instanceof String) {
            return (String) contentObj;
        }

        // 如果是数组，提取text字段
        if (contentObj instanceof JSONArray contentArray) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contentArray.size(); i++) {
                JSONObject part = contentArray.getJSONObject(i);
                if (part != null && part.containsKey("text")) {
                    sb.append(part.getStr("text"));
                }
            }
            return sb.toString();
        }

        return contentObj.toString();
    }

    // ========== 语音识别（ASR）==========

    /**
     * 通义千问语音识别API URL
     * <p>
     * 支持 sensevoice-v1, paraformer-v2 等模型（文件转写）
     * </p>
     */
    private static final String QWEN_ASR_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription";

    /**
     * 实时ASR WebSocket URL
     */
    private static final String QWEN_ASR_REALTIME_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";

    /**
     * 音频分块大小（字节）
     */
    private static final int AUDIO_CHUNK_SIZE = 3200;

    /**
     * 语音识别（ASR）
     * <p>
     * 支持两种模式：
     * <ul>
     *   <li>实时ASR：qwen3-asr-flash-realtime 等模型，使用 WebSocket</li>
     *   <li>文件转写：sensevoice-v1, paraformer-v2 等模型，使用 HTTP API</li>
     * </ul>
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用audio字段，支持Base64或URL）
     * @return 响应结果的Mono（包含content字段为识别文本）
     */
    @Override
    public Mono<AiResponse> speechToText(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("通义千问ASR调用开始: model={}", model.getCode());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            if (StrUtil.isBlank(selectedKey.getApiKey())) {
                throw AiProviderException.authError(getProviderCode(), "API Key not configured");
            }

            // 验证音频输入
            if (StrUtil.isBlank(request.getAudio())) {
                throw AiProviderException.invalidRequest(getProviderCode(), "音频数据不能为空");
            }

            try {
                String transcribedText;

                // 根据模型和音频格式选择API
                String audioFormat = request.getAudioFormat();
                if (shouldUseRealtimeAsr(model.getCode(), audioFormat)) {
                    // 使用实时ASR（WebSocket）- 仅支持PCM格式
                    transcribedText = callRealtimeAsr(model, selectedKey.getApiKey(), request);
                } else {
                    // 使用文件转写API - 支持MP3/WAV/FLAC等格式
                    String taskId = submitAsrTask(model, selectedKey.getApiKey(), request);
                    transcribedText = pollAsrResult(selectedKey.getApiKey(), taskId);
                }

                // 记录成功
                recordKeySuccess(selectedKey.getId(), null);

                return AiResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .content(transcribedText)
                        .model(model.getCode())
                        .build();
            } catch (AiProviderException e) {
                recordKeyFailure(selectedKey.getId());
                throw e;
            } catch (Exception e) {
                log.error("通义千问ASR调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 判断是否应该使用实时ASR
     * <p>
     * 只有同时满足以下条件才使用实时ASR：
     * <ul>
     *   <li>模型名包含 "realtime"</li>
     *   <li>音频格式是 PCM（实时ASR只支持PCM格式）</li>
     * </ul>
     * 其他格式（MP3/WAV/FLAC等）使用文件转写API
     * </p>
     */
    private boolean shouldUseRealtimeAsr(String modelCode, String audioFormat) {
        // 检查是否是realtime模型
        boolean isRealtimeModel = modelCode != null && modelCode.contains("realtime");
        if (!isRealtimeModel) {
            return false;
        }

        // 检查音频格式是否支持实时ASR（仅PCM格式）
        if (StrUtil.isBlank(audioFormat)) {
            // 默认使用文件转写（更通用）
            return false;
        }

        String format = audioFormat.toLowerCase();
        // 实时ASR只支持PCM格式
        return "pcm".equals(format) || "pcm16".equals(format);
    }

    /**
     * 使用实时ASR进行流式语音识别（DashScope SDK）
     * <p>
     * 支持 qwen3-asr-flash-realtime 等实时模型，返回流式识别结果
     * </p>
     *
     * @param model   AI模型聚合根
     * @param apiKey  API密钥
     * @param request 统一请求参数
     * @return 流式响应的Flux，每个元素是一个识别文本块
     */
    private Flux<AiStreamChunk> callRealtimeAsrStream(AiModel model, String apiKey, AiRequest request) {
        return Flux.create(sink -> {
            log.debug("通义千问实时ASR流式调用开始: model={}", model.getCode());

            // 获取音频数据（Base64）
            String audioBase64 = request.getAudio();

            // 如果是URL，需要先下载
            if (audioBase64.startsWith("http://") || audioBase64.startsWith("https://")) {
                try {
                    audioBase64 = downloadAudioAsBase64(audioBase64);
                } catch (IOException e) {
                    sink.error(AiProviderException.networkError(getProviderCode(), "下载音频文件失败: " + e.getMessage(), e));
                    return;
                }
            }

            // 解码音频数据
            final byte[] audioBytes;
            try {
                audioBytes = Base64.getDecoder().decode(audioBase64);
            } catch (IllegalArgumentException e) {
                sink.error(AiProviderException.invalidRequest(getProviderCode(), "无效的Base64音频数据"));
                return;
            }

            // 配置转写参数
            final String audioFormat = StrUtil.isNotBlank(request.getAudioFormat()) ? request.getAudioFormat() : "pcm";
            final int sampleRate = 16000;

            // 构建实时ASR参数
            OmniRealtimeParam param = OmniRealtimeParam.builder()
                    .model(model.getCode())
                    .url(QWEN_ASR_REALTIME_URL)
                    .apikey(apiKey)
                    .build();

            // 用于同步session创建
            CountDownLatch sessionCreatedLatch = new CountDownLatch(1);
            AtomicReference<Exception> errorRef = new AtomicReference<>();

            // 创建实时ASR会话
            AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>();
            OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
                @Override
                public void onOpen() {
                    log.debug("实时ASR流式连接已建立");
                }

                @Override
                public void onEvent(JsonObject message) {
                    String type = message.get("type").getAsString();
                    switch (type) {
                        case "session.created":
                            log.debug("实时ASR流式会话已创建");
                            try {
                                OmniRealtimeConversation conv = conversationRef.get();
                                if (conv != null) {
                                    OmniRealtimeTranscriptionParam transcriptionParam = new OmniRealtimeTranscriptionParam();
                                    transcriptionParam.setLanguage("zh");
                                    transcriptionParam.setInputAudioFormat(audioFormat);
                                    transcriptionParam.setInputSampleRate(sampleRate);

                                    OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                                            .modalities(Collections.singletonList(OmniRealtimeModality.TEXT))
                                            .transcriptionConfig(transcriptionParam)
                                            .build();
                                    conv.updateSession(config);
                                }
                            } catch (Exception e) {
                                log.error("配置ASR流式会话失败", e);
                                errorRef.set(e);
                            }
                            sessionCreatedLatch.countDown();
                            break;
                        case "session.updated":
                            log.debug("实时ASR流式会话配置已更新");
                            break;
                        case "conversation.item.input_audio_transcription.completed":
                            // 最终识别结果
                            if (message.has("transcript")) {
                                String transcript = message.get("transcript").getAsString();
                                if (StrUtil.isNotBlank(transcript)) {
                                    log.debug("实时ASR流式识别完成: {}", transcript);
                                    // 发送最终结果
                                    AiStreamChunk chunk = AiStreamChunk.builder()
                                            .id(UUID.randomUUID().toString())
                                            .model(model.getCode())
                                            .delta(transcript)
                                            .done(false)
                                            .build();
                                    sink.next(chunk);
                                }
                            }
                            // 发送完成标记
                            AiStreamChunk doneChunk = AiStreamChunk.builder()
                                    .id(UUID.randomUUID().toString())
                                    .model(model.getCode())
                                    .done(true)
                                    .finishReason("stop")
                                    .build();
                            sink.next(doneChunk);
                            sink.complete();
                            break;
                        case "conversation.item.input_audio_transcription.text":
                            // 中间识别结果（流式）
                            if (message.has("text")) {
                                String text = message.get("text").getAsString();
                                if (StrUtil.isNotBlank(text)) {
                                    log.debug("实时ASR流式中间结果: {}", text);
                                    AiStreamChunk chunk = AiStreamChunk.builder()
                                            .id(UUID.randomUUID().toString())
                                            .model(model.getCode())
                                            .delta(text)
                                            .done(false)
                                            .build();
                                    sink.next(chunk);
                                }
                            }
                            break;
                        case "input_audio_buffer.speech_started":
                            log.debug("实时ASR流式检测到语音开始");
                            break;
                        case "input_audio_buffer.speech_stopped":
                            log.debug("实时ASR流式检测到语音结束");
                            break;
                        case "error":
                            String errorMsg = message.has("message")
                                    ? message.get("message").getAsString()
                                    : "实时ASR流式调用失败";
                            log.error("实时ASR流式错误: {}", errorMsg);
                            sink.error(AiProviderException.serverError(getProviderCode(), errorMsg, 500));
                            break;
                        default:
                            log.debug("实时ASR流式收到消息: type={}", type);
                            break;
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    log.debug("实时ASR流式连接已关闭: code={}, reason={}", code, reason);
                    sessionCreatedLatch.countDown();
                    if (!sink.isCancelled()) {
                        // 如果还没有完成，发送完成标记
                        AiStreamChunk doneChunk = AiStreamChunk.builder()
                                .id(UUID.randomUUID().toString())
                                .model(model.getCode())
                                .done(true)
                                .finishReason("stop")
                                .build();
                        sink.next(doneChunk);
                        sink.complete();
                    }
                }
            });
            conversationRef.set(conversation);

            // 在后台线程中执行连接和发送音频
            Thread.startVirtualThread(() -> {
                try {
                    // 连接
                    conversation.connect();

                    // 等待session创建（最多10秒）
                    boolean sessionCreated = sessionCreatedLatch.await(10, TimeUnit.SECONDS);
                    if (!sessionCreated) {
                        sink.error(AiProviderException.networkError(getProviderCode(), "实时ASR流式会话创建超时", null));
                        return;
                    }

                    // 检查是否有错误
                    if (errorRef.get() != null) {
                        Exception error = errorRef.get();
                        if (error instanceof AiProviderException) {
                            sink.error(error);
                        } else {
                            sink.error(AiProviderException.networkError(getProviderCode(), error.getMessage(), error));
                        }
                        return;
                    }

                    // 等待一小段时间确保配置生效
                    Thread.sleep(500);

                    // 分块发送音频数据
                    int offset = 0;
                    while (offset < audioBytes.length && !sink.isCancelled()) {
                        int chunkSize = Math.min(AUDIO_CHUNK_SIZE, audioBytes.length - offset);
                        byte[] chunk = new byte[chunkSize];
                        System.arraycopy(audioBytes, offset, chunk, 0, chunkSize);
                        offset += chunkSize;

                        String chunkBase64 = Base64.getEncoder().encodeToString(chunk);
                        conversation.appendAudio(chunkBase64);

                        // 模拟实时发送
                        Thread.sleep(30);
                    }

                    // 发送静音以触发VAD停止
                    byte[] silence = new byte[1024];
                    for (int i = 0; i < 30 && !sink.isCancelled(); i++) {
                        String silenceBase64 = Base64.getEncoder().encodeToString(silence);
                        conversation.appendAudio(silenceBase64);
                        Thread.sleep(20);
                    }

                } catch (NoApiKeyException e) {
                    sink.error(AiProviderException.authError(getProviderCode(), "API Key无效: " + e.getMessage()));
                } catch (Exception e) {
                    log.error("实时ASR流式调用异常", e);
                    sink.error(AiProviderException.networkError(getProviderCode(), e.getMessage(), e));
                }
            });

            // 设置取消回调
            sink.onCancel(() -> {
                try {
                    conversation.close(1000, "cancelled");
                } catch (Exception e) {
                    log.warn("关闭实时ASR流式连接失败", e);
                }
            });

            sink.onDispose(() -> {
                try {
                    conversation.close(1000, "disposed");
                } catch (Exception e) {
                    log.warn("关闭实时ASR流式连接失败", e);
                }
            });
        });
    }

    /**
     * 使用实时ASR进行语音识别（DashScope SDK）
     * <p>
     * 支持 qwen3-asr-flash-realtime 等实时模型
     * </p>
     */
    private String callRealtimeAsr(AiModel model, String apiKey, AiRequest request)
            throws InterruptedException {
        // 获取音频数据（Base64）
        String audioBase64 = request.getAudio();

        // 如果是URL，需要先下载
        if (audioBase64.startsWith("http://") || audioBase64.startsWith("https://")) {
            try {
                audioBase64 = downloadAudioAsBase64(audioBase64);
            } catch (IOException e) {
                throw AiProviderException.networkError(getProviderCode(), "下载音频文件失败: " + e.getMessage(), e);
            }
        }

        // 解码音频数据
        final byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(audioBase64);
        } catch (IllegalArgumentException e) {
            throw AiProviderException.invalidRequest(getProviderCode(), "无效的Base64音频数据");
        }

        // 用于同步和收集结果
        CountDownLatch sessionCreatedLatch = new CountDownLatch(1); // 等待session创建
        CountDownLatch completeLatch = new CountDownLatch(1); // 等待识别完成
        AtomicReference<String> transcriptRef = new AtomicReference<>("");
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        StringBuilder transcriptBuilder = new StringBuilder();

        // 配置转写参数
        final String audioFormat = StrUtil.isNotBlank(request.getAudioFormat()) ? request.getAudioFormat() : "pcm";
        final int sampleRate = 16000; // 默认采样率

        // 构建实时ASR参数
        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(model.getCode())
                .url(QWEN_ASR_REALTIME_URL)
                .apikey(apiKey)
                .build();

        // 创建实时ASR会话
        AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>();
        OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            @Override
            public void onOpen() {
                log.debug("实时ASR连接已建立");
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch (type) {
                    case "session.created":
                        log.debug("实时ASR会话已创建");
                        // 配置会话并开始发送音频
                        try {
                            OmniRealtimeConversation conv = conversationRef.get();
                            if (conv != null) {
                                OmniRealtimeTranscriptionParam transcriptionParam = new OmniRealtimeTranscriptionParam();
                                transcriptionParam.setLanguage("zh");
                                transcriptionParam.setInputAudioFormat(audioFormat);
                                transcriptionParam.setInputSampleRate(sampleRate);

                                OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                                        .modalities(Collections.singletonList(OmniRealtimeModality.TEXT))
                                        .transcriptionConfig(transcriptionParam)
                                        .build();
                                conv.updateSession(config);
                            }
                        } catch (Exception e) {
                            log.error("配置ASR会话失败", e);
                            errorRef.set(e);
                        }
                        sessionCreatedLatch.countDown();
                        break;
                    case "session.updated":
                        log.debug("实时ASR会话配置已更新");
                        break;
                    case "conversation.item.input_audio_transcription.completed":
                        // 最终识别结果
                        if (message.has("transcript")) {
                            String transcript = message.get("transcript").getAsString();
                            transcriptRef.set(transcript);
                            log.debug("实时ASR识别完成: {}", transcript);
                        }
                        completeLatch.countDown();
                        break;
                    case "conversation.item.input_audio_transcription.text":
                        // 中间识别结果（流式）
                        if (message.has("text")) {
                            String text = message.get("text").getAsString();
                            transcriptBuilder.append(text);
                            log.debug("实时ASR中间结果: {}", text);
                        }
                        break;
                    case "input_audio_buffer.speech_started":
                        log.debug("检测到语音开始");
                        break;
                    case "input_audio_buffer.speech_stopped":
                        log.debug("检测到语音结束");
                        break;
                    case "error":
                        String errorMsg = message.has("message")
                                ? message.get("message").getAsString()
                                : "实时ASR调用失败";
                        log.error("实时ASR错误: {}", errorMsg);
                        errorRef.set(AiProviderException.serverError(getProviderCode(), errorMsg, 500));
                        sessionCreatedLatch.countDown();
                        completeLatch.countDown();
                        break;
                    default:
                        log.debug("实时ASR收到消息: type={}", type);
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.debug("实时ASR连接已关闭: code={}, reason={}", code, reason);
                sessionCreatedLatch.countDown();
                completeLatch.countDown();
            }
        });
        conversationRef.set(conversation);

        try {
            // 连接
            conversation.connect();

            // 等待session创建（最多10秒）
            boolean sessionCreated = sessionCreatedLatch.await(10, TimeUnit.SECONDS);
            if (!sessionCreated) {
                throw AiProviderException.networkError(getProviderCode(), "实时ASR会话创建超时", null);
            }

            // 检查是否有错误
            if (errorRef.get() != null) {
                Exception error = errorRef.get();
                if (error instanceof AiProviderException) {
                    throw (AiProviderException) error;
                }
                throw AiProviderException.networkError(getProviderCode(), error.getMessage(), error);
            }

            // 等待一小段时间确保配置生效
            Thread.sleep(500);

            // 分块发送音频数据
            int offset = 0;
            while (offset < audioBytes.length) {
                int chunkSize = Math.min(AUDIO_CHUNK_SIZE, audioBytes.length - offset);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(audioBytes, offset, chunk, 0, chunkSize);
                offset += chunkSize;

                String chunkBase64 = Base64.getEncoder().encodeToString(chunk);
                conversation.appendAudio(chunkBase64);

                // 模拟实时发送
                Thread.sleep(30);
            }

            // 发送静音以触发VAD停止
            byte[] silence = new byte[1024];
            for (int i = 0; i < 30; i++) {
                String silenceBase64 = Base64.getEncoder().encodeToString(silence);
                conversation.appendAudio(silenceBase64);
                Thread.sleep(20);
            }

            // 等待完成（最多60秒）
            boolean completed = completeLatch.await(60, TimeUnit.SECONDS);

            if (!completed) {
                throw AiProviderException.networkError(getProviderCode(), "实时ASR请求超时", null);
            }

            if (errorRef.get() != null) {
                Exception error = errorRef.get();
                if (error instanceof AiProviderException) {
                    throw (AiProviderException) error;
                }
                throw AiProviderException.networkError(getProviderCode(), error.getMessage(), error);
            }

            // 优先返回最终结果，如果没有则返回中间结果
            String result = transcriptRef.get();
            if (StrUtil.isBlank(result)) {
                result = transcriptBuilder.toString();
            }

            return result;

        } catch (NoApiKeyException e) {
            throw AiProviderException.authError(getProviderCode(), "API Key无效: " + e.getMessage());
        } finally {
            try {
                conversation.close(1000, "completed");
            } catch (Exception e) {
                log.warn("关闭实时ASR连接失败", e);
            }
        }
    }

    /**
     * 下载音频文件并转换为Base64
     */
    private String downloadAudioAsBase64(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载音频失败: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("音频响应体为空");
            }

            byte[] audioBytes = body.bytes();
            return Base64.getEncoder().encodeToString(audioBytes);
        }
    }

    /**
     * 上传音频文件到DashScope OSS获取临时URL
     * <p>
     * 使用DashScope两步上传流程：
     * 1. 获取上传凭证（policy, signature等）
     * 2. 上传文件到OSS
     * 3. 返回oss://前缀的临时URL
     * </p>
     *
     * @param apiKey      API Key
     * @param audioBase64 Base64编码的音频数据
     * @param format      音频格式（mp3, wav, flac等）
     * @return 上传后的文件URL（oss://前缀）
     */
    private String uploadAudioToDashScope(String apiKey, String audioBase64, String format) throws IOException {
        // Step 1: 获取上传凭证
        String policyUrl = DASHSCOPE_UPLOAD_URL + "?action=getPolicy&model=paraformer-v2";
        Request policyRequest = new Request.Builder()
                .url(policyUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .get()
                .build();

        String uploadHost;
        String uploadDir;
        String policy;
        String signature;
        String ossAccessKeyId;
        String xOssObjectAcl;
        String xOssForbidOverwrite;

        try (Response policyResponse = httpClient.newCall(policyRequest).execute()) {
            if (!policyResponse.isSuccessful()) {
                ResponseBody errorBody = policyResponse.body();
                String errorMsg = errorBody != null ? errorBody.string() : "获取上传凭证失败";
                log.error("DashScope获取上传凭证失败: code={}, body={}", policyResponse.code(), errorMsg);
                throw AiProviderException.serverError(getProviderCode(),
                        "获取上传凭证失败: " + errorMsg, policyResponse.code());
            }

            ResponseBody responseBody = policyResponse.body();
            if (responseBody == null) {
                throw AiProviderException.serverError(getProviderCode(), "上传凭证响应体为空", policyResponse.code());
            }

            String responseStr = responseBody.string();
            log.debug("DashScope上传凭证响应: {}", responseStr);

            JSONObject responseJson = JSONUtil.parseObj(responseStr);
            JSONObject data = responseJson.getJSONObject("data");
            if (data == null) {
                throw AiProviderException.serverError(getProviderCode(), "上传凭证响应data为空", 200);
            }

            uploadHost = data.getStr("upload_host");
            uploadDir = data.getStr("upload_dir");
            policy = data.getStr("policy");
            signature = data.getStr("signature");
            ossAccessKeyId = data.getStr("oss_access_key_id");
            xOssObjectAcl = data.getStr("x_oss_object_acl", "private");
            xOssForbidOverwrite = data.getStr("x_oss_forbid_overwrite", "true");
        }

        // Step 2: 上传文件到OSS
        byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
        String fileName = "audio_" + System.currentTimeMillis() + "." + format;
        String ossKey = uploadDir + "/" + fileName;

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("OSSAccessKeyId", ossAccessKeyId)
                .addFormDataPart("policy", policy)
                .addFormDataPart("signature", signature)
                .addFormDataPart("key", ossKey)
                .addFormDataPart("x-oss-object-acl", xOssObjectAcl)
                .addFormDataPart("x-oss-forbid-overwrite", xOssForbidOverwrite)
                .addFormDataPart("success_action_status", "200")
                .addFormDataPart("file", fileName,
                        RequestBody.create(audioBytes, MediaType.parse("audio/" + format)))
                .build();

        Request uploadRequest = new Request.Builder()
                .url(uploadHost)
                .post(multipartBody)
                .build();

        try (Response uploadResponse = httpClient.newCall(uploadRequest).execute()) {
            if (!uploadResponse.isSuccessful()) {
                ResponseBody errorBody = uploadResponse.body();
                String errorMsg = errorBody != null ? errorBody.string() : "上传失败";
                log.error("OSS文件上传失败: code={}, body={}", uploadResponse.code(), errorMsg);
                throw AiProviderException.serverError(getProviderCode(),
                        "音频文件上传失败: " + errorMsg, uploadResponse.code());
            }

            // 返回oss://前缀的URL
            String ossUrl = "oss://" + ossKey;
            log.debug("音频文件上传成功: {}", ossUrl);
            return ossUrl;
        }
    }

    /**
     * DashScope 文件上传凭证API URL
     */
    private static final String DASHSCOPE_UPLOAD_URL = "https://dashscope.aliyuncs.com/api/v1/uploads";

    /**
     * 提交ASR转写任务（文件转写API）
     */
    private String submitAsrTask(AiModel model, String apiKey, AiRequest request) throws IOException {
        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getCode());

        // input 包含文件URL列表
        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> fileUrls = new ArrayList<>();

        String audioData = request.getAudio();
        boolean useOssUrl = false; // 标记是否使用了OSS URL

        // 判断是URL还是Base64
        if (audioData.startsWith("http://") || audioData.startsWith("https://")) {
            // 直接使用URL
            Map<String, String> fileUrl = new HashMap<>();
            fileUrl.put("file_url", audioData);
            fileUrls.add(fileUrl);
        } else {
            // Base64数据需要先上传到DashScope获取URL
            String format = StrUtil.isNotBlank(request.getAudioFormat()) ? request.getAudioFormat() : "mp3";
            String uploadedUrl = uploadAudioToDashScope(apiKey, audioData, format);
            Map<String, String> fileUrl = new HashMap<>();
            fileUrl.put("file_url", uploadedUrl);
            fileUrls.add(fileUrl);
            useOssUrl = true; // 使用了OSS URL
        }
        input.put("file_urls", fileUrls);
        body.put(INPUT_FIELD, input);

        // 参数配置
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language_hints", Arrays.asList("zh", "en")); // 支持中英文
        body.put(PARAMETERS_FIELD, parameters);

        String json = JSONUtil.toJsonStr(body);
        log.debug("通义千问ASR请求: {}", json);

        RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder()
                .url(QWEN_ASR_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable") // 异步模式
                .post(requestBody);

        // 如果使用OSS URL，需要添加资源解析header
        if (useOssUrl) {
            requestBuilder.header("X-DashScope-OssResourceResolve", "enable");
        }

        Request httpRequest = requestBuilder.build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw handleError(response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw AiProviderException.serverError(getProviderCode(), "ASR响应体为空", response.code());
            }

            String responseStr = responseBody.string();
            log.debug("通义千问ASR提交响应: {}", responseStr);

            JSONObject responseJson = JSONUtil.parseObj(responseStr);
            JSONObject output = responseJson.getJSONObject(OUTPUT_FIELD);
            if (output == null) {
                throw AiProviderException.serverError(getProviderCode(), "ASR响应output为空", 200);
            }

            String taskId = output.getStr("task_id");
            if (StrUtil.isBlank(taskId)) {
                throw AiProviderException.serverError(getProviderCode(), "ASR未返回task_id", 200);
            }

            return taskId;
        }
    }

    /**
     * 轮询ASR任务结果
     */
    private String pollAsrResult(String apiKey, String taskId) throws IOException, InterruptedException {
        String queryUrl = QWEN_ASR_API_URL + "/" + taskId;
        int maxRetries = 60; // 最多等待60秒
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Request httpRequest = new Request.Builder()
                    .url(queryUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw handleError(response);
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw AiProviderException.serverError(getProviderCode(), "ASR查询响应体为空", response.code());
                }

                String responseStr = responseBody.string();
                log.debug("通义千问ASR查询响应: {}", responseStr);

                JSONObject responseJson = JSONUtil.parseObj(responseStr);
                JSONObject output = responseJson.getJSONObject(OUTPUT_FIELD);
                if (output == null) {
                    throw AiProviderException.serverError(getProviderCode(), "ASR查询响应output为空", 200);
                }

                String taskStatus = output.getStr("task_status");

                if ("SUCCEEDED".equals(taskStatus)) {
                    // 任务成功，解析结果
                    return extractAsrResult(output);
                } else if ("FAILED".equals(taskStatus)) {
                    String errorMsg = output.getStr("message", "ASR任务失败");
                    throw AiProviderException.serverError(getProviderCode(), errorMsg, 200);
                } else if ("RUNNING".equals(taskStatus) || "PENDING".equals(taskStatus)) {
                    // 任务还在进行中，等待后重试
                    Thread.sleep(1000);
                    retryCount++;
                } else {
                    // 未知状态
                    log.warn("ASR任务状态未知: {}", taskStatus);
                    Thread.sleep(1000);
                    retryCount++;
                }
            }
        }

        throw AiProviderException.networkError(getProviderCode(), "ASR任务超时", null);
    }

    /**
     * 提取ASR识别结果
     */
    private String extractAsrResult(JSONObject output) {
        // 解析转写结果
        // 结果格式: output.results[0].transcription_url 或 output.results[0].text
        JSONArray results = output.getJSONArray("results");
        if (CollUtil.isEmpty(results)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.getJSONObject(i);
            if (result != null) {
                // 尝试获取直接文本
                String text = result.getStr("text");
                if (StrUtil.isNotBlank(text)) {
                    sb.append(text);
                } else {
                    // 尝试从 transcription_url 获取
                    String transcriptionUrl = result.getStr("transcription_url");
                    if (StrUtil.isNotBlank(transcriptionUrl)) {
                        try {
                            String transcriptionText = fetchTranscriptionFromUrl(transcriptionUrl);
                            sb.append(transcriptionText);
                        } catch (Exception e) {
                            log.warn("获取转写结果失败: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 从URL获取转写文本
     */
    private String fetchTranscriptionFromUrl(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取转写结果失败: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                return "";
            }

            String content = body.string();
            // 解析JSON格式的转写结果
            JSONObject json = JSONUtil.parseObj(content);
            JSONArray transcripts = json.getJSONArray("transcripts");
            if (CollUtil.isEmpty(transcripts)) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < transcripts.size(); i++) {
                JSONObject transcript = transcripts.getJSONObject(i);
                if (transcript != null) {
                    String text = transcript.getStr("text");
                    if (StrUtil.isNotBlank(text)) {
                        sb.append(text);
                    }
                }
            }
            return sb.toString();
        }
    }

    // ========== 语音合成（TTS）- 使用DashScope SDK ==========

    /**
     * 语音合成（TTS）
     * <p>
     * 使用通义千问qwen3-tts-realtime系列模型进行实时语音合成（DashScope SDK）
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用input字段作为文本）
     * @return 响应结果的Mono（包含audioBase64字段，PCM格式）
     */
    @Override
    public Mono<AiResponse> textToSpeech(AiModel model, AiRequest request) {
        return Mono.fromCallable(() -> {
            log.debug("通义千问TTS(SDK)调用开始: model={}", model.getCode());

            // 选择API Key
            ModelApiKey selectedKey = selectApiKey(model);

            if (StrUtil.isBlank(selectedKey.getApiKey())) {
                throw AiProviderException.authError(getProviderCode(), "API Key not configured");
            }

            // 使用DashScope SDK进行TTS
            try {
                String audioBase64 = callTtsWithSdk(model, selectedKey.getApiKey(), request);

                // 记录成功
                recordKeySuccess(selectedKey.getId(), null);

                return AiResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .audioBase64(audioBase64)
                        .model(model.getCode())
                        .build();
            } catch (AiProviderException e) {
                recordKeyFailure(selectedKey.getId());
                throw e;
            } catch (Exception e) {
                log.error("通义千问TTS调用失败", e);
                recordKeyFailure(selectedKey.getId());
                throw AiProviderException.networkError(getProviderCode(), e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 使用DashScope SDK调用TTS
     */
    private String callTtsWithSdk(AiModel model, String apiKey, AiRequest request)
            throws InterruptedException {
        // 音色配置
        String voice = StrUtil.isNotBlank(request.getVoice()) ? request.getVoice() : "Chelsie";
        String textToSynthesize = request.getInput();

        // 用于收集音频数据和同步
        CountDownLatch completeLatch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        List<String> audioChunks = new CopyOnWriteArrayList<>();

        // 构建TTS参数
        QwenTtsRealtimeParam param = QwenTtsRealtimeParam.builder()
                .model(model.getCode())
                .apikey(apiKey)
                .build();

        // 创建TTS实例
        QwenTtsRealtime qwenTtsRealtime = new QwenTtsRealtime(param, new QwenTtsRealtimeCallback() {
            @Override
            public void onOpen() {
                log.debug("TTS连接已建立");
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch (type) {
                    case "session.created":
                        log.debug("TTS会话已创建");
                        break;
                    case "response.audio.delta":
                        // 收集音频数据
                        if (message.has("delta")) {
                            String delta = message.get("delta").getAsString();
                            if (StrUtil.isNotBlank(delta)) {
                                audioChunks.add(delta);
                            }
                        }
                        break;
                    case "response.done":
                        log.debug("TTS响应完成");
                        break;
                    case "session.finished":
                        log.debug("TTS会话结束");
                        completeLatch.countDown();
                        break;
                    case "error":
                        String errorMsg = message.has("message")
                                ? message.get("message").getAsString()
                                : "TTS调用失败";
                        log.error("TTS错误: {}", errorMsg);
                        errorRef.set(AiProviderException.serverError(getProviderCode(), errorMsg, 500));
                        completeLatch.countDown();
                        break;
                    default:
                        log.debug("TTS收到消息: type={}", type);
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.debug("TTS连接已关闭: code={}, reason={}", code, reason);
                completeLatch.countDown();
            }
        });

        try {
            // 连接
            qwenTtsRealtime.connect();

            // 配置音频参数（使用mp3格式）
            QwenTtsRealtimeConfig config = QwenTtsRealtimeConfig.builder()
                    .voice(voice)
                    .format("mp3")
                    .mode("server_commit")
                    .build();
            qwenTtsRealtime.updateSession(config);

            // 发送文本
            qwenTtsRealtime.appendText(textToSynthesize);

            // 发送完成信号
            qwenTtsRealtime.finish();

            // 等待完成（最多60秒）
            boolean completed = completeLatch.await(60, TimeUnit.SECONDS);

            if (!completed) {
                throw AiProviderException.networkError(getProviderCode(), "TTS请求超时", null);
            }

            if (errorRef.get() != null) {
                Exception error = errorRef.get();
                if (error instanceof AiProviderException) {
                    throw (AiProviderException) error;
                }
                throw AiProviderException.networkError(getProviderCode(), error.getMessage(), error);
            }

            // 合并所有音频片段
            if (audioChunks.isEmpty()) {
                throw AiProviderException.serverError(getProviderCode(), "TTS未返回音频数据", 200);
            }

            return mergeAudioChunks(audioChunks);

        } catch (NoApiKeyException e) {
            throw AiProviderException.authError(getProviderCode(), "API Key无效: " + e.getMessage());
        }
    }

    /**
     * 合并音频片段（Base64编码的音频数据）
     */
    private String mergeAudioChunks(List<String> chunks) {
        // 解码所有Base64片段
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (String chunk : chunks) {
            byte[] decoded = Base64.getDecoder().decode(chunk);
            outputStream.write(decoded, 0, decoded.length);
        }

        // 重新编码为Base64返回
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    // ========== 流式语音识别（ASR）- 使用 MultiModalConversation ==========

    /**
     * 流式语音识别（ASR）
     * <p>
     * 根据模型类型选择不同的流式实现：
     * <ul>
     *   <li>qwen3-asr-flash-realtime（实时模型 + PCM格式）：使用 WebSocket 实时流式识别</li>
     *   <li>qwen3-asr-flash 等其他模型：使用 MultiModalConversation API 流式识别</li>
     * </ul>
     * </p>
     *
     * @param model   AI模型聚合根
     * @param request 统一请求参数（使用audio字段，支持Base64或URL）
     * @return 流式响应的Flux，每个元素是一个识别文本块
     */
    @Override
    public Flux<AiStreamChunk> speechToTextStream(AiModel model, AiRequest request) {
        // 在Flux创建前选择Key
        ModelApiKey selectedKey = selectApiKey(model);

        // 验证API Key
        if (StrUtil.isBlank(selectedKey.getApiKey())) {
            return Flux.error(AiProviderException.authError(getProviderCode(), "API Key not configured"));
        }

        // 验证音频输入
        if (StrUtil.isBlank(request.getAudio())) {
            return Flux.error(AiProviderException.invalidRequest(getProviderCode(), "音频数据不能为空"));
        }

        // 根据模型和音频格式选择流式实现
        String audioFormat = request.getAudioFormat();
        if (shouldUseRealtimeAsr(model.getCode(), audioFormat)) {
            // 使用实时ASR WebSocket流式实现
            log.debug("使用实时ASR WebSocket流式实现: model={}", model.getCode());
            return callRealtimeAsrStream(model, selectedKey.getApiKey(), request)
                    .doOnComplete(() -> recordKeySuccess(selectedKey.getId(), null))
                    .doOnError(e -> recordKeyFailure(selectedKey.getId()));
        }

        // 使用 MultiModalConversation API 流式实现
        return callMultiModalAsrStream(model, selectedKey, request);
    }

    /**
     * 使用 MultiModalConversation API 进行流式语音识别
     * <p>
     * 适用于 qwen3-asr-flash 等非实时模型
     * </p>
     */
    private Flux<AiStreamChunk> callMultiModalAsrStream(AiModel model, ModelApiKey selectedKey, AiRequest request) {
        return Flux.create(sink -> {
            log.debug("通义千问MultiModal流式ASR调用开始: model={}", model.getCode());

            try {
                // 获取音频URL
                String audioUrl = getAudioUrl(request, selectedKey.getApiKey());

                // 构建多模态消息
                MultiModalMessage userMessage = MultiModalMessage.builder()
                        .role(Role.USER.getValue())
                        .content(Collections.singletonList(
                                Collections.singletonMap("audio", audioUrl)))
                        .build();

                // 系统消息（用于配置定制化识别的Context）
                MultiModalMessage sysMessage = MultiModalMessage.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(Collections.singletonList(Collections.singletonMap("text", "")))
                        .build();

                // ASR选项
                Map<String, Object> asrOptions = new HashMap<>();
                asrOptions.put("enable_itn", true); // 启用逆文本正则化

                // 构建参数
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(selectedKey.getApiKey())
                        .model(model.getCode())
                        .message(sysMessage)
                        .message(userMessage)
                        .parameter("asr_options", asrOptions)
                        .build();

                // 创建MultiModalConversation实例
                MultiModalConversation conv = new MultiModalConversation();

                // 流式调用
                io.reactivex.Flowable<MultiModalConversationResult> resultFlowable = conv.streamCall(param);

                // 订阅流式结果
                resultFlowable.subscribe(
                        item -> {
                            try {
                                // 提取识别文本
                                String text = extractAsrText(item);
                                if (StrUtil.isNotBlank(text)) {
                                    AiStreamChunk chunk = AiStreamChunk.builder()
                                            .id(UUID.randomUUID().toString())
                                            .model(model.getCode())
                                            .delta(text)
                                            .done(false)
                                            .build();
                                    sink.next(chunk);
                                }
                            } catch (Exception e) {
                                log.warn("解析ASR流式数据失败", e);
                            }
                        },
                        error -> {
                            log.error("通义千问MultiModal流式ASR调用失败", error);
                            recordKeyFailure(selectedKey.getId());
                            if (error instanceof ApiException) {
                                sink.error(AiProviderException.serverError(getProviderCode(),
                                        error.getMessage(), 500));
                            } else {
                                sink.error(AiProviderException.networkError(getProviderCode(),
                                        error.getMessage(), (Exception) error));
                            }
                        },
                        () -> {
                            // 完成
                            log.debug("通义千问MultiModal流式ASR调用完成");
                            recordKeySuccess(selectedKey.getId(), null);

                            // 发送完成标记
                            AiStreamChunk doneChunk = AiStreamChunk.builder()
                                    .id(UUID.randomUUID().toString())
                                    .model(model.getCode())
                                    .done(true)
                                    .finishReason("stop")
                                    .build();
                            sink.next(doneChunk);
                            sink.complete();
                        }
                );

            } catch (Exception e) {
                log.error("通义千问MultiModal流式ASR调用异常", e);
                recordKeyFailure(selectedKey.getId());
                sink.error(AiProviderException.networkError(getProviderCode(), e.getMessage(), e));
            }
        });
    }

    /**
     * 获取音频URL
     * <p>
     * 如果是Base64数据，先上传到DashScope获取URL
     * </p>
     */
    private String getAudioUrl(AiRequest request, String apiKey) throws IOException {
        String audioData = request.getAudio();

        // 如果已经是URL，直接返回
        if (audioData.startsWith("http://") || audioData.startsWith("https://")) {
            return audioData;
        }

        // Base64数据需要先上传
        String format = StrUtil.isNotBlank(request.getAudioFormat()) ? request.getAudioFormat() : "mp3";
        String ossUrl = uploadAudioToDashScope(apiKey, audioData, format);

        // 将 oss:// 转换为 https:// URL
        // DashScope OSS URL格式: oss://dashscope-result-bj/xxx
        // 需要转换为: https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/xxx
        if (ossUrl.startsWith("oss://")) {
            String path = ossUrl.substring(6); // 去掉 "oss://"
            int slashIndex = path.indexOf('/');
            if (slashIndex > 0) {
                String bucket = path.substring(0, slashIndex);
                String key = path.substring(slashIndex + 1);
                return String.format("https://%s.oss-cn-beijing.aliyuncs.com/%s", bucket, key);
            }
        }

        return ossUrl;
    }

    /**
     * 从MultiModalConversationResult中提取ASR文本
     */
    private String extractAsrText(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null) {
            return null;
        }

        var choices = result.getOutput().getChoices();
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        var message = choices.get(0).getMessage();
        if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
            return null;
        }

        // content是一个List<Map<String, Object>>，提取text字段
        var content = message.getContent().get(0);
        if (content instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contentMap = (Map<String, Object>) content;
            Object text = contentMap.get("text");
            return text != null ? text.toString() : null;
        }

        return null;
    }
}
