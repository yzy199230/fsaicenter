package com.fsa.aicenter.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志持久化对象
 *
 * @author FSA AI Center
 */
@Data
@TableName("admin_operation_log")
public class AdminOperationLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;

    @TableField("operation_type")
    private String operationType;

    @TableField("operation_desc")
    private String operationDesc;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_params")
    private String requestParams;

    @TableField("request_ip")
    private String requestIp;

    private String userAgent;

    @TableField("response_status")
    private Integer responseStatus;

    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField("error_message")
    private String errorMessage;

    private LocalDateTime createdTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
