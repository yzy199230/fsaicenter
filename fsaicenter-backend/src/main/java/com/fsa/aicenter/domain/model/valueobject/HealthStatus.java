package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 健康状态枚举
 *
 * @author FSA AI Center
 */
@Getter
public enum HealthStatus {
    HEALTHY(1, "健康"),
    UNHEALTHY(0, "异常"),
    DISABLED(-1, "禁用");

    private final int code;
    private final String desc;

    HealthStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 序列化时使用code值
     */
    @JsonValue
    public int getCode() {
        return code;
    }

    /**
     * 反序列化时使用此方法（支持int或String）
     */
    @JsonCreator
    public static HealthStatus fromCode(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Health status code cannot be null");
        }

        // 支持Integer类型
        if (value instanceof Integer) {
            int code = (Integer) value;
            for (HealthStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown health status code: " + code);
        }

        // 支持String类型（枚举名称或数字字符串）
        if (value instanceof String) {
            String str = (String) value;
            // 尝试作为枚举名称匹配
            for (HealthStatus status : values()) {
                if (status.name().equalsIgnoreCase(str)) {
                    return status;
                }
            }
            // 尝试作为数字字符串解析
            try {
                int code = Integer.parseInt(str);
                for (HealthStatus status : values()) {
                    if (status.code == code) {
                        return status;
                    }
                }
            } catch (NumberFormatException e) {
                // 忽略，继续抛出异常
            }
        }

        throw new IllegalArgumentException("Unknown health status value: " + value);
    }
}
