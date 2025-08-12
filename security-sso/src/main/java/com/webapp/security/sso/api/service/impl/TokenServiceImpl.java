package com.webapp.security.sso.api.service.impl;

import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.sso.api.config.ApiTokenProperties;
import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;
import com.webapp.security.sso.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final SysClientCredentialMapper clientCredentialMapper;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ApiTokenProperties tokenProperties;

    private static final String TOKEN_KEY_PREFIX = "openapi:token:";
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public TokenResponse generateToken(String appId, String appSecret) throws InvalidCredentialException {
        SysClientCredential credential = clientCredentialMapper.findByAppId(appId);
        if (credential == null) {
            throw new InvalidCredentialException("无效的客户端凭证");
        }
        if (credential.getStatus() == null || credential.getStatus() != 1) {
            throw new InvalidCredentialException("客户端凭证已被禁用");
        }
        if (!passwordEncoder.matches(appSecret, credential.getAppSecret())) {
            throw new InvalidCredentialException("无效的应用密钥");
        }

        String clientId = credential.getClientId();
        if (!org.springframework.util.StringUtils.hasText(clientId)) {
            throw new InvalidCredentialException("客户端配置错误：缺少OAuth客户端ID");
        }
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new InvalidCredentialException("系统配置错误：OAuth客户端配置不存在");
        }

        // TTL 策略
        Duration ttl = registeredClient.getTokenSettings().getAccessTokenTimeToLive();
        if (tokenProperties.getPolicy() == ApiTokenProperties.TokenPolicy.PROGRAM_CONFIGURED) {
            ttl = Duration.ofSeconds(Math.max(60, tokenProperties.getProgramTtlSeconds()));
        }
        long expiresInSeconds = ttl.getSeconds();
        if (expiresInSeconds <= 0) {
            expiresInSeconds = 2 * 60 * 60;
            ttl = Duration.ofSeconds(expiresInSeconds);
        }

        // 生成高熵不透明令牌
        String accessToken = randomUrlSafe(32);

        // 构建并持久化授权记录（grant_type=client_credentials）
        if (tokenProperties.isPersistInDb()) {
            OAuth2Authorization authorization = buildAuthorization(registeredClient, appId, accessToken, ttl);
            authorizationService.save(authorization);
        }

        // 写入缓存
        cacheToken(accessToken, credential, registeredClient, expiresInSeconds);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresInSeconds)
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        String key = TOKEN_KEY_PREFIX + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return true;
        }
        // DB 回查
        OAuth2Authorization auth = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (auth == null)
            return false;
        OAuth2Authorization.Token<OAuth2AccessToken> tokenObj = auth.getAccessToken();
        if (tokenObj == null || tokenObj.getToken() == null)
            return false;
        Instant expiresAt = tokenObj.getToken().getExpiresAt();
        if (expiresAt == null || Instant.now().isAfter(expiresAt))
            return false;
        // 回填缓存（TTL = 剩余寿命）
        long ttlSeconds = Math.max(1, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        String registeredClientId = auth.getRegisteredClientId();
        RegisteredClient rc = registeredClientRepository.findById(registeredClientId);
        String appId = auth.getPrincipalName();
        SysClientCredential cred = new SysClientCredential();
        cred.setAppId(appId);
        cacheToken(token, cred, rc, ttlSeconds);
        return true;
    }

    @Override
    public String getAppIdFromToken(String token) {
        String key = TOKEN_KEY_PREFIX + token;
        Map<Object, Object> info = redisTemplate.opsForHash().entries(key);
        if (info != null && !info.isEmpty()) {
            Object v = info.get("appId");
            return v == null ? null : v.toString();
        }
        OAuth2Authorization auth = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        return auth == null ? null : auth.getPrincipalName();
    }

    private void cacheToken(String token, SysClientCredential credential, RegisteredClient registeredClient,
            long ttlSeconds) {
        String key = TOKEN_KEY_PREFIX + token;
        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("appId", credential.getAppId());
        if (credential.getId() != null) {
            tokenInfo.put("credentialId", String.valueOf(credential.getId()));
        }
        tokenInfo.put("clientId", registeredClient.getClientId());
        tokenInfo.put("createTime", String.valueOf(Instant.now().getEpochSecond()));
        Set<String> scopes = registeredClient.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            tokenInfo.put("scope", String.join(" ", scopes));
        }
        if (registeredClient.getClientName() != null) {
            tokenInfo.put("clientName", registeredClient.getClientName());
        }
        redisTemplate.opsForHash().putAll(key, tokenInfo);
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    private OAuth2Authorization buildAuthorization(RegisteredClient registeredClient, String principalName,
            String tokenValue, Duration ttl) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ttl);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, tokenValue, issuedAt,
                expiresAt);
        return OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(principalName)
                .authorizationGrantType(
                        org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .token(accessToken)
                .build();
    }

    private String randomUrlSafe(int numBytes) {
        byte[] bytes = new byte[numBytes];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}