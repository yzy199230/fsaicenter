package com.fsa.aicenter.infrastructure.adapter.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * 自定义反序列化器，支持OpenAI content字段的两种格式：
 * 1. 字符串格式: "content": "hello"
 * 2. 数组格式(多模态): "content": [{"type":"text","text":"hello"}, {"type":"image_url",...}]
 *
 * 数组格式时提取所有type=text的文本内容拼接为字符串
 */
public class ContentDeserializer extends StdDeserializer<String> {

    public ContentDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.getText();
        }

        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (p.currentToken() == JsonToken.START_ARRAY) {
            JsonNode arrayNode = p.readValueAsTree();
            StringBuilder sb = new StringBuilder();
            for (JsonNode element : arrayNode) {
                if (element.isTextual()) {
                    sb.append(element.asText());
                } else if (element.isObject()) {
                    String type = element.has("type") ? element.get("type").asText() : "";
                    if ("text".equals(type) && element.has("text")) {
                        sb.append(element.get("text").asText());
                    }
                }
            }
            return sb.toString();
        }

        return p.getText();
    }
}
