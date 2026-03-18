package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 实体状态枚举
 */
@Getter
public enum EntityStatus {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;

    EntityStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 序列化时使用code值
     */
    @JsonValue
    public Integer getCode() {
        return code;
    }

    /**
     * 从代码转换为枚举（反序列化时使用）
     * @param value 状态码（支持Integer或String）
     * @return 状态枚举
     */
    @JsonCreator
    public static EntityStatus fromCode(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Status code cannot be null");
        }

        // 支持Integer类型
        if (value instanceof Integer) {
            Integer code = (Integer) value;
            for (EntityStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }

        // 支持String类型（枚举名称或数字字符串）
        if (value instanceof String) {
            String str = (String) value;
            // 尝试作为枚举名称匹配
            for (EntityStatus status : values()) {
                if (status.name().equalsIgnoreCase(str)) {
                    return status;
                }
            }
            // 尝试作为数字字符串解析
            try {
                Integer code = Integer.parseInt(str);
                for (EntityStatus status : values()) {
                    if (status.code.equals(code)) {
                        return status;
                    }
                }
            } catch (NumberFormatException e) {
                // 忽略，继续抛出异常
            }
        }

        throw new IllegalArgumentException("Unknown status value: " + value);
    }

    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return this == ENABLED;
    }
}
