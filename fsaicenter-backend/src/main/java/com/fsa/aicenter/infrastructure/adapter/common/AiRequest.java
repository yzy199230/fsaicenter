package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * AI请求参数
 * <p>
 * 统一的AI调用请求对象，屏蔽不同提供商的参数差异
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {
    /**
     * 模型标识（如 gpt-4, qwen-turbo）
     */
    @NotBlank(message = "模型标识不能为空")
    private String model;

    /**
     * 对话消息列表
     */
    @NotNull(message = "消息列表不能为空")
    private List<Message> messages;

    /**
     * 是否流式响应
     * <p>默认为false</p>
     */
    private Boolean stream;

    /**
     * 温度参数（0.0-2.0）
     * <p>
     * 控制输出的随机性：
     * <ul>
     *   <li>0.0-0.3: 事实性任务，输出确定</li>
     *   <li>0.7-1.0: 创意性任务，输出多样</li>
     * </ul>
     * </p>
     */
    @DecimalMin(value = "0.0", message = "temperature必须在0.0-2.0之间")
    @DecimalMax(value = "2.0", message = "temperature必须在0.0-2.0之间")
    private Double temperature;

    /**
     * 最大生成token数
     */
    private Integer maxTokens;

    /**
     * Top-p采样参数（0.0-1.0）
     * <p>核采样，控制输出多样性</p>
     */
    @DecimalMin(value = "0.0", message = "topP必须在0.0-1.0之间")
    @DecimalMax(value = "1.0", message = "topP必须在0.0-1.0之间")
    private Double topP;

    /**
     * 频率惩罚（-2.0-2.0）
     * <p>降低重复内容的概率</p>
     */
    @DecimalMin(value = "-2.0", message = "frequencyPenalty必须在-2.0-2.0之间")
    @DecimalMax(value = "2.0", message = "frequencyPenalty必须在-2.0-2.0之间")
    private Double frequencyPenalty;

    /**
     * 存在惩罚（-2.0-2.0）
     * <p>鼓励谈论新话题</p>
     */
    @DecimalMin(value = "-2.0", message = "presencePenalty必须在-2.0-2.0之间")
    @DecimalMax(value = "2.0", message = "presencePenalty必须在-2.0-2.0之间")
    private Double presencePenalty;

    /**
     * 停止序列
     * <p>遇到指定序列时停止生成</p>
     */
    private List<String> stop;

    /**
     * 用户标识
     * <p>用于追踪和滥用检测</p>
     */
    private String user;

    // ==================== 图片生成相关参数 ====================

    /**
     * 图片描述提示词
     * <p>用于Image Generation API</p>
     */
    private String prompt;

    /**
     * 负面提示词
     * <p>描述不希望出现在图片中的内容</p>
     */
    private String negativePrompt;

    /**
     * 生成图片数量
     * <p>默认1，范围1-10</p>
     */
    private Integer n;

    /**
     * 图片尺寸
     * <p>如：256x256, 512x512, 1024x1024</p>
     */
    private String size;

    /**
     * 响应格式
     * <p>url 或 b64_json</p>
     */
    private String responseFormat;

    /**
     * 图片风格
     * <p>vivid（生动）或 natural（自然）</p>
     */
    private String style;

    /**
     * 图片质量
     * <p>standard（标准）或 hd（高清）</p>
     */
    private String quality;

    // ==================== 向量嵌入相关参数 ====================

    /**
     * 向量嵌入的输入文本
     * <p>用于Embedding API</p>
     */
    private String input;

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
     * 图片输入（Base64或URL）
     * <p>用于图生图、图像识别等</p>
     */
    private String image;

    // ==================== 音频相关参数 ====================

    /**
     * 音频输入（Base64或URL）
     * <p>用于语音识别（ASR）</p>
     */
    private String audio;

    /**
     * 音频格式
     * <p>如：mp3, wav, flac等</p>
     */
    private String audioFormat;

    /**
     * 语音合成的声音类型
     * <p>如：alloy, echo, fable, onyx, nova, shimmer</p>
     */
    private String voice;

    /**
     * 语音合成的语速
     * <p>范围0.25-4.0，默认1.0</p>
     */
    private Double speed;

    // ==================== 视频相关参数 ====================

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 视频分辨率
     * <p>如：720p, 1080p</p>
     */
    private String resolution;

    /**
     * 视频宽高比
     * <p>如：16:9, 9:16, 1:1</p>
     */
    private String aspectRatio;
}
