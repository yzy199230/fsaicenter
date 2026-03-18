package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 向量嵌入请求参数
 * <p>
 * 参考OpenAI Embedding API规范
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {
    /**
     * 模型标识（如 text-embedding-ada-002, text-embedding-3-small）
     */
    @NotBlank(message = "模型标识不能为空")
    private String model;

    /**
     * 输入文本或文本列表
     * <p>支持单个字符串或字符串列表</p>
     */
    @NotNull(message = "输入内容不能为空")
    private List<String> input;

    /**
     * 编码格式
     * <p>
     * 可选值：
     * <ul>
     *   <li>float: 返回浮点数数组（默认）</li>
     *   <li>base64: 返回Base64编码的向量</li>
     * </ul>
     * </p>
     */
    private String encodingFormat;

    /**
     * 输出向量维度
     * <p>仅支持部分模型，如text-embedding-3系列</p>
     */
    private Integer dimensions;

    /**
     * 用户标识
     * <p>用于追踪和滥用检测</p>
     */
    private String user;
}