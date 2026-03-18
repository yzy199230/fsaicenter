package com.fsa.aicenter.domain.model.repository;

import com.fsa.aicenter.domain.model.entity.Provider;
import java.util.List;
import java.util.Optional;

/**
 * 提供商仓储接口
 */
public interface ProviderRepository {
    Optional<Provider> findById(Long id);
    Optional<Provider> findByCode(String code);
    List<Provider> findAll();
    List<Provider> findEnabled();
    Provider save(Provider provider);
    void update(Provider provider);
    void delete(Long id);
}
