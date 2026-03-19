package com.fsa.aicenter.infrastructure.config;

import com.fsa.aicenter.common.util.ApiKeyEncryptor;
import com.fsa.aicenter.infrastructure.persistence.typehandler.EncryptedApiKeyTypeHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.StringTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TypeHandler 配置类
 * <p>
 * 负责将 Spring 管理的 Bean 注入到非 Spring 管理的 TypeHandler 中。
 * </p>
 * <p>
 * EncryptedApiKeyTypeHandler 继承 BaseTypeHandler&lt;String&gt; 且没有 @MappedTypes 限定，
 * MyBatis-Plus 处理 @TableField(typeHandler = ...) 时会将其注册为 String 类型的全局默认处理器，
 * 导致所有 String 字段（如 admin_user.username）的参数设置都被加密。
 * 通过 ApplicationRunner 在启动完成后重置为内置 StringTypeHandler 修复此问题。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TypeHandlerConfig {

    private final ApiKeyEncryptor apiKeyEncryptor;

    /**
     * 尽早初始化加密器，确保启动期间的数据库操作也能正常加解密
     */
    @PostConstruct
    public void init() {
        EncryptedApiKeyTypeHandler.initEncryptor(apiKeyEncryptor);
    }

    /**
     * 启动完成后重置默认 String TypeHandler
     * <p>
     * MyBatis-Plus 解析 @TableField(typeHandler = EncryptedApiKeyTypeHandler.class) 时，
     * 会将 EncryptedApiKeyTypeHandler 注册为 String 类型的全局默认处理器。
     * 这里将默认处理器重置为内置的 StringTypeHandler，
     * 确保只有显式指定 typeHandler 的字段才使用加密处理器。
     * </p>
     */
    @Bean
    public ApplicationRunner resetDefaultStringTypeHandler(SqlSessionFactory sqlSessionFactory) {
        return args -> {
            TypeHandlerRegistry registry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
            registry.register(String.class, new StringTypeHandler());
            log.info("已重置默认 String TypeHandler，EncryptedApiKeyTypeHandler 仅对 @TableField 显式指定的字段生效");
        };
    }
}
