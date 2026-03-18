package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量统计信息
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量统计信息")
public class EmbeddingStats {

    @Schema(description = "维度数")
    private Integer dimensions;

    @Schema(description = "最小值")
    private Double min;

    @Schema(description = "最大值")
    private Double max;

    @Schema(description = "均值")
    private Double mean;

    @Schema(description = "标准差")
    private Double stdDev;
}
