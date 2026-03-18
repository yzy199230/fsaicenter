package com.fsa.aicenter.interfaces.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片数据
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {
    /**
     * 图片URL
     * <p>当responseFormat为url时返回</p>
     */
    private String url;

    /**
     * Base64编码的图片数据
     * <p>当responseFormat为b64_json时返回</p>
     */
    private String b64Json;

    /**
     * 修改后的提示词
     * <p>AI可能会优化原始提示词以获得更好的生成效果</p>
     */
    private String revisedPrompt;
}
