package com.fsa.aicenter.domain.billing.valueobject;

import lombok.Getter;

/**
 * 计费类型枚举
 */
@Getter
public enum BillingType {
    TOKEN("TOKEN", "按Token计费"),
    IMAGE("IMAGE", "按图片数计费"),
    AUDIO_DURATION("AUDIO_DURATION", "按音频时长计费"),
    VIDEO_DURATION("VIDEO_DURATION", "按视频时长计费");

    private final String code;
    private final String desc;

    BillingType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BillingType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Billing type code cannot be null");
        }
        for (BillingType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown billing type: " + code);
    }
}
