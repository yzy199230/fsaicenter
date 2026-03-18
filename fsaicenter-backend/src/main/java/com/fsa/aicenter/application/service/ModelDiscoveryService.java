package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateModelFromTemplateRequest;
import com.fsa.aicenter.application.dto.request.CreateModelsFromDiscoveryRequest;
import com.fsa.aicenter.application.dto.response.DiscoveredModelResponse;
import com.fsa.aicenter.application.dto.response.ModelTemplateResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.common.ModelDiscoveryAdapter;
import com.fsa.aicenter.infrastructure.adapter.common.ModelDiscoveryAdapter.DiscoveredModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型发现服务
 * <p>
 * 提供从AI提供商API自动发现模型的功能，并与模板库进行匹配。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final List<ModelDiscoveryAdapter> discoveryAdapters;
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final ModelTemplateService templateService;

    /**
     * 发现提供商的可用模型
     *
     * @param providerId 提供商ID
     * @param apiKey     API密钥（可选，用于需要认证的提供商）
     * @return 发现的模型列表
     */
    public List<DiscoveredModelResponse> discoverModels(Long providerId, String apiKey) {
        // 1. 获取提供商
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        log.info("开始发现提供商 {} 的模型", provider.getCode());

        // 2. 查找支持该提供商的适配器
        ModelDiscoveryAdapter adapter = findSupportedAdapter(provider);
        if (adapter == null) {
            log.warn("没有找到支持提供商 {} 的模型发现适配器", provider.getCode());
            throw new BusinessException(ErrorCode.DISCOVERY_NOT_SUPPORTED,
                    "该提供商不支持自动发现模型，请手动添加或从模板导入");
        }

        // 3. 调用适配器发现模型
        List<DiscoveredModel> discoveredModels = adapter.discoverModels(provider, apiKey);
        log.info("从 {} 发现了 {} 个模型", provider.getCode(), discoveredModels.size());

        // 4. 加载模板用于匹配
        List<ModelTemplateResponse> templates = templateService.listAllTemplates(
                provider.getCode(), null, null);
        Map<String, ModelTemplateResponse> templateMap = templates.stream()
                .collect(Collectors.toMap(
                        ModelTemplateResponse::getCode,
                        t -> t,
                        (existing, replacement) -> existing
                ));

        // 5. 转换为响应DTO，并匹配模板和检查是否已存在
        List<DiscoveredModelResponse> result = new ArrayList<>();
        for (DiscoveredModel discovered : discoveredModels) {
            DiscoveredModelResponse response = toResponse(discovered, provider, templateMap);
            result.add(response);
        }

        return result;
    }

    /**
     * 检查提供商是否支持模型发现
     *
     * @param providerId 提供商ID
     * @return true 表示支持
     */
    public boolean supportsDiscovery(Long providerId) {
        Provider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            return false;
        }
        return findSupportedAdapter(provider) != null;
    }

    /**
     * 查找支持该提供商的适配器
     */
    private ModelDiscoveryAdapter findSupportedAdapter(Provider provider) {
        for (ModelDiscoveryAdapter adapter : discoveryAdapters) {
            if (adapter.supportsDiscovery(provider)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * 转换为响应DTO
     */
    private DiscoveredModelResponse toResponse(
            DiscoveredModel discovered,
            Provider provider,
            Map<String, ModelTemplateResponse> templateMap) {

        // 匹配模板
        ModelTemplateResponse matchedTemplate = templateMap.get(discovered.id());

        // 检查是否已存在于系统中
        boolean existsInSystem = modelRepository.existsByProviderIdAndCode(
                provider.getId(), discovered.id());

        // 转换时间戳
        LocalDateTime createdAt = null;
        if (discovered.createdAt() != null) {
            createdAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(discovered.createdAt()),
                    ZoneId.systemDefault()
            );
        }

        return DiscoveredModelResponse.builder()
                .modelId(discovered.id())
                .name(discovered.name())
                .ownedBy(discovered.ownedBy())
                .inferredType(discovered.type())
                .existsInSystem(existsInSystem)
                .matchedTemplate(matchedTemplate)
                .createdAt(createdAt)
                .extra(convertToMap(discovered.extra()))
                .build();
    }

    /**
     * 安全地转换为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return null;
    }

    /**
     * 从发现的模型创建模型记录
     * <p>
     * 支持两种情况：
     * 1. 有模板的模型：使用模板配置创建
     * 2. 无模板的模型：自动生成基础配置（最多4个）
     *
     * @param request 创建请求
     * @return 成功创建的模型数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int createModelsFromDiscovery(CreateModelsFromDiscoveryRequest request) {
        // 1. 获取提供商
        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        log.info("开始从发现的模型创建: providerId={}, modelCount={}",
                provider.getId(), request.getModels().size());

        // 2. 分离有模板和无模板的模型
        List<CreateModelsFromDiscoveryRequest.DiscoveredModelItem> withTemplate = new ArrayList<>();
        List<CreateModelsFromDiscoveryRequest.DiscoveredModelItem> withoutTemplate = new ArrayList<>();

        for (CreateModelsFromDiscoveryRequest.DiscoveredModelItem item : request.getModels()) {
            if (Boolean.TRUE.equals(item.getHasTemplate()) && item.getTemplateCode() != null) {
                withTemplate.add(item);
            } else {
                withoutTemplate.add(item);
            }
        }

        // 3. 检查无模板模型数量限制（最多4个）
        if (withoutTemplate.size() > 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "无模板模型最多只能选择4个，当前选择了" + withoutTemplate.size() + "个");
        }

        int successCount = 0;

        // 4. 从模板创建模型
        if (!withTemplate.isEmpty()) {
            List<String> templateCodes = withTemplate.stream()
                    .map(CreateModelsFromDiscoveryRequest.DiscoveredModelItem::getTemplateCode)
                    .collect(Collectors.toList());
            CreateModelFromTemplateRequest templateRequest = new CreateModelFromTemplateRequest();
            templateRequest.setProviderId(request.getProviderId());
            templateRequest.setTemplateCodes(templateCodes);
            successCount += templateService.createModelsFromTemplates(templateRequest);
        }

        // 5. 为无模板模型自动生成配置并创建
        for (CreateModelsFromDiscoveryRequest.DiscoveredModelItem item : withoutTemplate) {
            try {
                // 检查是否已存在
                if (modelRepository.existsByProviderIdAndCode(provider.getId(), item.getModelId())) {
                    log.warn("模型已存在，跳过: providerId={}, modelId={}",
                            provider.getId(), item.getModelId());
                    continue;
                }

                // 创建模型
                AiModel model = new AiModel();
                model.setCode(item.getModelId());
                model.setName(item.getName() != null ? item.getName() : item.getModelId());
                model.setType(parseModelType(item.getType()));
                model.setProviderId(provider.getId());
                model.setSupportStream(isChatType(item.getType())); // chat类型默认支持流式
                model.setMaxTokenLimit(getDefaultMaxTokenLimit(item.getType()));
                model.setDescription("从提供商自动发现导入");
                model.setSortOrder(100);
                model.setStatus(EntityStatus.ENABLED);

                modelRepository.save(model);
                successCount++;
                log.info("创建无模板模型成功: code={}, type={}", item.getModelId(), item.getType());
            } catch (Exception e) {
                log.error("创建模型失败: modelId={}, error={}", item.getModelId(), e.getMessage());
            }
        }

        log.info("从发现模型创建完成: totalSuccess={}", successCount);
        return successCount;
    }

    /**
     * 解析模型类型
     */
    private ModelType parseModelType(String type) {
        if (type == null) {
            return ModelType.CHAT;
        }
        try {
            return ModelType.fromCode(type);
        } catch (Exception e) {
            return ModelType.CHAT; // 默认为chat类型
        }
    }

    /**
     * 判断是否为chat类型
     */
    private boolean isChatType(String type) {
        return type == null || "chat".equals(type) || "image_recognition".equals(type);
    }

    /**
     * 获取默认的最大Token限制
     */
    private Integer getDefaultMaxTokenLimit(String type) {
        if (type == null) {
            return 4096;
        }
        return switch (type) {
            case "chat", "image_recognition" -> 4096;
            case "embedding" -> 8192;
            default -> 4096;
        };
    }
}
