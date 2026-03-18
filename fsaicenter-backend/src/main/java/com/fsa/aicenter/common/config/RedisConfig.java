package com.fsa.aicenter.common.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置
 *
 * @author FSA AI Center
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用GenericJackson2JsonRedisSerializer来序列化和反序列化redis的value值（Spring Boot 3.x推荐）
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用GenericJackson2JsonRedisSerializer
        template.setValueSerializer(serializer);
        // hash的value序列化方式采用GenericJackson2JsonRedisSerializer
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return configuration -> {
            if (StringUtils.isEmpty(configuration.useSingleServer().getPassword())) {
                configuration.useSingleServer().setPassword(null);
            }
        };
    }
}
