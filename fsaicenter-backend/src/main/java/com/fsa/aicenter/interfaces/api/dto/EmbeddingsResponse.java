package com.fsa.aicenter.interfaces.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embeddings响应结果
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingsResponse {
    /**
     * 对象类型（固定值："list"）
     */
    private String object;

    /**
     * 向量结果列表
     */
    private List<Embedding> data;

    /**
     * 使用的模型代码
     */
    private String model;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;
}
