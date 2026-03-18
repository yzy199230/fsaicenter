package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型测试响应
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型测试响应")
public class TestModelResponse {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "耗时(毫秒)")
    private Long duration;

    @Schema(description = "输入Token数")
    private Integer inputTokens;

    @Schema(description = "输出Token数")
    private Integer outputTokens;

    @Schema(description = "文本内容（CHAT、IMAGE_RECOGNITION）")
    private String content;

    @Schema(description = "向量统计（EMBEDDING）")
    private EmbeddingStats embeddingStats;

    @Schema(description = "图片URL（IMAGE）")
    private List<String> imageUrls;

    @Schema(description = "视频URL（VIDEO）")
    private String videoUrl;

    @Schema(description = "音频URL（AUDIO）")
    private String audioUrl;
}
