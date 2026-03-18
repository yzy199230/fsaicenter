package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * IMAGE_RECOGNITION 视觉分析模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageRecognitionCapabilities {
    /** 上下文窗口大小 */
    private Integer contextWindow;
    /** 最大输出 Token */
    private Integer maxOutputTokens;
    /** 单次最大图片数 */
    private Integer maxImages;
    /** 最大图片大小（MB） */
    private Integer maxImageSizeMB;
    /** 支持的图片格式 */
    private List<String> supportedFormats;
    /** 详细程度选项 */
    private List<String> supportedDetails;
    /** 支持流式输出 */
    private Boolean supportStream;
}
