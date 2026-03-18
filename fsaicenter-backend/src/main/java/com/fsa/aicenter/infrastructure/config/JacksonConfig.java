package com.fsa.aicenter.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson配置类
 * 用于配置全局ObjectMapper Bean
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁用时间戳序列化(使用ISO-8601格式)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略null值字段(JSONB存储优化)
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 可选
        return mapper;
    }
}
