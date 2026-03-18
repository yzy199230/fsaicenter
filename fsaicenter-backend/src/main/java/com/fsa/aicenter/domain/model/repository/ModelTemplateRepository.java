package com.fsa.aicenter.domain.model.repository;

import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;

import java.util.List;
import java.util.Optional;

/**
 * 模型模板仓储接口
 */
public interface ModelTemplateRepository {

    /**
     * 保存模板
     *
     * @param template 模板实体
     * @return 保存后的模板(含ID)
     */
    ModelTemplate save(ModelTemplate template);

    /**
     * 根据ID查询模板
     *
     * @param id 模板ID
     * @return 模板实体
     */
    Optional<ModelTemplate> findById(Long id);

    /**
     * 根据提供商代码和模板代码查询
     *
     * @param providerCode 提供商代码
     * @param code 模板代码
     * @return 模板实体
     */
    Optional<ModelTemplate> findByProviderAndCode(String providerCode, String code);

    /**
     * 按条件查询模板列表
     *
     * @param providerCode 提供商代码(可为null)
     * @param type 模型类型(可为null)
     * @param source 模板来源(可为null)
     * @return 模板列表
     */
    List<ModelTemplate> findByConditions(String providerCode, String type, TemplateSource source);

    /**
     * 查询所有模板
     *
     * @return 所有模板列表
     */
    List<ModelTemplate> findAll();

    /**
     * 根据ID删除模板
     *
     * @param id 模板ID
     */
    void deleteById(Long id);

    /**
     * 批量保存模板
     *
     * @param templates 模板列表
     * @return 插入的记录数
     */
    int batchSave(List<ModelTemplate> templates);

    /**
     * 更新模板
     *
     * @param template 模板实体
     * @return 更新后的模板
     */
    ModelTemplate update(ModelTemplate template);
}
