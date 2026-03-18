package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求日志详情持久化对象
 */
@Data
@TableName("request_log_detail")
public class RequestLogDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 请求ID（唯一约束）
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 请求体（JSONB格式字符串）
     */
    @TableField("request_body")
    private String requestBody;

    /**
     * 响应体（JSONB格式字符串）
     */
    @TableField("response_body")
    private String responseBody;

    /**
     * 请求头（JSONB格式字符串）
     */
    @TableField("request_headers")
    private String requestHeaders;

    @TableField("created_time")
    private LocalDateTime createdTime;
}
