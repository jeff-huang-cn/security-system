package com.webapp.security.sso.api.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 自定义不透明令牌自省器
 * 直接使用OAuth2AuthorizationService验证令牌，并使用OpaqueTokenIntrospectionResponseEnhancer增强令牌自省结果
 */
public class CustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger logger = LoggerFactory.getLogger(CustomOpaqueTokenIntrospector.class);

    private final OAuth2AuthorizationService authorizationService;
    private final OpaqueTokenIntrospectionResponseEnhancer responseEnhancer;

    public CustomOpaqueTokenIntrospector(
            OAuth2AuthorizationService authorizationService,
            OpaqueTokenIntrospectionResponseEnhancer responseEnhancer) {
        this.authorizationService = authorizationService;
        this.responseEnhancer = responseEnhancer;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        try {
            // 使用OAuth2AuthorizationService查找令牌
            OAuth2Authorization authorization = authorizationService.findByToken(
                    token, OAuth2TokenType.ACCESS_TOKEN);

            if (authorization == null) {
                logger.warn("Token not found or invalid: {}", token);
                throw new IllegalArgumentException("Invalid token");
            }

            // 获取令牌属性
            Map<String, Object> claims = new HashMap<>();
            claims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
            claims.put(OAuth2TokenIntrospectionClaimNames.CLIENT_ID,
                    authorization.getRegisteredClientId());

            // 如果有主体名称，添加到声明中
            if (authorization.getPrincipalName() != null) {
                claims.put(OAuth2TokenIntrospectionClaimNames.USERNAME,
                        authorization.getPrincipalName());
            }

            // 获取令牌属性 - 使用正确的API
            OAuth2Authorization.Token<?> accessToken = authorization.getToken(OAuth2TokenType.ACCESS_TOKEN.getValue());

            if (accessToken != null) {
                if (accessToken.getToken().getIssuedAt() != null) {
                    claims.put(OAuth2TokenIntrospectionClaimNames.IAT,
                            accessToken.getToken().getIssuedAt().toString());
                }
                if (accessToken.getToken().getExpiresAt() != null) {
                    claims.put(OAuth2TokenIntrospectionClaimNames.EXP,
                            accessToken.getToken().getExpiresAt().toString());
                }
            }

            // 使用增强器添加权限信息
            Map<String, Object> enhancedClaims = responseEnhancer.enhance(token, claims);

            // 创建权限列表
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (enhancedClaims.containsKey("authorities")) {
                @SuppressWarnings("unchecked")
                List<String> authoritiesList = (List<String>) enhancedClaims.get("authorities");
                for (String authority : authoritiesList) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
            }

            // 创建认证主体
            return new EnhancedOAuth2AuthenticatedPrincipal(
                    authorization.getPrincipalName(),
                    enhancedClaims,
                    authorities);

        } catch (Exception e) {
            logger.error("Error introspecting token", e);
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}