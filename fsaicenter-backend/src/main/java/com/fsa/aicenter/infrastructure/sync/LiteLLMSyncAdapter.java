package com.fsa.aicenter.infrastructure.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LiteLLM 模型库同步适配器
 * 从 LiteLLM GitHub 仓库同步模型元数据
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteLLMSyncAdapter implements TemplateSyncAdapter {

    private final ObjectMapper objectMapper;

    private static final String SOURCE_NAME = "litellm";
    private static final String DISPLAY_NAME = "LiteLLM 模型库";

    /** LiteLLM 模型数据 GitHub Raw URL */
    private static final String LITELLM_MODEL_DATA_URL =
            "https://raw.githubusercontent.com/BerriAI/litellm/main/model_prices_and_context_window.json";

    /** 备用镜像地址 */
    private static final String LITELLM_MODEL_DATA_URL_MIRROR =
            "https://cdn.jsdelivr.net/gh/BerriAI/litellm@main/model_prices_and_context_window.json";

    /** LiteLLM mode 到本项目 ModelType 的映射 */
    private static final Map<String, ModelType> MODE_TYPE_MAPPING = Map.of(
            "chat", ModelType.CHAT,
            "completion", ModelType.CHAT,
            "embedding", ModelType.EMBEDDING,
            "image_generation", ModelType.IMAGE,
            "audio_transcription", ModelType.ASR,
            "audio_speech", ModelType.TTS,
            "rerank", ModelType.EMBEDDING  // rerank 归类为 embedding
    );

    /** LiteLLM provider 到本项目 providerCode 的映射 */
    private static final Map<String, String> PROVIDER_MAPPING = Map.ofEntries(
            Map.entry("openai", "openai"),
            Map.entry("azure", "azure"),
            Map.entry("azure_ai", "azure"),
            Map.entry("anthropic", "anthropic"),
            Map.entry("bedrock", "bedrock"),
            Map.entry("vertex_ai", "google"),
            Map.entry("vertex_ai-chat-models", "google"),
            Map.entry("vertex_ai-text-models", "google"),
            Map.entry("vertex_ai-vision-models", "google"),
            Map.entry("vertex_ai-embedding-models", "google"),
            Map.entry("gemini", "google"),
            Map.entry("cohere", "cohere"),
            Map.entry("cohere_chat", "cohere"),
            Map.entry("mistral", "mistral"),
            Map.entry("groq", "groq"),
            Map.entry("together_ai", "together"),
            Map.entry("deepseek", "deepseek"),
            Map.entry("ollama", "ollama"),
            Map.entry("ollama_chat", "ollama"),
            Map.entry("huggingface", "huggingface"),
            Map.entry("replicate", "replicate"),
            Map.entry("perplexity", "perplexity"),
            Map.entry("fireworks_ai", "fireworks"),
            Map.entry("anyscale", "anyscale"),
            Map.entry("cloudflare", "cloudflare"),
            Map.entry("ai21", "ai21"),
            Map.entry("nlp_cloud", "nlp_cloud"),
            Map.entry("aleph_alpha", "aleph_alpha"),
            Map.entry("sagemaker", "sagemaker"),
            Map.entry("palm", "google"),
            Map.entry("text-completion-openai", "openai"),
            Map.entry("text-completion-codestral", "mistral"),
            // 国产厂商映射
            Map.entry("volcengine", "volcengine"),
            Map.entry("doubao", "volcengine"),
            Map.entry("qwen", "qwen"),
            Map.entry("dashscope", "qwen"),
            Map.entry("tongyi", "qwen"),
            Map.entry("aliyun", "qwen"),
            Map.entry("baidu", "wenxin"),
            Map.entry("wenxin", "wenxin"),
            Map.entry("ernie", "wenxin"),
            Map.entry("zhipu", "zhipu"),
            Map.entry("glm", "zhipu"),
            Map.entry("moonshot", "moonshot"),
            Map.entry("kimi", "moonshot"),
            Map.entry("minimax", "minimax"),
            Map.entry("baichuan", "baichuan"),
            Map.entry("yi", "yi"),
            Map.entry("lingyi", "yi")
    );

    /** 需要过滤掉的提供商（非主流或已废弃） */
    private static final Set<String> EXCLUDED_PROVIDERS = Set.of(
            "petals", "baseten", "voyage", "xinference"
    );

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public List<ModelTemplate> fetchTemplates() throws TemplateSyncException {
        log.info("开始从 LiteLLM 同步模型模板...");

        String jsonContent = fetchModelData();
        List<ModelTemplate> templates = parseModelData(jsonContent);

        log.info("从 LiteLLM 获取到 {} 个模型模板", templates.size());
        return templates;
    }

    @Override
    public boolean isAvailable() {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(LITELLM_MODEL_DATA_URL)
                    .head()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("LiteLLM 数据源不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 LiteLLM GitHub 获取模型数据 JSON
     */
    private String fetchModelData() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 尝试主地址
        String content = tryFetchUrl(client, LITELLM_MODEL_DATA_URL);
        if (content != null) {
            return content;
        }

        // 尝试镜像地址
        content = tryFetchUrl(client, LITELLM_MODEL_DATA_URL_MIRROR);
        if (content != null) {
            return content;
        }

        throw new TemplateSyncException(SOURCE_NAME, "无法从 LiteLLM 获取模型数据");
    }

    private String tryFetchUrl(OkHttpClient client, String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            log.warn("获取 {} 失败: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * 解析 LiteLLM 模型数据 JSON
     */
    private List<ModelTemplate> parseModelData(String jsonContent) {
        List<ModelTemplate> templates = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String modelKey = entry.getKey();
                JsonNode modelData = entry.getValue();

                // 跳过非模型节点（如 sample_spec）
                if (!modelData.isObject() || modelKey.startsWith("sample_")) {
                    continue;
                }

                try {
                    ModelTemplate template = parseModelEntry(modelKey, modelData);
                    if (template != null) {
                        templates.add(template);
                    }
                } catch (Exception e) {
                    log.debug("解析模型 {} 失败: {}", modelKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new TemplateSyncException(SOURCE_NAME, "解析 LiteLLM 模型数据失败: " + e.getMessage(), e);
        }

        return templates;
    }

    /**
     * 解析单个模型条目
     */
    private ModelTemplate parseModelEntry(String modelKey, JsonNode modelData) {
        // 获取提供商
        String litellmProvider = getTextValue(modelData, "litellm_provider");
        if (litellmProvider == null) {
            return null;
        }

        // 过滤不支持的提供商
        if (EXCLUDED_PROVIDERS.contains(litellmProvider)) {
            return null;
        }

        // 映射提供商代码
        String providerCode = PROVIDER_MAPPING.getOrDefault(litellmProvider, litellmProvider);

        // 获取模型类型
        String mode = getTextValue(modelData, "mode");
        ModelType modelType = MODE_TYPE_MAPPING.getOrDefault(mode, ModelType.CHAT);

        // 提取模型代码（去除提供商前缀）
        String modelCode = extractModelCode(modelKey, litellmProvider);

        // 构建模板
        ModelTemplate template = new ModelTemplate();
        template.setCode(modelCode);
        template.setName(generateModelName(modelCode));
        template.setType(modelType);
        template.setProviderCode(providerCode);
        template.setSource(TemplateSource.SYSTEM);

        // 解析能力配置
        Map<String, Object> capabilities = new HashMap<>();

        Integer maxInputTokens = getIntValue(modelData, "max_input_tokens");
        Integer maxOutputTokens = getIntValue(modelData, "max_output_tokens");
        Integer maxTokens = getIntValue(modelData, "max_tokens");

        if (maxInputTokens != null) {
            capabilities.put("contextWindow", maxInputTokens);
        }
        if (maxOutputTokens != null) {
            capabilities.put("maxOutputTokens", maxOutputTokens);
            template.setMaxTokenLimit(maxOutputTokens);
        } else if (maxTokens != null) {
            template.setMaxTokenLimit(maxTokens);
        }

        // 功能支持标志
        if (modelData.has("supports_vision")) {
            capabilities.put("supportVision", modelData.get("supports_vision").asBoolean());
        }
        if (modelData.has("supports_function_calling")) {
            capabilities.put("supportFunctionCall", modelData.get("supports_function_calling").asBoolean());
        }
        if (modelData.has("supports_parallel_function_calling")) {
            capabilities.put("supportParallelFunctionCall", modelData.get("supports_parallel_function_calling").asBoolean());
        }
        if (modelData.has("supports_response_schema")) {
            capabilities.put("supportResponseSchema", modelData.get("supports_response_schema").asBoolean());
        }

        // 流式支持（大部分 chat 模型都支持）
        boolean supportsStream = modelType == ModelType.CHAT || modelType == ModelType.EMBEDDING;
        template.setSupportStream(supportsStream);
        capabilities.put("supportStream", supportsStream);

        template.setCapabilities(capabilities);

        // 设置默认配置
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("temperature", 0.7);
        defaultConfig.put("topP", 0.9);
        template.setDefaultConfig(defaultConfig);

        // 设置标签
        List<String> tags = generateTags(modelCode, litellmProvider, modelData);
        template.setTags(tags);

        template.setDeprecated(false);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        return template;
    }

    /**
     * 提取模型代码（去除提供商前缀）
     */
    private String extractModelCode(String modelKey, String provider) {
        // 处理类似 "openai/gpt-4o" 的格式
        if (modelKey.contains("/")) {
            String[] parts = modelKey.split("/", 2);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return modelKey;
    }

    /**
     * 生成模型显示名称
     */
    private String generateModelName(String modelCode) {
        // 简单的名称美化：gpt-4o -> GPT-4o
        String name = modelCode;
        if (name.startsWith("gpt-")) {
            name = "GPT-" + name.substring(4);
        } else if (name.startsWith("claude-")) {
            name = "Claude " + capitalizeFirst(name.substring(7));
        } else if (name.startsWith("gemini-")) {
            name = "Gemini " + capitalizeFirst(name.substring(7));
        } else if (name.startsWith("llama-") || name.startsWith("llama3")) {
            name = "Llama " + name.substring(name.indexOf("-") + 1);
        }
        return name;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 生成标签
     */
    private List<String> generateTags(String modelCode, String provider, JsonNode modelData) {
        List<String> tags = new ArrayList<>();

        // 基于模型名称生成标签
        String lower = modelCode.toLowerCase();
        if (lower.contains("mini") || lower.contains("small") || lower.contains("lite")) {
            tags.add("轻量");
        }
        if (lower.contains("pro") || lower.contains("plus") || lower.contains("max")) {
            tags.add("高性能");
        }
        if (lower.contains("turbo") || lower.contains("flash")) {
            tags.add("快速");
        }
        if (lower.contains("vision") || lower.contains("-vl")) {
            tags.add("视觉");
        }

        // 基于提供商添加标签
        if ("ollama".equals(provider)) {
            tags.add("本地");
        }

        return tags;
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asInt()
                : null;
    }
}
