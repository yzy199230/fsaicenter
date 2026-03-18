package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 模型类型枚举
 */
@Getter
public enum ModelType {
    CHAT("chat", "文本生成", "对话、文章、代码生成等"),
    IMAGE_RECOGNITION("image_recognition", "视觉分析", "图片理解、多模态分析"),
    ASR("asr", "语音识别", "语音转文字"),
    TTS("tts", "语音合成", "文字转语音"),
    IMAGE("image", "图像生成", "文生图、图生图"),
    EMBEDDING("embedding", "向量嵌入", "文本向量化"),
    VIDEO("video", "视频生成", "文生视频、图生视频");

    private final String code;
    private final String desc;
    private final String description;

    ModelType(String code, String desc, String description) {
        this.code = code;
        this.desc = desc;
        this.description = description;
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
    public static ModelType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Model type code cannot be null");
        }
        for (ModelType type : values()) {
            // 支持大小写不敏感匹配（兼容数据库存储的大写格式）
            if (type.code.equalsIgnoreCase(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model type: " + code);
    }
}
