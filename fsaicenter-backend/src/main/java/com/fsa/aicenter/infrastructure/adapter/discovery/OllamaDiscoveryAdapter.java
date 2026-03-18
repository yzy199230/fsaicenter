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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Ollama模型发现适配器
 * <p>
 * Ollama使用 GET /api/tags 获取本地已安装的模型列表。
 * 同时支持 openai_compatible 和 custom_http 协议类型中的Ollama实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaDiscoveryAdapter implements ModelDiscoveryAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public List<String> getSupportedProtocols() {
        // Ollama同时支持自己的API和OpenAI兼容API
        return List.of(Provider.PROTOCOL_CUSTOM_HTTP);
    }

    @Override
    public boolean supportsDiscovery(Provider provider) {
        // 检查是否是Ollama提供商
        String code = provider.getCode();
        if (code != null && code.toLowerCase().contains("ollama")) {
            return true;
        }

        // 检查baseUrl是否包含ollama相关路径
        String baseUrl = provider.getBaseUrl();
        if (baseUrl != null && baseUrl.toLowerCase().contains("ollama")) {
            return true;
        }

        return false;
    }

    @Override
    public List<DiscoveredModel> discoverModels(Provider provider, String apiKey) {
        List<DiscoveredModel> models = new ArrayList<>();

        String baseUrl = provider.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("Provider {} 没有配置baseUrl", provider.getCode());
            return models;
        }

        // 构建Ollama模型列表API URL
        // Ollama API: GET /api/tags
        String ollamaBaseUrl = baseUrl;
        if (ollamaBaseUrl.contains("/v1")) {
            // 如果配置的是OpenAI兼容端点，需要转换为Ollama原生端点
            ollamaBaseUrl = ollamaBaseUrl.replace("/v1", "");
        }

        String tagsUrl = ollamaBaseUrl.endsWith("/")
                ? ollamaBaseUrl + "api/tags"
                : ollamaBaseUrl + "/api/tags";

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(tagsUrl)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("调用Ollama模型列表API失败: {} - {}", response.code(), response.message());
                    return models;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                models = parseOllamaResponse(responseBody);
                log.info("从Ollama发现了 {} 个模型", models.size());
            }
        } catch (Exception e) {
            log.error("发现Ollama模型时发生错误: {}", e.getMessage(), e);
        }

        return models;
    }

    /**
     * 解析Ollama模型列表响应
     */
    private List<DiscoveredModel> parseOllamaResponse(String responseBody) {
        List<DiscoveredModel> models = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode modelsNode = root.get("models");

            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode modelNode : modelsNode) {
                    String name = getTextValue(modelNode, "name");
                    if (name == null || name.isBlank()) {
                        continue;
                    }

                    // 解析修改时间
                    Long modifiedAt = null;
                    if (modelNode.has("modified_at")) {
                        try {
                            String modifiedAtStr = modelNode.get("modified_at").asText();
                            modifiedAt = Instant.parse(modifiedAtStr).getEpochSecond();
                        } catch (Exception ignored) {
                        }
                    }

                    // 收集额外信息
                    Map<String, Object> extra = new HashMap<>();
                    if (modelNode.has("size")) {
                        extra.put("size", modelNode.get("size").asLong());
                    }
                    if (modelNode.has("digest")) {
                        extra.put("digest", modelNode.get("digest").asText());
                    }
                    if (modelNode.has("details")) {
                        JsonNode details = modelNode.get("details");
                        if (details.has("family")) {
                            extra.put("family", details.get("family").asText());
                        }
                        if (details.has("parameter_size")) {
                            extra.put("parameterSize", details.get("parameter_size").asText());
                        }
                        if (details.has("quantization_level")) {
                            extra.put("quantization", details.get("quantization_level").asText());
                        }
                    }

                    // 推断模型类型
                    String inferredType = inferOllamaModelType(name);

                    models.add(new DiscoveredModel(
                            name,
                            formatModelName(name),
                            "local",
                            modifiedAt,
                            inferredType,
                            extra.isEmpty() ? null : extra
                    ));
                }
            }
        } catch (Exception e) {
            log.error("解析Ollama模型列表响应失败: {}", e.getMessage());
        }

        return models;
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    /**
     * 格式化模型名称
     * 例如: llama3.3:70b -> Llama 3.3 70B
     */
    private String formatModelName(String modelId) {
        if (modelId == null) {
            return null;
        }

        // 移除标签部分的冒号前后处理
        String[] parts = modelId.split(":");
        String baseName = parts[0];
        String tag = parts.length > 1 ? parts[1].toUpperCase() : "";

        // 首字母大写
        String formatted = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);

        // 添加版本号之间的空格
        formatted = formatted.replaceAll("(\\d+\\.\\d+)", " $1 ");
        formatted = formatted.replaceAll("\\s+", " ").trim();

        if (!tag.isEmpty()) {
            formatted += " " + tag;
        }

        return formatted;
    }

    /**
     * 推断Ollama模型类型
     */
    private String inferOllamaModelType(String modelId) {
        String lower = modelId.toLowerCase();

        if (lower.contains("embed") || lower.contains("nomic-embed") || lower.contains("mxbai-embed")) {
            return "embedding";
        }
        if (lower.contains("vision") || lower.contains("-vl") || lower.contains("llava")) {
            return "image_recognition";
        }

        // Ollama主要是chat模型
        return "chat";
    }
}
