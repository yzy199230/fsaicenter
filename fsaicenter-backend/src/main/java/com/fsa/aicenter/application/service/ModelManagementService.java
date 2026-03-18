package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateModelRequest;
import com.fsa.aicenter.application.dto.request.ModelQueryRequest;
import com.fsa.aicenter.application.dto.request.TestModelRequest;
import com.fsa.aicenter.application.dto.request.UpdateModelRequest;
import com.fsa.aicenter.application.dto.response.ModelResponse;
import com.fsa.aicenter.application.dto.response.TestModelResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.test.ModelTestStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型管理服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManagementService {

    private final ModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    private final ModelTestStrategyFactory strategyFactory;

    /**
     * 创建模型
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createModel(CreateModelRequest request) {
        // 检查模型编码是否已存在
        if (modelRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.MODEL_CODE_EXISTS);
        }

        // 构建领域对象
        AiModel model = new AiModel();
        model.setCode(request.getCode());
        model.setName(request.getName());
        model.setType(request.getType());
        model.setProviderId(request.getProviderId());
        model.setConfig(request.getConfig());
        model.setSupportStream(request.getSupportStream());
        model.setMaxTokenLimit(request.getMaxTokenLimit());
        model.setDescription(request.getDescription());
        model.setSortOrder(request.getSortOrder());
        model.setCapabilities(request.getCapabilities());
        model.setStatus(EntityStatus.ENABLED);

        // 保存
        AiModel savedModel = modelRepository.save(model);
        log.info("创建模型成功: id={}, code={}", savedModel.getId(), savedModel.getCode());

        return savedModel.getId();
    }

    /**
     * 更新模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(Long id, UpdateModelRequest request) {
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        // 更新字段
        model.setName(request.getName());
        model.setConfig(request.getConfig());
        model.setSupportStream(request.getSupportStream());
        model.setMaxTokenLimit(request.getMaxTokenLimit());
        model.setDescription(request.getDescription());
        model.setSortOrder(request.getSortOrder());
        model.setCapabilities(request.getCapabilities());

        modelRepository.update(model);
        log.info("更新模型成功: id={}, code={}", model.getId(), model.getCode());
    }

    /**
     * 删除模型（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        modelRepository.delete(id);
        log.info("删除模型成功: id={}, code={}", model.getId(), model.getCode());
    }

    /**
     * 启用/禁用模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleModelStatus(Long id) {
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        EntityStatus newStatus = model.isEnabled() ? EntityStatus.DISABLED : EntityStatus.ENABLED;
        model.setStatus(newStatus);
        modelRepository.update(model);

        log.info("切换模型状态: id={}, status={}", model.getId(), newStatus);
    }

    /**
     * 查询模型详情
     */
    public ModelResponse getModel(Long id) {
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        return toResponse(model);
    }

    /**
     * 查询所有模型
     */
    public List<ModelResponse> listModels() {
        List<AiModel> models = modelRepository.findAll();
        return models.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 条件查询模型列表
     */
    public List<ModelResponse> listModels(ModelQueryRequest request) {
        ModelType modelType = null;
        if (StringUtils.hasText(request.getModelType())) {
            modelType = ModelType.fromCode(request.getModelType());
        }

        EntityStatus status = null;
        if (StringUtils.hasText(request.getStatus())) {
            status = EntityStatus.valueOf(request.getStatus());
        }

        List<AiModel> models = modelRepository.findByCondition(
            request.getKeyword(),
            modelType,
            request.getProviderId(),
            status
        );
        return models.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 测试模型
     */
    public TestModelResponse testModel(Long id, TestModelRequest request) {
        // 查询模型
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        // 查询提供商
        Provider provider = providerRepository.findById(model.getProviderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        // 使用策略模式执行测试
        return strategyFactory.getStrategy(model.getType()).test(model, provider, request);
    }

    /**
     * 流式测试模型
     */
    public SseEmitter testModelStream(Long id, TestModelRequest request) {
        // 查询模型
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        // 查询提供商
        Provider provider = providerRepository.findById(model.getProviderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        // 使用策略模式执行流式测试
        return strategyFactory.getStrategy(model.getType()).testStream(model, provider, request);
    }

    /**
     * 更新模型排序
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSortOrder(Long id, Integer sortOrder) {
        AiModel model = modelRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        model.setSortOrder(sortOrder);
        modelRepository.update(model);
        log.info("更新模型排序: id={}, sortOrder={}", id, sortOrder);
    }

    /**
     * 领域对象转响应DTO
     */
    private ModelResponse toResponse(AiModel model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setCode(model.getCode());
        response.setName(model.getName());
        response.setType(model.getType());
        response.setProviderId(model.getProviderId());
        // 查询提供商名称
        providerRepository.findById(model.getProviderId())
            .ifPresent(provider -> response.setProviderName(provider.getName()));
        response.setConfig(model.getConfig());
        response.setSupportStream(model.getSupportStream());
        response.setMaxTokenLimit(model.getMaxTokenLimit());
        response.setDescription(model.getDescription());
        response.setSortOrder(model.getSortOrder());
        response.setStatus(model.getStatus());
        response.setCapabilities(model.getCapabilities());
        response.setCreatedTime(model.getCreatedTime());
        response.setUpdatedTime(model.getUpdatedTime());
        return response;
    }
}
