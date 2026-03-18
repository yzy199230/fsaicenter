package com.fsa.aicenter.infrastructure.adapter.common;

import com.fsa.aicenter.domain.model.entity.Provider;

import java.util.List;

/**
 * 模型发现适配器接口
 * <p>
 * 用于从AI提供商API自动发现可用的模型列表。
 * 不同提供商的模型发现API不同：
 * <ul>
 *   <li>OpenAI兼容: GET /models</li>
 *   <li>Ollama: GET /api/tags</li>
 *   <li>其他厂商可能没有此API</li>
 * </ul>
 */
public interface ModelDiscoveryAdapter {

    /**
     * 获取此适配器支持的协议类型
     *
     * @return 协议类型列表
     */
    List<String> getSupportedProtocols();

    /**
     * 是否支持模型发现
     *
     * @param provider 提供商
     * @return true 表示支持
     */
    default boolean supportsDiscovery(Provider provider) {
        return getSupportedProtocols().contains(provider.getProtocolType());
    }

    /**
     * 发现模型列表
     *
     * @param provider 提供商配置
     * @param apiKey   API密钥（可选）
     * @return 发现的模型列表
     */
    List<DiscoveredModel> discoverModels(Provider provider, String apiKey);

    /**
     * 发现的模型信息
     */
    record DiscoveredModel(
            String id,
            String name,
            String ownedBy,
            Long createdAt,
            String type,
            Object extra
    ) {
        public static DiscoveredModel of(String id, String name) {
            return new DiscoveredModel(id, name, null, null, null, null);
        }

        public static DiscoveredModel of(String id, String name, String ownedBy) {
            return new DiscoveredModel(id, name, ownedBy, null, null, null);
        }
    }
}
