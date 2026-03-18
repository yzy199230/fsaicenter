package com.fsa.aicenter.domain.model.repository;

import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import java.util.List;
import java.util.Optional;

/**
 * 模型仓储接口
 */
public interface ModelRepository {
    Optional<AiModel> findById(Long id);
    Optional<AiModel> findByCode(String code);
    List<AiModel> findByType(ModelType type);
    List<AiModel> findEnabledByType(ModelType type);
    List<AiModel> findAll();
    List<AiModel> findByCondition(String keyword, ModelType modelType, Long providerId, EntityStatus status);
    AiModel save(AiModel model);
    void update(AiModel model);
    void delete(Long id);

    /**
     * 检查模型是否存在
     */
    boolean existsByProviderIdAndCode(Long providerId, String code);
}
