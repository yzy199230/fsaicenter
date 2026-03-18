package com.fsa.aicenter.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    private String operation;
    private String method;
    private String params;
    private String ip;
    private String location;
    private String userAgent;
    private Integer executeTime;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createdTime;
}
