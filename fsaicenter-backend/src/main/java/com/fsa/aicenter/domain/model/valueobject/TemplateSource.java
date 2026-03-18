package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 模板来源枚举
 */
@Getter
public enum TemplateSource {
    USER("USER", "用户自定义"),
    SYSTEM("SYSTEM", "系统导入"),
    BUILTIN("BUILTIN", "内置模板");

    private final String code;
    private final String desc;

    TemplateSource(String code, String desc) {
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
    public static TemplateSource fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Template source code cannot be null");
        }
        for (TemplateSource source : values()) {
            // 支持大小写不敏感匹配
            if (source.code.equalsIgnoreCase(code) || source.name().equalsIgnoreCase(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown template source: " + code);
    }
}
