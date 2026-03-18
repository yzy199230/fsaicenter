package com.fsa.aicenter.domain.admin.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志聚合根
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * IP归属地
     */
    private String location;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 执行时长(ms)
     */
    private Integer executeTime;

    /**
     * 状态(1:成功 0:失败)
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 状态枚举
     */
    public enum Status {
        FAILED(0, "失败"),
        SUCCESS(1, "成功");

        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 判断操作是否成功
     */
    public boolean isSuccess() {
        return this.status != null && Status.SUCCESS.getCode() == this.status;
    }
}
