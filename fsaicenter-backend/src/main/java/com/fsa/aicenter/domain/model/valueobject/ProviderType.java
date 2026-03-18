package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 提供商类型枚举
 */
@Getter
public enum ProviderType {
    REMOTE("remote", "远程提供商"),
    LOCAL("local", "本地提供商");

    private final String code;
    private final String desc;

    ProviderType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 序列化时使用code值
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 反序列化时使用此方法
     */
    @JsonCreator
    public static ProviderType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Provider type code cannot be null");
        }
        for (ProviderType type : values()) {
            // 支持大小写不敏感匹配（兼容数据库存储的大写格式）
            if (type.code.equalsIgnoreCase(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider type: " + code);
    }
}
