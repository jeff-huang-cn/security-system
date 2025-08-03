package com.webapp.security.sso.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT令牌自定义配置
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /**
     * 自定义JWT令牌内容
     * 将用户权限信息添加到令牌中
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            log.info("JWT customizer invoked for token type: {}", context.getTokenType().getValue());

            // 只处理访问令牌
            if (context.getTokenType().getValue().equals("access_token")) {
                // 获取认证信息
                Authentication principal = context.getPrincipal();
                log.info("Principal class: {}", principal.getClass().getName());
                log.info("Principal authorities: {}", principal.getAuthorities());

                // 获取权限
                Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

                if (authorities == null || authorities.isEmpty()) {
                    log.warn("No authorities found in principal: {}", principal.getName());
                } else {
                    // 转换为字符串列表
                    List<String> authoritiesList = authorities.stream()
                            .map(authority -> {
                                String auth = authority.getAuthority();
                                log.info("Adding authority to JWT: {}", auth);
                                return auth;
                            })
                            .collect(Collectors.toList());

                    // 将权限信息添加到JWT声明中
                    log.info("Setting authorities claim in JWT: {}", authoritiesList);
                    context.getClaims().claim("authorities", authoritiesList);
                }
            }
        };
    }
}