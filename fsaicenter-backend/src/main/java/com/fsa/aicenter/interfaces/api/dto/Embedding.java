package com.fsa.aicenter.interfaces.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个文本的向量结果
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Embedding {
    /**
     * 索引位置
     * <p>对应输入数组中的位置</p>
     */
    private Integer index;

    /**
     * 向量数据（浮点数数组）
     * <p>当encodingFormat为float时使用</p>
     */
    private List<Double> embedding;

    /**
     * 向量数据（Base64编码字符串）
     * <p>当encodingFormat为base64时使用</p>
     */
    private String embeddingBase64;

    /**
     * 对象类型（固定值："embedding"）
     */
    private String object;

    /**
     * 创建浮点数向量结果
     */
    public static Embedding ofFloat(Integer index, List<Double> embedding) {
        return Embedding.builder()
                .index(index)
                .embedding(embedding)
                .object("embedding")
                .build();
    }

    /**
     * 创建Base64向量结果
     */
    public static Embedding ofBase64(Integer index, String embeddingBase64) {
        return Embedding.builder()
                .index(index)
                .embeddingBase64(embeddingBase64)
                .object("embedding")
                .build();
    }
}
