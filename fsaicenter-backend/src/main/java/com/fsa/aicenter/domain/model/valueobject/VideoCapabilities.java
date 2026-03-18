package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * VIDEO 视频生成模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCapabilities {
    /** 最大视频时长（秒） */
    private Integer maxDurationSeconds;
    /** 最小视频时长（秒） */
    private Integer minDurationSeconds;
    /** 支持的分辨率列表 */
    private List<String> supportedResolutions;
    /** 支持的宽高比列表 */
    private List<String> supportedAspectRatios;
    /** 输出格式列表 */
    private List<String> supportedFormats;
    /** 最大提示词长度 */
    private Integer maxPromptLength;
    /** 支持图生视频 */
    private Boolean supportImageToVideo;
    /** 支持视频延长/续写 */
    private Boolean supportVideoExtend;
    /** 支持视频风格转换 */
    private Boolean supportVideoToVideo;
    /** 风格选项 */
    private List<String> supportedStyles;
    /** 最大帧率 */
    private Integer maxFps;
    /** 支持音频生成 */
    private Boolean supportAudio;
}
