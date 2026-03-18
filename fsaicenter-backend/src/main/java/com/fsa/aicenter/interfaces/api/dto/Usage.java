package com.fsa.aicenter.interfaces.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用情况
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usage {
    /**
     * 提示词消耗的token数
     */
    private Integer promptTokens;

    /**
     * 生成内容消耗的token数
     */
    private Integer completionTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;
}
