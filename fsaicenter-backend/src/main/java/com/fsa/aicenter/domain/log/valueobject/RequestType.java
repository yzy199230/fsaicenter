package com.fsa.aicenter.domain.log.valueobject;

import lombok.Getter;

/**
 * 请求类型枚举
 */
@Getter
public enum RequestType {
    CHAT("CHAT", "对话请求"),
    EMBEDDING("EMBEDDING", "向量请求"),
    IMAGE("IMAGE", "图像生成请求"),
    AUDIO("AUDIO", "音频请求");

    private final String code;
    private final String desc;

    RequestType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RequestType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Request type code cannot be null");
        }
        for (RequestType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown request type: " + code);
    }
}
