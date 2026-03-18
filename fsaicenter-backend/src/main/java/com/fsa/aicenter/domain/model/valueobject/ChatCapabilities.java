package com.fsa.aicenter.domain.model.valueobject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CHAT 文本生成模型能力配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCapabilities {
    /** 上下文窗口大小，如 128000 */
    private Integer contextWindow;
    /** 最大输出 Token，如 16384 */
    private Integer maxOutputTokens;
    /** 支持流式输出 */
    private Boolean supportStream;
    /** 支持图片输入（多模态） */
    private Boolean supportVision;
    /** 支持函数调用 */
    private Boolean supportFunctionCall;
}
