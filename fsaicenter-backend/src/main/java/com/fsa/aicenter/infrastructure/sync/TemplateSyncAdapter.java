package com.fsa.aicenter.infrastructure.sync;

import com.fsa.aicenter.domain.model.entity.ModelTemplate;

import java.util.List;

/**
 * 模板同步适配器接口
 * 定义从外部数据源同步模型模板的规范
 *
 * @author FSA AI Center
 */
public interface TemplateSyncAdapter {

    /**
     * 获取数据源名称
     *
     * @return 数据源名称，如 "litellm", "volcengine"
     */
    String getSourceName();

    /**
     * 获取数据源显示名称
     *
     * @return 显示名称，如 "LiteLLM 模型库"
     */
    String getDisplayName();

    /**
     * 从数据源获取模型模板列表
     *
     * @return 模型模板列表
     * @throws TemplateSyncException 同步异常
     */
    List<ModelTemplate> fetchTemplates() throws TemplateSyncException;

    /**
     * 检查数据源是否可用
     *
     * @return true 如果可用
     */
    boolean isAvailable();

    /**
     * 获取支持的提供商代码列表
     * 用于过滤只同步特定提供商的模板
     *
     * @return 提供商代码列表，null 表示支持所有
     */
    default List<String> getSupportedProviderCodes() {
        return null;
    }
}
