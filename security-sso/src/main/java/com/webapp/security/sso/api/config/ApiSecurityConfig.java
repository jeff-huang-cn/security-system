package com.webapp.security.sso.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.sso.api.filter.TokenAuthenticationFilter;
import com.webapp.security.sso.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API安全配置
 * 配置基于令牌的API认证
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    /**
     * 配置密码编码器
     * 注意：此配置必须与admin服务中SysClientCredentialServiceImpl使用的配置一致
     * 默认工作因子为10，与admin中static final ENCODER一致
     * 
     * 使用@Primary确保此Bean被优先注入
     * 使用@ConditionalOnMissingBean避免与现有Bean冲突
     * 
     * @return 与admin服务兼容的密码编码器
     */
    @Bean
    @ConditionalOnMissingBean(name = "tokenPasswordEncoder")
    public PasswordEncoder tokenPasswordEncoder() {
        // 确保与admin中使用的编码器完全一致
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    @DependsOn("tokenPasswordEncoder")
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenService, objectMapper);
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .antMatcher("/api/**")
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/v1/token").permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}