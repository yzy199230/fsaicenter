package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 请求趋势数据响应
 */
@Data
@Schema(description = "请求趋势数据")
public class DashboardTrendResponse {

    @Schema(description = "X轴数据(日期)")
    private List<String> xAxis;

    @Schema(description = "数据系列")
    private List<SeriesData> series;

    /**
     * 数据系列
     */
    @Data
    @Schema(description = "数据系列")
    public static class SeriesData {

        @Schema(description = "系列名称")
        private String name;

        @Schema(description = "数据值")
        private List<Long> data;
    }
}
