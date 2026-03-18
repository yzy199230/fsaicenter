package com.fsa.aicenter.domain.log.valueobject;

import lombok.Getter;

/**
 * 日志状态枚举
 */
@Getter
public enum LogStatus {
    SUCCESS(1, "成功"),
    FAILURE(0, "失败");

    private final Integer code;
    private final String desc;

    LogStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LogStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("Log status code cannot be null");
        }
        for (LogStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown log status code: " + code);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
