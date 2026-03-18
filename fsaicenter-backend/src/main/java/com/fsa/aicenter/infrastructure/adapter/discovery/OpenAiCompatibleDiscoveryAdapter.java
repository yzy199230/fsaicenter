package com.fsa.aicenter.infrastructure.adapter.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.infrastructure.adapter.common.ModelDiscoveryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI兼容协议的模型发现适配器
 * <p>
 * 支持 openai_compatible 和 openai_like 协议类型。
 * 调用 GET /models 端点获取模型列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleDiscoveryAdapter implements ModelDiscoveryAdapter {

    private final ObjectMapper objectMapper;

    private static final List<String> SUPPORTED_PROTOCOLS = List.of(
            Provider.PROTOCOL_OPENAI_COMPATIBLE,
            Provider.PROTOCOL_OPENAI_LIKE
    );

    @Override
    public List<String> getSupportedProtocols() {
        return SUPPORTED_PROTOCOLS;
    }

    @Override
    public List<DiscoveredModel> discoverModels(Provider provider, String apiKey) {
        List<DiscoveredModel> models = new ArrayList<>();

        String baseUrl = provider.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("Provider {} 没有配置baseUrl", provider.getCode());
            return models;
        }

        // 构建模型列表API URL
        String modelsUrl = baseUrl.endsWith("/")
                ? baseUrl + "models"
                : baseUrl + "/models";

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request.Builder requestBuilder = new Request.Builder()
                    .url(modelsUrl)
                    .get();

            // 添加认证头
            if (apiKey != null && !apiKey.isBlank()) {
                String authHeader = provider.getAuthHeader() != null
                        ? provider.getAuthHeader()
                        : "Authorization";
                String authPrefix = provider.getAuthPrefixOrDefault();
                requestBuilder.header(authHeader, authPrefix + apiKey);
            }

            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("调用模型列表API失败: {} - {}", response.code(), response.message());
                    return models;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                models = parseModelsResponse(responseBody);
                log.info("从 {} 发现了 {} 个模型", provider.getCode(), models.size());
            }
        } catch (Exception e) {
            log.error("发现模型时发生错误: {}", e.getMessage(), e);
        }

        return models;
    }

    /**
     * 解析OpenAI格式的模型列表响应
     */
    private List<DiscoveredModel> parseModelsResponse(String responseBody) {
        List<DiscoveredModel> models = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.get("data");

            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    String id = getTextValue(modelNode, "id");
                    if (id == null || id.isBlank()) {
                        continue;
                    }

                    String name = getTextValue(modelNode, "name");
                    if (name == null) {
                        name = id;
                    }

                    String ownedBy = getTextValue(modelNode, "owned_by");
                    Long createdAt = modelNode.has("created")
                            ? modelNode.get("created").asLong()
                            : null;

                    // 推断模型类型
                    String inferredType = inferModelType(id);

                    models.add(new DiscoveredModel(id, name, ownedBy, createdAt, inferredType, null));
                }
            }
        } catch (Exception e) {
            log.error("解析模型列表响应失败: {}", e.getMessage());
        }

        return models;
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    /**
     * 根据模型ID推断模型类型
     */
    private String inferModelType(String modelId) {
        String lower = modelId.toLowerCase();

        if (lower.contains("embed") || lower.contains("embedding")) {
            return "embedding";
        }
        if (lower.contains("whisper") || lower.contains("asr") || lower.contains("speech-to-text")) {
            return "asr";
        }
        if (lower.contains("tts") || lower.contains("text-to-speech")) {
            return "tts";
        }
        if (lower.contains("dall-e") || lower.contains("image") || lower.contains("wanx")
                || lower.contains("cogview") || lower.contains("jimeng") || lower.contains("seedream")) {
            return "image";
        }
        if (lower.contains("video") || lower.contains("sora")) {
            return "video";
        }
        if (lower.contains("vision") || lower.contains("-vl") || lower.contains("4v")) {
            return "image_recognition";
        }

        // 默认为chat类型
        return "chat";
    }
}
