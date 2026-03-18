package com.fsa.aicenter.application.service;

import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import com.fsa.aicenter.domain.model.repository.ModelTemplateRepository;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import com.fsa.aicenter.infrastructure.sync.TemplateSyncAdapter;
import com.fsa.aicenter.infrastructure.sync.TemplateSyncException;
import com.fsa.aicenter.infrastructure.sync.TemplateSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型模板同步服务
 * 负责从外部数据源同步模型模板，执行智能合并策略
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
public class ModelTemplateSyncService {

    private final ModelTemplateRepository templateRepository;
    private final Map<String, TemplateSyncAdapter> adapterMap;

    public ModelTemplateSyncService(
            ModelTemplateRepository templateRepository,
            List<TemplateSyncAdapter> adapters) {
        this.templateRepository = templateRepository;
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        TemplateSyncAdapter::getSourceName,
                        adapter -> adapter
                ));
        log.info("已注册 {} 个模板同步适配器: {}",
                adapterMap.size(), adapterMap.keySet());
    }

    /**
     * 获取所有可用的数据源
     *
     * @return 数据源信息列表
     */
    public List<Map<String, Object>> listAvailableSources() {
        return adapterMap.values().stream()
                .map(adapter -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", adapter.getSourceName());
                    info.put("displayName", adapter.getDisplayName());
                    info.put("available", adapter.isAvailable());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 从指定数据源同步模板
     *
     * @param sourceName 数据源名称
     * @param dryRun     是否为试运行（不实际写入数据库）
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateSyncResult syncFromSource(String sourceName, boolean dryRun) {
        log.info("开始同步模板: source={}, dryRun={}", sourceName, dryRun);

        TemplateSyncAdapter adapter = adapterMap.get(sourceName);
        if (adapter == null) {
            return TemplateSyncResult.failure(sourceName, "未知的数据源: " + sourceName);
        }

        TemplateSyncResult result = TemplateSyncResult.success(sourceName);
        result.setStartTime(LocalDateTime.now());

        try {
            // 1. 从数据源获取模板
            List<ModelTemplate> remoteTemplates = adapter.fetchTemplates();
            log.info("从 {} 获取到 {} 个模板", sourceName, remoteTemplates.size());

            if (remoteTemplates.isEmpty()) {
                result.setEndTime(LocalDateTime.now());
                return result;
            }

            // 2. 获取本地已有模板
            List<ModelTemplate> localTemplates = templateRepository.findAll();
            Map<String, ModelTemplate> localMap = localTemplates.stream()
                    .filter(t -> t.getDeduplicationKey() != null)
                    .collect(Collectors.toMap(
                            ModelTemplate::getDeduplicationKey,
                            t -> t,
                            (existing, replacement) -> existing
                    ));

            log.debug("本地已有 {} 个模板", localMap.size());

            // 3. 执行智能合并
            List<ModelTemplate> toInsert = new ArrayList<>();
            List<ModelTemplate> toUpdate = new ArrayList<>();

            for (ModelTemplate remote : remoteTemplates) {
                String key = remote.getDeduplicationKey();
                if (key == null) {
                    continue;
                }

                ModelTemplate local = localMap.get(key);

                if (local == null) {
                    // 新模板，直接添加
                    toInsert.add(remote);
                    result.setAddedCount(result.getAddedCount() + 1);
                } else if (local.getSource() == TemplateSource.USER) {
                    // 用户自定义模板，跳过不覆盖
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    log.debug("跳过用户自定义模板: {}", key);
                } else {
                    // SYSTEM 或 BUILTIN 模板，执行智能更新
                    if (shouldUpdate(local, remote)) {
                        mergeTemplate(local, remote);
                        toUpdate.add(local);
                        result.setUpdatedCount(result.getUpdatedCount() + 1);
                    } else {
                        result.setUnchangedCount(result.getUnchangedCount() + 1);
                    }
                }
            }

            log.info("同步分析完成: 新增={}, 更新={}, 跳过={}, 未变={}",
                    result.getAddedCount(), result.getUpdatedCount(),
                    result.getSkippedCount(), result.getUnchangedCount());

            // 4. 写入数据库（非试运行）
            if (!dryRun) {
                if (!toInsert.isEmpty()) {
                    templateRepository.batchSave(toInsert);
                    log.info("批量插入 {} 个新模板", toInsert.size());
                }

                if (!toUpdate.isEmpty()) {
                    for (ModelTemplate template : toUpdate) {
                        templateRepository.update(template);
                    }
                    log.info("更新 {} 个模板", toUpdate.size());
                }
            } else {
                log.info("试运行模式，未写入数据库");
            }

            result.setSuccess(true);
        } catch (TemplateSyncException e) {
            log.error("同步失败: {}", e.getMessage());
            result.setSuccess(false);
            result.addError(e.getMessage());
        } catch (Exception e) {
            log.error("同步发生未知错误", e);
            result.setSuccess(false);
            result.addError("同步失败: " + e.getMessage());
        }

        result.setEndTime(LocalDateTime.now());
        log.info("同步完成: source={}, success={}, duration={}ms",
                sourceName, result.isSuccess(), result.getDurationMs());

        return result;
    }

    /**
     * 判断是否需要更新
     * 比较关键字段是否有变化
     */
    private boolean shouldUpdate(ModelTemplate local, ModelTemplate remote) {
        // 比较能力配置
        if (!Objects.equals(local.getCapabilities(), remote.getCapabilities())) {
            return true;
        }

        // 比较最大 Token 限制
        if (!Objects.equals(local.getMaxTokenLimit(), remote.getMaxTokenLimit())) {
            return true;
        }

        // 比较流式支持
        if (!Objects.equals(local.getSupportStream(), remote.getSupportStream())) {
            return true;
        }

        return false;
    }

    /**
     * 智能合并模板
     * 更新元数据字段，保留用户可能修改的字段
     */
    private void mergeTemplate(ModelTemplate local, ModelTemplate remote) {
        // 更新能力配置（合并而非覆盖）
        if (remote.getCapabilities() != null) {
            Map<String, Object> mergedCapabilities = new HashMap<>();
            if (local.getCapabilities() != null) {
                mergedCapabilities.putAll(local.getCapabilities());
            }
            mergedCapabilities.putAll(remote.getCapabilities());
            local.setCapabilities(mergedCapabilities);
        }

        // 更新最大 Token 限制
        if (remote.getMaxTokenLimit() != null) {
            local.setMaxTokenLimit(remote.getMaxTokenLimit());
        }

        // 更新流式支持
        if (remote.getSupportStream() != null) {
            local.setSupportStream(remote.getSupportStream());
        }

        // 保留本地的 name, description, tags（如果用户修改过）
        // 只有在本地为空时才使用远程的值
        if (local.getName() == null || local.getName().isEmpty()) {
            local.setName(remote.getName());
        }
        if (local.getDescription() == null || local.getDescription().isEmpty()) {
            local.setDescription(remote.getDescription());
        }

        local.setUpdatedTime(LocalDateTime.now());
    }

    /**
     * 检查数据源是否可用
     */
    public boolean isSourceAvailable(String sourceName) {
        TemplateSyncAdapter adapter = adapterMap.get(sourceName);
        return adapter != null && adapter.isAvailable();
    }
}
