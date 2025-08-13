package com.webapp.security.sso.oauth2.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 自定义令牌生成器
 * 根据客户端ID决定使用哪种令牌格式
 */
public class CustomTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {

    private static final Logger log = LoggerFactory.getLogger(CustomTokenGenerator.class);
    private static final String OPENAPI_CLIENT_ID = "openapi";

    private final OAuth2AccessTokenGenerator accessTokenGenerator;
    private final JwtGenerator jwtGenerator;

    public CustomTokenGenerator(OAuth2AccessTokenGenerator accessTokenGenerator, JwtGenerator jwtGenerator) {
        this.accessTokenGenerator = accessTokenGenerator;
        this.jwtGenerator = jwtGenerator;
    }

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        if (context == null) {
            log.warn("Token context is null");
            return null;
        }

        // 获取客户端ID
        String clientId = context.getRegisteredClient().getClientId();
        log.info("Generating token for client: {}", clientId);

        // 对于OpenAPI客户端，始终使用不透明令牌
        if (OPENAPI_CLIENT_ID.equals(clientId)) {
            log.info("Using opaque token for OpenAPI client");
            OAuth2Token token = accessTokenGenerator.generate(context);
            if (token != null) {
                log.info("Generated opaque token successfully");
            } else {
                log.warn("Failed to generate opaque token for OpenAPI client");
            }
            return token;
        }

        // 默认使用JWT
        log.info("Using JWT token by default for client: {}", clientId);
        OAuth2Token token = jwtGenerator.generate(context);
        if (token == null) {
            log.warn("JWT generation failed, falling back to opaque token");
            return accessTokenGenerator.generate(context);
        }
        return token;
    }
}