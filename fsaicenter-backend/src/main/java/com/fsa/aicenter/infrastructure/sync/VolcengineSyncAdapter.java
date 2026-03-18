package com.fsa.aicenter.infrastructure.sync;

import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 火山引擎（豆包）模型同步适配器
 * 基于官方文档的内置模型数据
 *
 * @author FSA AI Center
 * @see <a href="https://www.volcengine.com/docs/82379/1330310">火山方舟模型列表</a>
 */
@Slf4j
@Component
public class VolcengineSyncAdapter implements TemplateSyncAdapter {

    private static final String SOURCE_NAME = "volcengine";
    private static final String DISPLAY_NAME = "火山引擎（豆包）";
    private static final String PROVIDER_CODE = "volcengine";

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
        log.info("开始获取火山引擎模型模板...");
        List<ModelTemplate> templates = buildBuiltinTemplates();
        log.info("获取到 {} 个火山引擎模型模板", templates.size());
        return templates;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<String> getSupportedProviderCodes() {
        return List.of(PROVIDER_CODE);
    }

    /**
     * 构建内置模型模板列表
     * 基于火山引擎官方文档 2025年最新模型
     */
    private List<ModelTemplate> buildBuiltinTemplates() {
        List<ModelTemplate> templates = new ArrayList<>();

        // ========== 豆包 Seed 1.6 系列（最新旗舰） ==========
        templates.add(createTemplate(
                "doubao-seed-1.6",
                "豆包 Seed 1.6",
                ModelType.CHAT,
                256000, 16384,
                true, true, true, false,
                List.of("旗舰", "推理", "最新"),
                "豆包1.6旗舰版，支持思考/非思考/自适应三种模式，复杂推理能力强",
                LocalDate.of(2025, 6, 1)
        ));

        templates.add(createTemplate(
                "doubao-seed-1.6-thinking",
                "豆包 Seed 1.6 思考版",
                ModelType.CHAT,
                256000, 32768,
                true, true, true, false,
                List.of("旗舰", "深度思考", "推理"),
                "豆包1.6思考版，专注深度推理任务",
                LocalDate.of(2025, 6, 1)
        ));

        templates.add(createTemplate(
                "doubao-seed-1.6-flash",
                "豆包 Seed 1.6 Flash",
                ModelType.CHAT,
                256000, 8192,
                true, true, true, false,
                List.of("快速", "高性价比"),
                "豆包1.6快速版，速度快成本低",
                LocalDate.of(2025, 6, 1)
        ));

        templates.add(createTemplate(
                "doubao-seed-1.6-lite",
                "豆包 Seed 1.6 Lite",
                ModelType.CHAT,
                128000, 4096,
                true, true, false, false,
                List.of("轻量", "低成本"),
                "豆包1.6轻量版，适合简单任务",
                LocalDate.of(2025, 6, 1)
        ));

        templates.add(createTemplate(
                "doubao-seed-1.6-vision",
                "豆包 Seed 1.6 视觉版",
                ModelType.CHAT,
                128000, 8192,
                true, true, true, true,
                List.of("视觉", "多模态"),
                "豆包1.6视觉版，支持图像理解",
                LocalDate.of(2025, 6, 1)
        ));

        // ========== 豆包 1.5 系列 ==========
        templates.add(createTemplate(
                "doubao-1.5-pro-32k",
                "豆包 1.5 Pro 32K",
                ModelType.CHAT,
                32000, 4096,
                true, true, true, false,
                List.of("专业", "稳定"),
                "豆包1.5专业版，32K上下文",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-pro-128k",
                "豆包 1.5 Pro 128K",
                ModelType.CHAT,
                128000, 4096,
                true, true, true, false,
                List.of("专业", "长上下文"),
                "豆包1.5专业版，128K长上下文",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-pro-256k",
                "豆包 1.5 Pro 256K",
                ModelType.CHAT,
                256000, 4096,
                true, true, true, false,
                List.of("专业", "超长上下文"),
                "豆包1.5专业版，256K超长上下文",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-lite-32k",
                "豆包 1.5 Lite 32K",
                ModelType.CHAT,
                32000, 4096,
                true, true, false, false,
                List.of("轻量", "高性价比"),
                "豆包1.5轻量版",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-vision-pro",
                "豆包 1.5 Vision Pro",
                ModelType.CHAT,
                128000, 4096,
                true, true, true, true,
                List.of("视觉", "专业"),
                "豆包1.5视觉专业版，支持图像理解和分析",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-vision-lite",
                "豆包 1.5 Vision Lite",
                ModelType.CHAT,
                32000, 4096,
                true, false, false, true,
                List.of("视觉", "轻量"),
                "豆包1.5视觉轻量版",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-thinking-pro",
                "豆包 1.5 Thinking Pro",
                ModelType.CHAT,
                128000, 16384,
                true, true, true, false,
                List.of("深度思考", "推理"),
                "豆包1.5思考专业版，深度推理能力",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "doubao-1.5-thinking-vision-pro",
                "豆包 1.5 Thinking Vision Pro",
                ModelType.CHAT,
                128000, 16384,
                true, true, true, true,
                List.of("深度思考", "视觉", "推理"),
                "豆包1.5视觉思考版，支持图像深度推理",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== 经典版本（兼容旧项目） ==========
        templates.add(createTemplate(
                "doubao-pro-32k",
                "豆包 Pro 32K",
                ModelType.CHAT,
                32000, 4096,
                true, true, true, false,
                List.of("经典", "稳定"),
                "豆包经典Pro版本",
                LocalDate.of(2024, 5, 1)
        ));

        templates.add(createTemplate(
                "doubao-pro-128k",
                "豆包 Pro 128K",
                ModelType.CHAT,
                128000, 4096,
                true, true, true, false,
                List.of("经典", "长上下文"),
                "豆包经典Pro长上下文版本",
                LocalDate.of(2024, 5, 1)
        ));

        templates.add(createTemplate(
                "doubao-lite-32k",
                "豆包 Lite 32K",
                ModelType.CHAT,
                32000, 4096,
                true, true, false, false,
                List.of("经典", "轻量"),
                "豆包经典Lite版本",
                LocalDate.of(2024, 5, 1)
        ));

        // ========== Embedding 模型 ==========
        templates.add(createEmbeddingTemplate(
                "doubao-embedding",
                "豆包 Embedding",
                2048, 1024,
                "豆包文本向量模型"
        ));

        templates.add(createEmbeddingTemplate(
                "doubao-embedding-large",
                "豆包 Embedding Large",
                2048, 2048,
                "豆包大型文本向量模型，精度更高"
        ));

        templates.add(createTemplate(
                "doubao-embedding-vision",
                "豆包 Embedding Vision",
                ModelType.EMBEDDING,
                2048, 1024,
                false, false, false, true,
                List.of("向量", "多模态"),
                "豆包多模态向量模型，支持图文",
                LocalDate.of(2024, 8, 1)
        ));

        // ========== 图像生成模型 ==========
        templates.add(createImageTemplate(
                "doubao-seedream-4.0",
                "豆包 Seedream 4.0",
                "豆包最新图像生成模型4.0版本"
        ));

        templates.add(createImageTemplate(
                "doubao-seedream-4.5",
                "豆包 Seedream 4.5",
                "豆包图像生成模型4.5版本，质量更高"
        ));

        templates.add(createImageTemplate(
                "doubao-seedream-3.0-t2i",
                "豆包 Seedream 3.0 文生图",
                "豆包3.0文生图模型"
        ));

        templates.add(createImageTemplate(
                "doubao-seededit-3.0-i2i",
                "豆包 Seededit 3.0 图生图",
                "豆包3.0图像编辑模型"
        ));

        // ========== 视频生成模型 ==========
        templates.add(createVideoTemplate(
                "doubao-seedance-1.0-pro",
                "豆包 Seedance 1.0 Pro",
                "豆包视频生成专业版"
        ));

        templates.add(createVideoTemplate(
                "doubao-seedance-1.0-lite",
                "豆包 Seedance 1.0 Lite",
                "豆包视频生成轻量版"
        ));

        // ========== 翻译模型 ==========
        templates.add(createTemplate(
                "doubao-seed-translation",
                "豆包翻译",
                ModelType.CHAT,
                8000, 4096,
                true, false, false, false,
                List.of("翻译", "专用"),
                "豆包专用翻译模型",
                LocalDate.of(2024, 10, 1)
        ));

        return templates;
    }

    /**
     * 创建聊天模型模板
     */
    private ModelTemplate createTemplate(
            String code, String name, ModelType type,
            int contextWindow, int maxOutput,
            boolean supportStream, boolean supportFunctionCall,
            boolean supportThinking, boolean supportVision,
            List<String> tags, String description, LocalDate releaseDate) {

        ModelTemplate template = new ModelTemplate();
        template.setCode(code);
        template.setName(name);
        template.setType(type);
        template.setProviderCode(PROVIDER_CODE);
        template.setSource(TemplateSource.SYSTEM);
        template.setSupportStream(supportStream);
        template.setMaxTokenLimit(maxOutput);
        template.setDescription(description);
        template.setTags(tags);
        template.setDeprecated(false);
        template.setReleaseDate(releaseDate);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("contextWindow", contextWindow);
        capabilities.put("maxOutputTokens", maxOutput);
        capabilities.put("supportStream", supportStream);
        capabilities.put("supportFunctionCall", supportFunctionCall);
        capabilities.put("supportThinking", supportThinking);
        capabilities.put("supportVision", supportVision);
        template.setCapabilities(capabilities);

        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("temperature", 0.7);
        defaultConfig.put("topP", 0.9);
        template.setDefaultConfig(defaultConfig);

        return template;
    }

    /**
     * 创建Embedding模型模板
     */
    private ModelTemplate createEmbeddingTemplate(String code, String name, int maxInput, int dimension, String description) {
        ModelTemplate template = new ModelTemplate();
        template.setCode(code);
        template.setName(name);
        template.setType(ModelType.EMBEDDING);
        template.setProviderCode(PROVIDER_CODE);
        template.setSource(TemplateSource.SYSTEM);
        template.setSupportStream(false);
        template.setMaxTokenLimit(maxInput);
        template.setDescription(description);
        template.setTags(List.of("向量", "文本"));
        template.setDeprecated(false);
        template.setReleaseDate(LocalDate.of(2024, 6, 1));
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("maxInputTokens", maxInput);
        capabilities.put("dimension", dimension);
        template.setCapabilities(capabilities);

        template.setDefaultConfig(new HashMap<>());

        return template;
    }

    /**
     * 创建图像生成模型模板
     */
    private ModelTemplate createImageTemplate(String code, String name, String description) {
        ModelTemplate template = new ModelTemplate();
        template.setCode(code);
        template.setName(name);
        template.setType(ModelType.IMAGE);
        template.setProviderCode(PROVIDER_CODE);
        template.setSource(TemplateSource.SYSTEM);
        template.setSupportStream(false);
        template.setDescription(description);
        template.setTags(List.of("图像生成"));
        template.setDeprecated(false);
        template.setReleaseDate(LocalDate.of(2024, 10, 1));
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedSizes", List.of("1024x1024", "1024x768", "768x1024"));
        template.setCapabilities(capabilities);

        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("size", "1024x1024");
        defaultConfig.put("quality", "standard");
        template.setDefaultConfig(defaultConfig);

        return template;
    }

    /**
     * 创建视频生成模型模板
     */
    private ModelTemplate createVideoTemplate(String code, String name, String description) {
        ModelTemplate template = new ModelTemplate();
        template.setCode(code);
        template.setName(name);
        template.setType(ModelType.VIDEO);
        template.setProviderCode(PROVIDER_CODE);
        template.setSource(TemplateSource.SYSTEM);
        template.setSupportStream(false);
        template.setDescription(description);
        template.setTags(List.of("视频生成"));
        template.setDeprecated(false);
        template.setReleaseDate(LocalDate.of(2024, 12, 1));
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedDurations", List.of(5, 10));
        template.setCapabilities(capabilities);

        template.setDefaultConfig(new HashMap<>());

        return template;
    }
}
