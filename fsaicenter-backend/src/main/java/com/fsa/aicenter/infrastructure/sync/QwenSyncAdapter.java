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
 * 通义千问模型同步适配器
 * 基于阿里云百炼官方文档的内置模型数据
 *
 * @author FSA AI Center
 * @see <a href="https://help.aliyun.com/zh/model-studio/models">阿里云百炼模型列表</a>
 */
@Slf4j
@Component
public class QwenSyncAdapter implements TemplateSyncAdapter {

    private static final String SOURCE_NAME = "qwen";
    private static final String DISPLAY_NAME = "通义千问（阿里云）";
    private static final String PROVIDER_CODE = "qwen";

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
        log.info("开始获取通义千问模型模板...");
        List<ModelTemplate> templates = buildBuiltinTemplates();
        log.info("获取到 {} 个通义千问模型模板", templates.size());
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
     * 基于阿里云百炼官方文档 2025年最新模型
     */
    private List<ModelTemplate> buildBuiltinTemplates() {
        List<ModelTemplate> templates = new ArrayList<>();

        // ========== Qwen3 Max 系列（旗舰） ==========
        templates.add(createTemplate(
                "qwen3-max",
                "Qwen3 Max",
                ModelType.CHAT,
                262144, 65536,
                true, true, true, false,
                List.of("旗舰", "最新", "推理"),
                "通义千问3.0旗舰版，复杂多步骤任务，支持联网搜索",
                LocalDate.of(2025, 9, 1)
        ));

        templates.add(createTemplate(
                "qwen3-max-thinking",
                "Qwen3 Max Thinking",
                ModelType.CHAT,
                262144, 65536,
                true, true, true, false,
                List.of("旗舰", "深度思考"),
                "通义千问3.0旗舰思考版，支持思维链推理",
                LocalDate.of(2025, 9, 1)
        ));

        // ========== Qwen Max 系列（稳定版） ==========
        templates.add(createTemplate(
                "qwen-max",
                "Qwen Max",
                ModelType.CHAT,
                32768, 8192,
                true, true, true, false,
                List.of("旗舰", "稳定"),
                "通义千问Max稳定版，效果最好的模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen-max-latest",
                "Qwen Max Latest",
                ModelType.CHAT,
                131072, 16384,
                true, true, true, false,
                List.of("旗舰", "最新"),
                "通义千问Max最新版，基于Qwen2.5-Max",
                LocalDate.of(2025, 1, 1)
        ));

        templates.add(createTemplate(
                "qwen-max-longcontext",
                "Qwen Max 长上下文",
                ModelType.CHAT,
                1000000, 8192,
                true, true, true, false,
                List.of("旗舰", "超长上下文"),
                "通义千问Max百万级上下文版本",
                LocalDate.of(2025, 1, 1)
        ));

        // ========== Qwen Plus 系列（均衡） ==========
        templates.add(createTemplate(
                "qwen-plus",
                "Qwen Plus",
                ModelType.CHAT,
                131072, 16384,
                true, true, true, false,
                List.of("均衡", "推荐"),
                "通义千问Plus，效果、速度、成本均衡",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen-plus-latest",
                "Qwen Plus Latest",
                ModelType.CHAT,
                1000000, 16384,
                true, true, true, false,
                List.of("均衡", "最新"),
                "通义千问Plus最新版，支持百万上下文",
                LocalDate.of(2025, 1, 1)
        ));

        templates.add(createTemplate(
                "qwen3-plus",
                "Qwen3 Plus",
                ModelType.CHAT,
                262144, 32768,
                true, true, true, false,
                List.of("均衡", "Qwen3"),
                "通义千问3.0 Plus版本",
                LocalDate.of(2025, 9, 1)
        ));

        // ========== Qwen Turbo/Flash 系列（快速） ==========
        templates.add(createTemplate(
                "qwen-turbo",
                "Qwen Turbo",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, false,
                List.of("快速", "经济"),
                "通义千问Turbo，速度快成本低（建议迁移到Flash）",
                LocalDate.of(2024, 6, 1)
        ));

        templates.add(createTemplate(
                "qwen-turbo-latest",
                "Qwen Turbo Latest",
                ModelType.CHAT,
                1000000, 8192,
                true, true, false, false,
                List.of("快速", "经济"),
                "通义千问Turbo最新版",
                LocalDate.of(2025, 1, 1)
        ));

        templates.add(createTemplate(
                "qwen3-flash",
                "Qwen3 Flash",
                ModelType.CHAT,
                1000000, 16384,
                true, true, false, false,
                List.of("快速", "推荐"),
                "通义千问3.0 Flash，速度最快成本极低",
                LocalDate.of(2025, 9, 1)
        ));

        templates.add(createTemplate(
                "qwen3-flash-thinking",
                "Qwen3 Flash Thinking",
                ModelType.CHAT,
                1000000, 32768,
                true, true, true, false,
                List.of("快速", "思考"),
                "通义千问3.0 Flash思考版",
                LocalDate.of(2025, 9, 1)
        ));

        // ========== Qwen VL 视觉系列 ==========
        templates.add(createTemplate(
                "qwen-vl-max",
                "Qwen VL Max",
                ModelType.CHAT,
                32768, 8192,
                true, true, true, true,
                List.of("视觉", "旗舰"),
                "通义千问VL视觉旗舰版，支持图像理解",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen-vl-plus",
                "Qwen VL Plus",
                ModelType.CHAT,
                32768, 8192,
                true, true, false, true,
                List.of("视觉", "均衡"),
                "通义千问VL视觉均衡版",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-vl-72b-instruct",
                "Qwen2.5 VL 72B",
                ModelType.CHAT,
                131072, 8192,
                true, true, true, true,
                List.of("视觉", "开源", "大参数"),
                "通义千问2.5 VL 72B开源视觉模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-vl-7b-instruct",
                "Qwen2.5 VL 7B",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, true,
                List.of("视觉", "开源", "轻量"),
                "通义千问2.5 VL 7B开源视觉模型",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== Qwen Coder 代码系列 ==========
        templates.add(createTemplate(
                "qwen3-coder-plus",
                "Qwen3 Coder Plus",
                ModelType.CHAT,
                1000000, 65536,
                true, true, true, false,
                List.of("代码", "Agent"),
                "通义千问3.0代码专用模型，Coding Agent能力强",
                LocalDate.of(2025, 9, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-coder-32b-instruct",
                "Qwen2.5 Coder 32B",
                ModelType.CHAT,
                131072, 8192,
                true, true, true, false,
                List.of("代码", "开源"),
                "通义千问2.5代码32B开源模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-coder-7b-instruct",
                "Qwen2.5 Coder 7B",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, false,
                List.of("代码", "开源", "轻量"),
                "通义千问2.5代码7B开源模型",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== Qwen Math 数学系列 ==========
        templates.add(createTemplate(
                "qwen2.5-math-72b-instruct",
                "Qwen2.5 Math 72B",
                ModelType.CHAT,
                4096, 4096,
                true, false, true, false,
                List.of("数学", "推理"),
                "通义千问2.5数学专用模型",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== Qwen Omni 多模态系列 ==========
        templates.add(createTemplate(
                "qwen3-omni-turbo",
                "Qwen3 Omni Turbo",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, true,
                List.of("多模态", "语音"),
                "通义千问3.0全模态模型，支持语音图像",
                LocalDate.of(2025, 9, 1)
        ));

        // ========== Qwen Audio 音频系列 ==========
        templates.add(createTemplate(
                "qwen-audio-turbo",
                "Qwen Audio Turbo",
                ModelType.ASR,
                32768, 8192,
                false, false, false, false,
                List.of("语音识别"),
                "通义千问音频理解模型",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== 开源基础模型系列 ==========
        templates.add(createTemplate(
                "qwen2.5-72b-instruct",
                "Qwen2.5 72B Instruct",
                ModelType.CHAT,
                131072, 8192,
                true, true, true, false,
                List.of("开源", "大参数"),
                "通义千问2.5 72B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-32b-instruct",
                "Qwen2.5 32B Instruct",
                ModelType.CHAT,
                131072, 8192,
                true, true, true, false,
                List.of("开源"),
                "通义千问2.5 32B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-14b-instruct",
                "Qwen2.5 14B Instruct",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, false,
                List.of("开源"),
                "通义千问2.5 14B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-7b-instruct",
                "Qwen2.5 7B Instruct",
                ModelType.CHAT,
                131072, 8192,
                true, true, false, false,
                List.of("开源", "轻量"),
                "通义千问2.5 7B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-3b-instruct",
                "Qwen2.5 3B Instruct",
                ModelType.CHAT,
                32768, 8192,
                true, false, false, false,
                List.of("开源", "轻量"),
                "通义千问2.5 3B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        templates.add(createTemplate(
                "qwen2.5-1.5b-instruct",
                "Qwen2.5 1.5B Instruct",
                ModelType.CHAT,
                32768, 8192,
                true, false, false, false,
                List.of("开源", "超轻量"),
                "通义千问2.5 1.5B开源指令模型",
                LocalDate.of(2024, 12, 1)
        ));

        // ========== Embedding 模型 ==========
        templates.add(createEmbeddingTemplate(
                "text-embedding-v3",
                "通义文本向量 V3",
                8192, 1024,
                "通义千问第三代文本向量模型"
        ));

        templates.add(createEmbeddingTemplate(
                "text-embedding-v2",
                "通义文本向量 V2",
                2048, 1536,
                "通义千问第二代文本向量模型"
        ));

        templates.add(createEmbeddingTemplate(
                "text-embedding-v1",
                "通义文本向量 V1",
                2048, 1536,
                "通义千问第一代文本向量模型"
        ));

        // ========== 图像生成模型 ==========
        templates.add(createImageTemplate(
                "wanx-v1",
                "通义万相 V1",
                "通义万相文生图模型"
        ));

        templates.add(createImageTemplate(
                "wanx2.1-t2i-turbo",
                "通义万相 2.1 Turbo",
                "通义万相2.1快速文生图模型"
        ));

        templates.add(createImageTemplate(
                "wanx2.1-t2i-plus",
                "通义万相 2.1 Plus",
                "通义万相2.1高质量文生图模型"
        ));

        // ========== 语音合成模型 ==========
        templates.add(createTTSTemplate(
                "cosyvoice-v1",
                "CosyVoice V1",
                "通义语音合成模型，多音色"
        ));

        templates.add(createTTSTemplate(
                "sambert-zhichu-v1",
                "Sambert 知楚",
                "通义语音合成模型 - 知楚音色"
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
        defaultConfig.put("topP", 0.8);
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
        template.setTags(List.of("图像生成", "万相"));
        template.setDeprecated(false);
        template.setReleaseDate(LocalDate.of(2024, 10, 1));
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedSizes", List.of("1024x1024", "720x1280", "1280x720"));
        template.setCapabilities(capabilities);

        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("size", "1024x1024");
        defaultConfig.put("n", 1);
        template.setDefaultConfig(defaultConfig);

        return template;
    }

    /**
     * 创建TTS语音合成模型模板
     */
    private ModelTemplate createTTSTemplate(String code, String name, String description) {
        ModelTemplate template = new ModelTemplate();
        template.setCode(code);
        template.setName(name);
        template.setType(ModelType.TTS);
        template.setProviderCode(PROVIDER_CODE);
        template.setSource(TemplateSource.SYSTEM);
        template.setSupportStream(true);
        template.setDescription(description);
        template.setTags(List.of("语音合成"));
        template.setDeprecated(false);
        template.setReleaseDate(LocalDate.of(2024, 8, 1));
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedFormats", List.of("mp3", "wav", "pcm"));
        template.setCapabilities(capabilities);

        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("format", "mp3");
        defaultConfig.put("sampleRate", 16000);
        template.setDefaultConfig(defaultConfig);

        return template;
    }
}
