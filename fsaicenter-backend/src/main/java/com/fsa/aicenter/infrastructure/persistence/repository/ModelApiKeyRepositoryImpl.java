package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.repository.ModelApiKeyRepository;
import com.fsa.aicenter.domain.model.valueobject.HealthStatus;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelApiKeyPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型API Key仓储实现
 *
 * @author FSA AI Center
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ModelApiKeyRepositoryImpl implements ModelApiKeyRepository {

    private final ModelApiKeyMapper mapper;

    @Override
    public Optional<ModelApiKey> findById(Long id) {
        ModelApiKeyPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ModelApiKey> findAvailableKeysByModelId(Long modelId) {
        LambdaQueryWrapper<ModelApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelApiKeyPO::getModelId, modelId)
                .eq(ModelApiKeyPO::getStatus, 1) // 启用
                .eq(ModelApiKeyPO::getHealthStatus, 1) // 健康
                .and(w -> w.isNull(ModelApiKeyPO::getExpireTime)
                        .or().gt(ModelApiKeyPO::getExpireTime, LocalDateTime.now())) // 未过期
                .orderByDesc(ModelApiKeyPO::getWeight) // 按权重排序
                .orderByAsc(ModelApiKeyPO::getSortOrder);

        List<ModelApiKeyPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(this::toDomain)
                .filter(ModelApiKey::hasQuota) // 过滤有配额的
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelApiKey> findByModelId(Long modelId) {
        LambdaQueryWrapper<ModelApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelApiKeyPO::getModelId, modelId)
                .orderByDesc(ModelApiKeyPO::getWeight)
                .orderByAsc(ModelApiKeyPO::getSortOrder);

        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ModelApiKey save(ModelApiKey modelApiKey) {
        ModelApiKeyPO po = toPO(modelApiKey);
        po.setCreatedTime(LocalDateTime.now());
        po.setUpdatedTime(LocalDateTime.now());
        mapper.insert(po);
        modelApiKey.setId(po.getId());
        return modelApiKey;
    }

    @Override
    public void update(ModelApiKey modelApiKey) {
        ModelApiKeyPO po = toPO(modelApiKey);
        po.setUpdatedTime(LocalDateTime.now());
        mapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void recordSuccess(Long id) {
        mapper.recordSuccess(id, LocalDateTime.now());
    }

    @Override
    public void recordFailure(Long id) {
        mapper.recordFailure(id, LocalDateTime.now());
    }

    @Override
    public boolean consumeQuota(Long id, long amount) {
        int rows = mapper.consumeQuota(id, amount);
        return rows > 0;
    }

    private ModelApiKey toDomain(ModelApiKeyPO po) {
        ModelApiKey domain = new ModelApiKey();
        BeanUtils.copyProperties(po, domain);
        if (po.getHealthStatus() != null) {
            domain.setHealthStatus(HealthStatus.fromCode(po.getHealthStatus()));
        }
        return domain;
    }

    private ModelApiKeyPO toPO(ModelApiKey domain) {
        ModelApiKeyPO po = new ModelApiKeyPO();
        BeanUtils.copyProperties(domain, po);
        if (domain.getHealthStatus() != null) {
            po.setHealthStatus(domain.getHealthStatus().getCode());
        }
        return po;
    }
}
