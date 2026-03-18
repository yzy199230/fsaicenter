package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelConfig;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.exception.RepositoryException;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ModelRepositoryImpl implements ModelRepository {

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<AiModel> findById(Long id) {
        ModelPO po = modelMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AiModel> findByCode(String code) {
        ModelPO po = modelMapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AiModel> findByType(ModelType type) {
        List<ModelPO> poList = modelMapper.selectByType(type.getCode());
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModel> findEnabledByType(ModelType type) {
        List<ModelPO> poList = modelMapper.selectEnabledByType(type.getCode(), EntityStatus.ENABLED.getCode());
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModel> findAll() {
        List<ModelPO> poList = modelMapper.selectList(null);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModel> findByCondition(String keyword, ModelType modelType, Long providerId, EntityStatus status) {
        String modelTypeCode = modelType != null ? modelType.getCode() : null;
        Integer statusCode = status != null ? status.getCode() : null;
        List<ModelPO> poList = modelMapper.selectByCondition(keyword, modelTypeCode, providerId, statusCode);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AiModel save(AiModel model) {
        ModelPO po = toPO(model);
        modelMapper.insert(po);
        model.setId(po.getId());
        return model;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(AiModel model) {
        ModelPO po = toPO(model);
        modelMapper.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        modelMapper.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndCode(Long providerId, String code) {
        LambdaQueryWrapper<ModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelPO::getProviderId, providerId)
               .eq(ModelPO::getModelCode, code);
        return modelMapper.selectCount(wrapper) > 0;
    }

    // ========== 转换方法 ==========

    /**
     * PO转领域对象
     */
    private AiModel toDomain(ModelPO po) {
        // 验证必需的枚举字段
        if (po.getModelType() == null || po.getModelType().trim().isEmpty()) {
            log.error("Invalid model type for model {}: null or empty", po.getId());
            throw new IllegalStateException("Model type cannot be null for model: " + po.getId());
        }
        if (po.getStatus() == null) {
            log.error("Invalid status for model {}: null", po.getId());
            throw new IllegalStateException("Model status cannot be null for model: " + po.getId());
        }

        AiModel model = new AiModel();
        model.setId(po.getId());
        model.setCode(po.getModelCode());
        model.setName(po.getModelName());
        model.setType(ModelType.fromCode(po.getModelType()));
        model.setProviderId(po.getProviderId());
        model.setConfig(parseConfig(po.getConfig()));
        model.setSupportStream(po.getSupportStream());
        model.setMaxTokenLimit(po.getMaxTokens());
        model.setDescription(po.getDescription());
        model.setSortOrder(po.getSortOrder());
        model.setStatus(EntityStatus.fromCode(po.getStatus()));
        model.setCreatedTime(po.getCreatedTime());
        model.setUpdatedTime(po.getUpdatedTime());
        return model;
    }

    /**
     * 领域对象转PO
     */
    private ModelPO toPO(AiModel model) {
        ModelPO po = new ModelPO();
        po.setId(model.getId());
        po.setProviderId(model.getProviderId());
        po.setModelCode(model.getCode());
        po.setModelName(model.getName());
        po.setModelType(model.getType().getCode());
        po.setSupportStream(model.getSupportStream());
        po.setMaxTokens(model.getMaxTokenLimit());
        po.setConfig(serializeConfig(model.getConfig()));
        po.setDescription(model.getDescription());
        po.setSortOrder(model.getSortOrder());
        po.setStatus(model.getStatus().getCode());
        po.setCreatedTime(model.getCreatedTime());
        po.setUpdatedTime(model.getUpdatedTime());
        return po;
    }

    /**
     * 解析配置JSON为值对象
     */
    private ModelConfig parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(configJson, ModelConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse model config JSON: {}", configJson, e);
            throw new RepositoryException("Invalid model config format for JSON: " + configJson, e);
        }
    }

    /**
     * 序列化配置值对象为JSON
     */
    private String serializeConfig(ModelConfig config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize model config: {}", config, e);
            throw new RepositoryException("Failed to serialize model config", e);
        }
    }
}
