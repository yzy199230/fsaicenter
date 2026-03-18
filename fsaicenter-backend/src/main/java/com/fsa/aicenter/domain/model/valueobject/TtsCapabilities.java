package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * TTS 语音合成模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsCapabilities {
    /** 支持的声音列表 */
    private List<String> supportedVoices;
    /** 最小语速，如 0.25 */
    private Double minSpeed;
    /** 最大语速，如 4.0 */
    private Double maxSpeed;
    /** 输出格式列表 */
    private List<String> supportedFormats;
    /** 最大输入字符数 */
    private Integer maxInputCharacters;
}
