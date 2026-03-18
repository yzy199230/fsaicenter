package com.fsa.aicenter.domain.model.repository;

import com.fsa.aicenter.domain.model.entity.ModelApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 模型API Key仓储接口
 *
 * @author FSA AI Center
 */
public interface ModelApiKeyRepository {

    /**
     * 根据ID查找
     */
    Optional<ModelApiKey> findById(Long id);

    /**
     * 根据模型ID查找所有可用的Key
     * @param modelId 模型ID
     * @return 可用的Key列表（已启用 + 健康 + 未过期 + 有配额）
     */
    List<ModelApiKey> findAvailableKeysByModelId(Long modelId);

    /**
     * 根据模型ID查找所有Key
     */
    List<ModelApiKey> findByModelId(Long modelId);

    /**
     * 保存
     */
    ModelApiKey save(ModelApiKey modelApiKey);

    /**
     * 更新
     */
    void update(ModelApiKey modelApiKey);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 记录成功请求
     */
    void recordSuccess(Long id);

    /**
     * 记录失败请求
     */
    void recordFailure(Long id);

    /**
     * 消费配额
     */
    boolean consumeQuota(Long id, long amount);
}
