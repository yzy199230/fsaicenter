package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * EMBEDDING 向量嵌入模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingCapabilities {
    /** 默认维度 */
    private Integer defaultDimensions;
    /** 最大维度 */
    private Integer maxDimensions;
    /** 最小维度 */
    private Integer minDimensions;
    /** 最大输入 Token */
    private Integer maxInputTokens;
    /** 支持的编码格式 */
    private List<String> supportedEncodings;
}
