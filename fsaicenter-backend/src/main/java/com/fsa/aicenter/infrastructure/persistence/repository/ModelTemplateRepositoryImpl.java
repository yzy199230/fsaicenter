package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.repository.ModelTemplateRepository;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelTemplatePO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型模板仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ModelTemplateRepositoryImpl implements ModelTemplateRepository {

    private final ModelTemplateMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ModelTemplate save(ModelTemplate template) {
        ModelTemplatePO po = toPO(template);
        mapper.insert(po);
        template.setId(po.getId());
        return template;
    }

    @Override
    public Optional<ModelTemplate> findById(Long id) {
        ModelTemplatePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<ModelTemplate> findByProviderAndCode(String providerCode, String code) {
        ModelTemplatePO po = mapper.selectByProviderAndCode(providerCode, code);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<ModelTemplate> findByConditions(String providerCode, String type, TemplateSource source) {
        String sourceCode = source != null ? source.getCode() : null;
        List<ModelTemplatePO> poList = mapper.selectByConditions(providerCode, type, sourceCode);
        return poList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelTemplate> findAll() {
        List<ModelTemplatePO> poList = mapper.selectList(null);
        return poList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ModelTemplate update(ModelTemplate template) {
        ModelTemplatePO po = toPO(template);
        mapper.updateById(po);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchSave(List<ModelTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ModelTemplate template : templates) {
            // 检查是否已存在
            ModelTemplatePO existing = mapper.selectByProviderAndCode(
                    template.getProviderCode(), template.getCode());

            ModelTemplatePO po = toPO(template);

            if (existing != null) {
                // 已存在，更新
                po.setId(existing.getId());
                po.setCreatedTime(existing.getCreatedTime());
                mapper.updateById(po);
                template.setId(existing.getId());
                log.debug("更新模板: provider={}, code={}", template.getProviderCode(), template.getCode());
            } else {
                // 不存在，插入
                mapper.insert(po);
                template.setId(po.getId());
                log.debug("新增模板: provider={}, code={}", template.getProviderCode(), template.getCode());
            }
            count++;
        }
        return count;
    }

    // ========== 转换方法 ==========

    /**
     * PO转领域对象
     */
    private ModelTemplate toEntity(ModelTemplatePO po) {
        // 验证必需的枚举字段
        if (po.getType() == null || po.getType().trim().isEmpty()) {
            log.error("Invalid model type for template {}: null or empty", po.getId());
            throw new IllegalStateException("Model type cannot be null for template: " + po.getId());
        }
        if (po.getSource() == null || po.getSource().trim().isEmpty()) {
            log.error("Invalid source for template {}: null or empty", po.getId());
            throw new IllegalStateException("Template source cannot be null for template: " + po.getId());
        }

        ModelTemplate template = new ModelTemplate();
        template.setId(po.getId());
        template.setCode(po.getCode());
        template.setName(po.getName());
        template.setType(ModelType.fromCode(po.getType()));
        template.setProviderCode(po.getProviderCode());
        template.setSupportStream(po.getSupportStream());
        template.setMaxTokenLimit(po.getMaxTokenLimit());
        template.setDescription(po.getDescription());
        template.setCapabilities(po.getCapabilities());
        template.setDefaultConfig(po.getDefaultConfig());
        template.setTags(po.getTags());
        template.setDeprecated(po.getDeprecated());
        template.setReleaseDate(po.getReleaseDate());
        template.setSource(TemplateSource.fromCode(po.getSource()));
        template.setCreatedTime(po.getCreatedTime());
        template.setUpdatedTime(po.getUpdatedTime());
        return template;
    }

    /**
     * 领域对象转PO
     */
    private ModelTemplatePO toPO(ModelTemplate template) {
        ModelTemplatePO po = new ModelTemplatePO();
        po.setId(template.getId());
        po.setCode(template.getCode());
        po.setName(template.getName());
        po.setType(template.getType() != null ? template.getType().getCode() : null);
        po.setProviderCode(template.getProviderCode());
        po.setSupportStream(template.getSupportStream());
        po.setMaxTokenLimit(template.getMaxTokenLimit());
        po.setDescription(template.getDescription());
        po.setCapabilities(template.getCapabilities());
        po.setDefaultConfig(template.getDefaultConfig());
        po.setTags(template.getTags());
        po.setDeprecated(template.getDeprecated());
        po.setReleaseDate(template.getReleaseDate());
        po.setSource(template.getSource() != null ? template.getSource().getCode() : null);
        po.setCreatedTime(template.getCreatedTime());
        po.setUpdatedTime(template.getUpdatedTime());
        return po;
    }
}
