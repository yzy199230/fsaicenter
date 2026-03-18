package com.fsa.aicenter.interfaces.api.dto;

import com.fsa.aicenter.infrastructure.adapter.common.Message;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat Completion请求参数
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    /**
     * 模型代码（可选，指定具体模型）
     * <p>如果不指定，则根据modelType选择</p>
     */
    private String model;

    /**
     * 模型类型（必填）
     * <p>chat/embedding/image等</p>
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    /**
     * 对话消息列表
     */
    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    /**
     * 是否流式响应
     * <p>默认false</p>
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

    /**
     * 获取stream值，默认为false
     */
    public boolean getStream() {
        return stream != null && stream;
    }
}
