package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * IMAGE 图像生成模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageCapabilities {
    /** 最大生成数量，如 4 */
    private Integer maxN;
    /** 支持的尺寸列表 */
    private List<String> supportedSizes;
    /** 质量选项 */
    private List<String> supportedQualities;
    /** 风格选项 */
    private List<String> supportedStyles;
    /** 最大提示词长度 */
    private Integer maxPromptLength;
    /** 支持图生图 */
    private Boolean supportImageToImage;
    /** 支持编辑 */
    private Boolean supportEdit;
}
