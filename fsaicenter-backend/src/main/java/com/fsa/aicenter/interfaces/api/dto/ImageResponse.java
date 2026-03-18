package com.fsa.aicenter.interfaces.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Image Generation响应结果
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    /**
     * 响应ID
     */
    private String id;

    /**
     * 使用的模型代码
     */
    private String model;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;

    /**
     * 生成的图片数据列表
     */
    private List<ImageData> data;
}
