package com.webapp.security.sso.api.service.impl;

import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;
import com.webapp.security.sso.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 令牌服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final SysClientCredentialMapper clientCredentialMapper;
    private final RegisteredClientRepository registeredClientRepository;
    private final StringRedisTemplate redisTemplate;

    @Qualifier("tokenPasswordEncoder")
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public TokenResponse generateToken(String appId, String appSecret) throws InvalidCredentialException {
        // 1. 查找客户端凭证
        SysClientCredential credential = clientCredentialMapper.findByAppId(appId);
        if (credential == null) {
            log.warn("找不到AppID: {} 对应的客户端凭证", appId);
            throw new InvalidCredentialException("无效的客户端凭证");
        }

        // 2. 检查凭证状态
        if (credential.getStatus() != 1) {
            log.warn("客户端凭证 {} 已被禁用", appId);
            throw new InvalidCredentialException("客户端凭证已被禁用");
        }

        // 3. 验证密钥
        if (!passwordEncoder.matches(appSecret, credential.getAppSecret())) {
            log.warn("客户端 {} 提供的密钥无效", appId);
            throw new InvalidCredentialException("无效的应用密钥");
        }

        // 4. 查询OAuth2注册客户端信息
        String clientId = credential.getClientId();
        if (clientId == null || clientId.isEmpty()) {
            log.error("客户端凭证 {} 没有关联的OAuth客户端ID", appId);
            throw new InvalidCredentialException("客户端配置错误：缺少OAuth客户端ID");
        }

        RegisteredClient registeredClient = registeredClientRepository.findById(clientId);
        if (registeredClient == null) {
            // 尝试通过clientId查找
            registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                log.error("找不到clientId为{}的OAuth2注册客户端", clientId);
                throw new InvalidCredentialException("系统配置错误：OAuth客户端配置不存在");
            }
        }

        // 5. 获取令牌有效期，必须从数据库配置获取
        Duration tokenTTL = registeredClient.getTokenSettings().getAccessTokenTimeToLive();
        if (tokenTTL == null) {
            log.error("客户端 {} (clientId={}) 未配置令牌有效期", appId, clientId);
            throw new InvalidCredentialException("系统配置错误：未指定令牌有效期");
        }
        long expiresIn = tokenTTL.getSeconds();

        if (expiresIn <= 0) {
            log.error("客户端 {} (clientId={}) 配置的令牌有效期无效: {}", appId, clientId, expiresIn);
            throw new InvalidCredentialException("系统配置错误：令牌有效期必须大于0");
        }

        // 6. 生成随机令牌
        String accessToken = generateRandomToken();

        // 7. 存储令牌信息到Redis
        storeTokenInRedis(accessToken, credential, registeredClient, expiresIn);

        // 8. 返回令牌响应
        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .tokenType(OAuth2AccessToken.TokenType.BEARER.getValue())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        // 直接检查Redis中是否存在该令牌的键
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }

    @Override
    public String getAppIdFromToken(String token) {
        // 直接从Redis中获取令牌相关信息
        Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(token);
        return tokenInfo != null && !tokenInfo.isEmpty() ? (String) tokenInfo.get("appId") : null;
    }

    /**
     * 生成随机令牌
     * 
     * @return Base64编码的随机字符串
     */
    private String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 将令牌信息存储到Redis
     * 
     * @param token            访问令牌
     * @param credential       客户端凭证
     * @param registeredClient OAuth2注册客户端
     * @param expiresIn        有效期（秒）
     */
    private void storeTokenInRedis(String token, SysClientCredential credential,
            RegisteredClient registeredClient, long expiresIn) {
        // 直接使用令牌作为键
        String key = token;

        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("appId", credential.getAppId());
        tokenInfo.put("credentialId", String.valueOf(credential.getId()));
        tokenInfo.put("clientId", registeredClient.getClientId());
        tokenInfo.put("createTime", String.valueOf(Instant.now().getEpochSecond()));

        // 从registeredClient获取作用域信息
        tokenInfo.put("scope", String.join(" ", registeredClient.getScopes()));

        // 将客户端名称也存入Redis，便于后续使用
        if (registeredClient.getClientName() != null) {
            tokenInfo.put("clientName", registeredClient.getClientName());
        }

        redisTemplate.opsForHash().putAll(key, tokenInfo);
        redisTemplate.expire(key, expiresIn, TimeUnit.SECONDS);

        log.info("为客户端 {} 生成的令牌已存储，过期时间: {} 秒", credential.getAppId(), expiresIn);
    }
}