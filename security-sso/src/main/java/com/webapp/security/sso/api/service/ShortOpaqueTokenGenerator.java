package com.webapp.security.sso.api.service;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

/**
 * 自定义不透明令牌生成器
 * 生成更短的不透明令牌
 */
public class ShortOpaqueTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {

    private static final int TOKEN_LENGTH = 32; // 生成32字符的令牌
    private static final Random RANDOM = new Random();

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null ||
                !context.getTokenType().getValue().equals(OAuth2TokenType.ACCESS_TOKEN.getValue())) {
            return null;
        }

        // 从客户端配置中获取令牌有效期
        RegisteredClient registeredClient = context.getRegisteredClient();
        Duration accessTokenTimeToLive = registeredClient.getTokenSettings().getAccessTokenTimeToLive();

        // 获取当前时间和过期时间
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTimeToLive);

        // 生成短令牌值
        String tokenValue = generateShortToken();

        // 创建OAuth2AccessToken
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                issuedAt,
                expiresAt,
                context.getAuthorizedScopes());
    }

    /**
     * 生成更短的令牌值
     */
    private String generateShortToken() {
        byte[] randomBytes = new byte[24]; // 24字节会生成约32个Base64字符
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}