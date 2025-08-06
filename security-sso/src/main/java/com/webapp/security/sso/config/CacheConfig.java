package com.webapp.security.sso.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis缓存管理器
     * 支持分布式缓存，应用重启后缓存不丢失
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 缓存30分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存null值

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("oauth2-clients-by-id",
                        config.entryTtl(Duration.ofMinutes(60))) // 客户端信息缓存1小时
                .withCacheConfiguration("oauth2-clients-by-client-id",
                        config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("token-blacklist",
                        config.entryTtl(Duration.ofDays(1))) // 黑名单缓存1天
                .build();
    }
}
