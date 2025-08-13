package com.webapp.security.sso.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * API模块配置类
 * 提供OpenAPI相关的Bean配置
 */
@Configuration
public class ApiConfig {

    /**
     * RestTemplate Bean
     * 用于调用OAuth2 token端点
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}