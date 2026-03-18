package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求日志持久化对象
 */
@Data
@TableName("request_log")
public class RequestLogPO {

    /**
     * 主键ID
     * 注意：request_log是分区表，PRIMARY KEY包含(id, created_time)，
     * 但MyBatis-Plus只需要映射id字段
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("model_id")
    private Long modelId;

    /**
     * 请求类型：CHAT, EMBEDDING, IMAGE, AUDIO
     */
    @TableField("request_type")
    private String requestType;

    @TableField("is_stream")
    private Boolean isStream;

    /**
     * 输入Token数
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /**
     * 输出Token数
     */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /**
     * 总Token数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("request_ip")
    private String requestIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("http_status")
    private Integer httpStatus;

    /**
     * 响应时间（毫秒）
     */
    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField("error_message")
    private String errorMessage;

    /**
     * 状态：1=成功，0=失败
     */
    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
