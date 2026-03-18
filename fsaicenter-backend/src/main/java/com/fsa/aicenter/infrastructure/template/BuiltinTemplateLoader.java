package com.fsa.aicenter.infrastructure.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * 内置模板加载器
 * 从classpath加载model-templates.json并解析为ModelTemplate列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinTemplateLoader {

    private static final String TEMPLATE_FILE = "templates/model-templates.json";
    private final ObjectMapper objectMapper;

    /**
     * 缓存内置模板
     */
    private List<ModelTemplate> cachedTemplates;

    /**
     * 提供商名称映射
     */
    private Map<String, String> providerNameMap;

    @PostConstruct
    public void init() {
        loadTemplates();
    }

    /**
     * 获取所有内置模板
     *
     * @return 内置模板列表
     */
    public List<ModelTemplate> getBuiltinTemplates() {
        if (cachedTemplates == null) {
            loadTemplates();
        }
        return new ArrayList<>(cachedTemplates);
    }

    /**
     * 获取提供商名称
     *
     * @param providerCode 提供商代码
     * @return 提供商名称
     */
    public String getProviderName(String providerCode) {
        if (providerNameMap == null) {
            loadTemplates();
        }
        return providerNameMap.getOrDefault(providerCode, providerCode);
    }

    /**
     * 加载模板文件（从classpath）
     */
    private synchronized void loadTemplates() {
        if (cachedTemplates != null) {
            return; // 已加载，避免重复加载
        }

        log.info("开始加载内置模板: {}", TEMPLATE_FILE);
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_FILE);
            if (!resource.exists()) {
                log.error("内置模板文件不存在: {}", TEMPLATE_FILE);
                cachedTemplates = Collections.emptyList();
                providerNameMap = Collections.emptyMap();
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(inputStream);

                List<ModelTemplate> templates = new ArrayList<>();
                Map<String, String> nameMap = new HashMap<>();

                // 遍历 providers 数组
                JsonNode providers = root.get("providers");
                if (providers != null && providers.isArray()) {
                    for (JsonNode providerNode : providers) {
                        String providerCode = providerNode.get("code").asText();
                        String providerName = providerNode.get("name").asText();
                        nameMap.put(providerCode, providerName);

                        // 遍历该提供商的 models 数组
                        JsonNode models = providerNode.get("models");
                        if (models != null && models.isArray()) {
                            for (JsonNode modelNode : models) {
                                ModelTemplate template = parseModelTemplate(modelNode, providerCode);
                                if (template != null) {
                                    templates.add(template);
                                }
                            }
                        }
                    }
                }

                cachedTemplates = Collections.unmodifiableList(templates);
                providerNameMap = Collections.unmodifiableMap(nameMap);

                log.info("内置模板加载成功，共 {} 个提供商，{} 个模板", nameMap.size(), templates.size());
            }
        } catch (IOException e) {
            log.error("加载内置模板失败: {}", TEMPLATE_FILE, e);
            cachedTemplates = Collections.emptyList();
            providerNameMap = Collections.emptyMap();
        }
    }

    /**
     * 解析单个模板节点
     *
     * @param node         模板JSON节点
     * @param providerCode 提供商代码
     * @return ModelTemplate对象
     */
    private ModelTemplate parseModelTemplate(JsonNode node, String providerCode) {
        try {
            ModelTemplate template = new ModelTemplate();

            // 基础字段
            template.setCode(node.get("code").asText());
            template.setName(node.get("name").asText());
            template.setProviderCode(providerCode);
            template.setSource(TemplateSource.BUILTIN);

            // 模型类型
            String typeCode = node.get("type").asText();
            template.setType(ModelType.fromCode(typeCode));

            // 流式支持
            JsonNode supportStreamNode = node.get("supportStream");
            Boolean supportStream = supportStreamNode != null ? supportStreamNode.asBoolean() : false;
            template.setSupportStream(supportStream);

            // 最大Token限制（可选）
            JsonNode maxTokenLimitNode = node.get("maxTokenLimit");
            if (maxTokenLimitNode != null && !maxTokenLimitNode.isNull()) {
                template.setMaxTokenLimit(maxTokenLimitNode.asInt());
            }

            // 描述（可选）
            JsonNode descriptionNode = node.get("description");
            if (descriptionNode != null && !descriptionNode.isNull()) {
                template.setDescription(descriptionNode.asText());
            }

            // 能力配置（JSONB）
            JsonNode capabilitiesNode = node.get("capabilities");
            if (capabilitiesNode != null && !capabilitiesNode.isNull()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> capabilities = objectMapper.convertValue(capabilitiesNode, Map.class);
                template.setCapabilities(capabilities);
            }

            // 默认参数配置（JSONB）
            JsonNode defaultConfigNode = node.get("defaultConfig");
            if (defaultConfigNode != null && !defaultConfigNode.isNull()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> defaultConfig = objectMapper.convertValue(defaultConfigNode, Map.class);
                template.setDefaultConfig(defaultConfig);
            }

            // 标签数组
            JsonNode tagsNode = node.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                List<String> tags = new ArrayList<>();
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText());
                }
                template.setTags(tags);
            }

            // 是否弃用
            JsonNode deprecatedNode = node.get("deprecated");
            Boolean deprecated = deprecatedNode != null ? deprecatedNode.asBoolean() : false;
            template.setDeprecated(deprecated);

            // 发布日期（可选）
            JsonNode releaseDateNode = node.get("releaseDate");
            if (releaseDateNode != null && !releaseDateNode.isNull()) {
                template.setReleaseDate(LocalDate.parse(releaseDateNode.asText()));
            }

            // 一致性校验
            validateStreamConsistency(template);

            return template;
        } catch (Exception e) {
            log.error("解析模板失败: providerCode={}, node={}", providerCode, node, e);
            return null;
        }
    }

    /**
     * 一致性校验：顶层 supportStream 应与 capabilities 一致
     *
     * @param template 模板对象
     */
    private void validateStreamConsistency(ModelTemplate template) {
        if (template.getCapabilities() == null) {
            return;
        }

        Object capabilitiesStreamValue = template.getCapabilities().get("supportStream");
        if (capabilitiesStreamValue != null) {
            boolean capabilitiesStream = Boolean.parseBoolean(capabilitiesStreamValue.toString());
            boolean topLevelStream = Boolean.TRUE.equals(template.getSupportStream());

            if (capabilitiesStream != topLevelStream) {
                log.warn("模板 supportStream 不一致: code={}, topLevel={}, capabilities={}, 使用 topLevel 值",
                        template.getCode(), topLevelStream, capabilitiesStream);
            }
        }
    }

    /**
     * 重新加载模板（用于热更新）
     */
    public synchronized void reload() {
        log.info("重新加载内置模板");
        cachedTemplates = null;
        providerNameMap = null;
        loadTemplates();
    }
}
