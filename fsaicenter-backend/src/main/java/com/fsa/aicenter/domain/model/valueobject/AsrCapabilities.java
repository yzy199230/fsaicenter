package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * ASR 语音识别模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsrCapabilities {
    /** 支持的语言列表 */
    private List<String> supportedLanguages;
    /** 支持的音频输入格式 */
    private List<String> supportedInputFormats;
    /** 支持的输出格式 */
    private List<String> supportedOutputFormats;
    /** 最大音频时长（秒） */
    private Integer maxDurationSeconds;
    /** 最大文件大小（MB） */
    private Integer maxFileSizeMB;
    /** 支持实时识别（边录边识别） */
    private Boolean supportRealtime;
    /** 支持流式输出（识别结果逐步返回） */
    private Boolean supportStream;
    /** 支持时间戳 */
    private Boolean supportTimestamp;
}
