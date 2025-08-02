package com.webapp.security.sso.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 缓存配置
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器 - 使用内存缓存
     * 生产环境可以考虑使用Redis等分布式缓存
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "oauth2-clients-by-id",
            "oauth2-clients-by-client-id"
        ));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}

