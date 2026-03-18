package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateModelFromTemplateRequest;
import com.fsa.aicenter.application.dto.request.SaveUserTemplateRequest;
import com.fsa.aicenter.application.dto.response.ModelTemplateResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.repository.ModelTemplateRepository;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import com.fsa.aicenter.infrastructure.template.BuiltinTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型模板服务
 * 提供模板管理和从模板创建模型的功能
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelTemplateService {

    private final ModelTemplateRepository templateRepository;
    private final ModelRepository modelRepository;
    private final ProviderRepository providerRepository;
    private final BuiltinTemplateLoader builtinTemplateLoader;

    /**
     * 获取所有可用模板（合并内置+数据库）
     * 优先级：USER > SYSTEM > BUILTIN
     *
     * @param providerCode 提供商代码(可选)
     * @param type         模型类型(可选)
     * @param keyword      关键词搜索(可选)
     * @return 模板列表
     */
    public List<ModelTemplateResponse> listAllTemplates(String providerCode, String type, String keyword) {
        log.debug("查询模板列表: providerCode={}, type={}, keyword={}", providerCode, type, keyword);

        // 1. 从数据库查询模板
        List<ModelTemplate> dbTemplates = templateRepository.findByConditions(providerCode, type, null);
        log.debug("数据库查询到 {} 个模板", dbTemplates.size());

        // 2. 加载内置模板
        List<ModelTemplate> builtinTemplates = builtinTemplateLoader.getBuiltinTemplates();
        log.debug("内置模板 {} 个", builtinTemplates.size());

        // 3. 按提供商和类型过滤内置模板
        List<ModelTemplate> filteredBuiltin = builtinTemplates.stream()
                .filter(t -> providerCode == null || providerCode.equals(t.getProviderCode()))
                .filter(t -> type == null || type.equalsIgnoreCase(t.getType().getCode()))
                .collect(Collectors.toList());
        log.debug("过滤后内置模板 {} 个", filteredBuiltin.size());

        // 4. 合并去重（使用LinkedHashMap保持顺序，key = providerCode:code）
        Map<String, ModelTemplate> mergedMap = new LinkedHashMap<>();

        // 先放入内置模板（优先级最低）
        for (ModelTemplate template : filteredBuiltin) {
            String key = template.getDeduplicationKey();
            if (key != null) {
                mergedMap.put(key, template);
            }
        }

        // 再放入数据库模板（SYSTEM和USER优先级更高，会覆盖同key的BUILTIN）
        for (ModelTemplate template : dbTemplates) {
            String key = template.getDeduplicationKey();
            if (key != null) {
                mergedMap.put(key, template);
            }
        }

        List<ModelTemplate> mergedTemplates = new ArrayList<>(mergedMap.values());
        log.debug("合并去重后 {} 个模板", mergedTemplates.size());

        // 5. 关键词过滤
        List<ModelTemplate> result = mergedTemplates.stream()
                .filter(t -> keyword == null || matchesKeyword(t, keyword))
                .collect(Collectors.toList());
        log.debug("关键词过滤后 {} 个模板", result.size());

        // 6. 转换为响应DTO
        return result.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据模板批量创建模型
     *
     * @param request 创建请求
     * @return 成功创建的模型数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int createModelsFromTemplates(CreateModelFromTemplateRequest request) {
        log.info("开始从模板创建模型: providerId={}, templateCodes={}",
                request.getProviderId(), request.getTemplateCodes());

        // 1. 校验providerId存在
        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        int successCount = 0;

        // 2. 遍历templateCodes
        for (String templateCode : request.getTemplateCodes()) {
            try {
                // 3. 查找模板（先数据库后内置）
                ModelTemplate template = findTemplate(provider.getCode(), templateCode);

                // 4. 检查模型是否已存在
                if (modelRepository.existsByProviderIdAndCode(request.getProviderId(), template.getCode())) {
                    log.warn("模型已存在，跳过: providerId={}, code={}", request.getProviderId(), template.getCode());
                    continue;
                }

                // 5. 创建AiModel并保存
                AiModel model = new AiModel();
                model.setCode(template.getCode());
                model.setName(template.getName());
                model.setType(template.getType());
                model.setProviderId(request.getProviderId());
                model.setSupportStream(template.getSupportStream());
                model.setMaxTokenLimit(template.getMaxTokenLimit());
                model.setDescription(template.getDescription());
                model.setCapabilities(template.getCapabilities());
                model.setStatus(EntityStatus.ENABLED);
                model.setCreatedTime(LocalDateTime.now());
                model.setUpdatedTime(LocalDateTime.now());

                modelRepository.save(model);
                successCount++;

                log.info("从模板创建模型成功: code={}, name={}", model.getCode(), model.getName());
            } catch (BusinessException e) {
                log.error("从模板创建模型失败: templateCode={}, error={}", templateCode, e.getMessage());
                // 继续处理下一个模板
            }
        }

        log.info("批量创建模型完成: 成功={}, 总数={}", successCount, request.getTemplateCodes().size());
        return successCount;
    }

    /**
     * 保存用户自定义模板
     *
     * @param request 保存请求
     * @return 保存后的模板
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelTemplate saveUserTemplate(SaveUserTemplateRequest request) {
        log.info("保存用户自定义模板: code={}, name={}", request.getCode(), request.getName());

        // 检查是否已存在同providerCode+code的模板
        Optional<ModelTemplate> existing = templateRepository.findByProviderAndCode(
                request.getProviderCode(), request.getCode());

        ModelTemplate template;
        if (existing.isPresent()) {
            template = existing.get();

            // 只有USER类型的模板才可编辑
            if (!template.isUserTemplate()) {
                throw new BusinessException(ErrorCode.TEMPLATE_NOT_EDITABLE,
                        "只有用户自定义模板可以编辑");
            }

            // 更新现有模板
            template.setName(request.getName());
            template.setType(ModelType.fromCode(request.getType()));
            template.setSupportStream(request.getSupportStream());
            template.setMaxTokenLimit(request.getMaxTokenLimit());
            template.setDescription(request.getDescription());
            template.setCapabilities(request.getCapabilities());
            template.setDefaultConfig(request.getDefaultConfig());
            template.setTags(request.getTags());
            template.setDeprecated(request.getDeprecated());
            template.setReleaseDate(request.getReleaseDate());
            template.setUpdatedTime(LocalDateTime.now());

            log.info("更新用户模板: id={}", template.getId());
        } else {
            // 创建新模板
            template = new ModelTemplate();
            template.setCode(request.getCode());
            template.setName(request.getName());
            template.setType(ModelType.fromCode(request.getType()));
            template.setProviderCode(request.getProviderCode());
            template.setSupportStream(request.getSupportStream());
            template.setMaxTokenLimit(request.getMaxTokenLimit());
            template.setDescription(request.getDescription());
            template.setCapabilities(request.getCapabilities());
            template.setDefaultConfig(request.getDefaultConfig());
            template.setTags(request.getTags());
            template.setDeprecated(request.getDeprecated());
            template.setReleaseDate(request.getReleaseDate());
            template.setSource(TemplateSource.USER);
            template.setCreatedTime(LocalDateTime.now());
            template.setUpdatedTime(LocalDateTime.now());

            log.info("创建新用户模板");
        }

        ModelTemplate saved = templateRepository.save(template);
        log.info("用户模板保存成功: id={}", saved.getId());
        return saved;
    }

    /**
     * 删除用户模板
     *
     * @param id 模板ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserTemplate(Long id) {
        log.info("删除用户模板: id={}", id);

        ModelTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

        // 只有USER类型的模板才可删除
        if (!template.isUserTemplate()) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_DELETABLE,
                    "只有用户自定义模板可以删除");
        }

        templateRepository.deleteById(id);
        log.info("用户模板删除成功: id={}", id);
    }

    /**
     * 导入内置模板到数据库
     * 将所有内置模板以SYSTEM类型保存到数据库
     *
     * @return 导入的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int importBuiltinTemplatesToDatabase() {
        log.info("开始导入内置模板到数据库");

        List<ModelTemplate> builtinTemplates = builtinTemplateLoader.getBuiltinTemplates();
        log.info("加载到 {} 个内置模板", builtinTemplates.size());

        // 查询数据库中已存在的SYSTEM模板
        List<ModelTemplate> existingSystemTemplates = templateRepository.findByConditions(
                null, null, TemplateSource.SYSTEM);

        Set<String> existingKeys = existingSystemTemplates.stream()
                .map(ModelTemplate::getDeduplicationKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("数据库已存在 {} 个SYSTEM模板", existingKeys.size());

        // 过滤出需要导入的模板（排除已存在的）
        List<ModelTemplate> templatesToImport = builtinTemplates.stream()
                .filter(t -> !existingKeys.contains(t.getDeduplicationKey()))
                .map(this::convertToSystemTemplate)
                .collect(Collectors.toList());

        log.info("需要导入 {} 个新模板", templatesToImport.size());

        if (templatesToImport.isEmpty()) {
            log.info("没有需要导入的新模板");
            return 0;
        }

        // 批量保存
        int importedCount = templateRepository.batchSave(templatesToImport);
        log.info("内置模板导入完成: 成功导入 {} 个", importedCount);

        return importedCount;
    }

    // ========== 私有方法 ==========

    /**
     * 关键词匹配
     *
     * @param template 模板
     * @param keyword  关键词
     * @return 是否匹配
     */
    private boolean matchesKeyword(ModelTemplate template, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();
        return (template.getCode() != null && template.getCode().toLowerCase().contains(lowerKeyword))
                || (template.getName() != null && template.getName().toLowerCase().contains(lowerKeyword))
                || (template.getDescription() != null && template.getDescription().toLowerCase().contains(lowerKeyword))
                || (template.getTags() != null && template.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword)));
    }

    /**
     * 转换为响应DTO
     *
     * @param template 模板实体
     * @return 响应DTO
     */
    private ModelTemplateResponse toResponse(ModelTemplate template) {
        ModelTemplateResponse response = new ModelTemplateResponse();
        response.setId(template.getId());
        response.setCode(template.getCode());
        response.setName(template.getName());
        response.setType(template.getType() != null ? template.getType().getCode() : null);
        response.setProviderCode(template.getProviderCode());

        // 获取提供商名称
        String providerName = builtinTemplateLoader.getProviderName(template.getProviderCode());
        response.setProviderName(providerName);

        response.setSupportStream(template.getSupportStream());
        response.setMaxTokenLimit(template.getMaxTokenLimit());
        response.setDescription(template.getDescription());
        response.setCapabilities(template.getCapabilities());
        response.setDefaultConfig(template.getDefaultConfig());
        response.setTags(template.getTags());
        response.setDeprecated(template.getDeprecated());
        response.setReleaseDate(template.getReleaseDate());
        response.setSource(template.getSource() != null ? template.getSource().getCode() : null);

        return response;
    }

    /**
     * 查找模板（先数据库后内置）
     *
     * @param providerCode 提供商代码
     * @param templateCode 模板代码
     * @return 模板实体
     */
    private ModelTemplate findTemplate(String providerCode, String templateCode) {
        // 先从数据库查找
        Optional<ModelTemplate> dbTemplate = templateRepository.findByProviderAndCode(
                providerCode, templateCode);

        if (dbTemplate.isPresent()) {
            return dbTemplate.get();
        }

        // 再从内置模板查找
        ModelTemplate builtinTemplate = builtinTemplateLoader.getBuiltinTemplates().stream()
                .filter(t -> providerCode.equals(t.getProviderCode())
                        && templateCode.equals(t.getCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND,
                        "模板不存在: " + providerCode + ":" + templateCode));

        return builtinTemplate;
    }

    /**
     * 转换内置模板为系统模板
     *
     * @param builtinTemplate 内置模板
     * @return 系统模板
     */
    private ModelTemplate convertToSystemTemplate(ModelTemplate builtinTemplate) {
        ModelTemplate systemTemplate = new ModelTemplate();
        systemTemplate.setCode(builtinTemplate.getCode());
        systemTemplate.setName(builtinTemplate.getName());
        systemTemplate.setType(builtinTemplate.getType());
        systemTemplate.setProviderCode(builtinTemplate.getProviderCode());
        systemTemplate.setSupportStream(builtinTemplate.getSupportStream());
        systemTemplate.setMaxTokenLimit(builtinTemplate.getMaxTokenLimit());
        systemTemplate.setDescription(builtinTemplate.getDescription());
        systemTemplate.setCapabilities(builtinTemplate.getCapabilities());
        systemTemplate.setDefaultConfig(builtinTemplate.getDefaultConfig());
        systemTemplate.setTags(builtinTemplate.getTags());
        systemTemplate.setDeprecated(builtinTemplate.getDeprecated());
        systemTemplate.setReleaseDate(builtinTemplate.getReleaseDate());
        systemTemplate.setSource(TemplateSource.SYSTEM); // 改为SYSTEM来源
        systemTemplate.setCreatedTime(LocalDateTime.now());
        systemTemplate.setUpdatedTime(LocalDateTime.now());

        return systemTemplate;
    }
}
