package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型测试请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模型测试请求")
public class TestModelRequest {

    @Schema(description = "文本输入（CHAT、EMBEDDING、IMAGE文生图、VIDEO、TTS）", example = "你好，请介绍一下自己")
    private String text;

    @Schema(description = "图片输入（IMAGE图生图、IMAGE_RECOGNITION）")
    private String image;

    @Schema(description = "音频输入（ASR语音识别，Base64或URL）")
    private String audio;

    @Schema(description = "音频格式（ASR，如pcm、wav、mp3，实时ASR模型默认pcm）", example = "pcm")
    private String audioFormat;

    @Schema(description = "提示词（IMAGE图生图）")
    private String prompt;

    @Schema(description = "是否流式（CHAT）")
    private Boolean stream;

    @Schema(description = "温度参数")
    private Double temperature;

    @Schema(description = "最大token数")
    private Integer maxTokens;

    @Schema(description = "语音类型（TTS）", example = "alloy")
    private String voice;

    @Schema(description = "语速（TTS，0.25-4.0）", example = "1.0")
    private Double speed;

    @Schema(description = "图像生成模式（IMAGE：text2image文生图、image2image图生图）", example = "text2image")
    private String mode;

    @Schema(description = "图片URL（IMAGE图生图参考图）")
    private String imageUrl;
}
