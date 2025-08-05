package com.webapp.security.sso.controller;

import com.webapp.security.sso.context.ClientContext;
import com.webapp.security.sso.model.LoginRequest;
import com.webapp.security.sso.model.RefreshTokenRequest;
import com.webapp.security.sso.model.LogoutRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * OAuth2认证控制�?- 使用OAuth2TokenContext方式
 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    private final AuthenticationManager authenticationManager;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthorizationServerSettings authorizationServerSettings;

    /**
     * 用户登录 - 使用OAuth2TokenContext方式
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 从ClientContext获取clientId
            String clientId = ClientContext.getClientId();
            log.info("OAuth2 login attempt for user: {}, clientId: {}", loginRequest.getUsername(), clientId);

            // 1. 获取注册的客户端
            RegisteredClient registeredClient = getRegisteredClient(clientId);

            // 2. 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            // 4. 创建OAuth2授权
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(authentication.getName())
                    .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.PASSWORD)
                    .authorizedScopes(registeredClient.getScopes());

            // 5. 生成Access Token
            OAuth2AccessToken accessToken = generateAccessToken(authentication, registeredClient, authorizationBuilder);

            // 6. 生成Refresh Token（可选）
            OAuth2RefreshToken refreshToken = generateRefreshToken(authentication, registeredClient,
                    authorizationBuilder);

            // 7. 保存授权信息
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);

            // 8. 计算过期时间（秒）
            long expiresIn = 0;
            if (accessToken.getExpiresAt() != null) {
                expiresIn = Duration.between(Instant.now(), accessToken.getExpiresAt()).getSeconds();
            }

            // 9. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken.getTokenValue());
            response.put("token_type", accessToken.getTokenType().getValue());
            response.put("expires_in", expiresIn);
            response.put("scope", String.join(" ", accessToken.getScopes()));
            response.put("username", authentication.getName());
            response.put("client_id", clientId);

            if (refreshToken != null) {
                response.put("refresh_token", refreshToken.getTokenValue());
            }

            log.info("OAuth2 User login successful: {} for client: {}", loginRequest.getUsername(), clientId);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            log.warn("OAuth2 Login failed for user: " + loginRequest.getUsername(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_grant");
            errorResponse.put("error_description", "用户名或密码错误");

            return ResponseEntity.status(401).body(errorResponse);
        } catch (IllegalStateException e) {
            log.warn("OAuth2 Client error: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_client");
            errorResponse.put("error_description", e.getMessage());

            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("OAuth2 Login error for user: " + loginRequest.getUsername(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "服务器内部错误");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 创建AuthorizationServerContext
     */
    private AuthorizationServerContext createAuthorizationServerContext() {
        return new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return authorizationServerSettings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return authorizationServerSettings;
            }
        };
    }

    /**
     * 生成Access Token - 使用OAuth2TokenContext
     */
    private OAuth2AccessToken generateAccessToken(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(createAuthorizationServerContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.PASSWORD)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);

        if (!(generatedToken instanceof Jwt)) {
            throw new IllegalStateException("生成的令牌不是Jwt类型");
        }

        Jwt jwt = (Jwt) generatedToken;

        // 将JWT包装为OAuth2AccessToken
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                registeredClient.getScopes());

        // 将令牌添加到授权构建器
        authorizationBuilder.accessToken(accessToken);

        return accessToken;
    }

    /**
     * 生成Refresh Token - 使用OAuth2TokenContext
     */
    private OAuth2RefreshToken generateRefreshToken(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 检查客户端是否支持refresh token
        if (!registeredClient.getAuthorizationGrantTypes().contains(
                org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)) {
            return null;
        }

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(createAuthorizationServerContext())
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.PASSWORD)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);

        if (!(generatedToken instanceof OAuth2RefreshToken)) {
            return null;
        }

        OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) generatedToken;

        // 将令牌添加到授权构建器
        authorizationBuilder.refreshToken(refreshToken);

        return refreshToken;
    }

    /**
     * 根据客户端ID获取注册客户端
     * 每个应用在授权服务数据库添加后，通过请求传递clientId
     */
    private RegisteredClient getRegisteredClient(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("客户端ID不能为空");
        }

        RegisteredClient client = registeredClientRepository.findByClientId(clientId.trim());
        if (client == null) {
            throw new IllegalStateException("未找到客户端: " + clientId + "，请确保该客户端已在授权服务器中注册");
        }

        return client;
    }

    /**
     * 用户登出 - 撤销授权记录
     * 授权记录是指OAuth2Authorization，包含用户的访问令牌、刷新令牌等信息
     * 这不是第三方授权登录记录，而是本授权服务器颁发的令牌授权记录
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        try {
            // 从ClientContext获取clientId
            String clientId = ClientContext.getClientId();
            String accessToken = logoutRequest.getAccessToken();

            if (accessToken == null || accessToken.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "访问令牌不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 1. 根据访问令牌查找授权记录
            OAuth2Authorization authorization = authorizationService.findByToken(accessToken,
                    OAuth2TokenType.ACCESS_TOKEN);

            if (authorization != null) {
                // 2. 验证客户端ID（如果提供）
                if (clientId != null && !clientId.trim().isEmpty()) {
                    // 直接通过clientId查找RegisteredClient，然后比较registeredClientId
                    RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
                    if (registeredClient == null
                            || !registeredClient.getId().equals(authorization.getRegisteredClientId())) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "invalid_client");
                        errorResponse.put("error_description", "客户端ID不匹配");
                        return ResponseEntity.status(400).body(errorResponse);
                    }
                }

                // 3. 删除授权记录（撤销所有相关令牌）
                authorizationService.remove(authorization);

                log.info("OAuth2 Authorization revoked for user: {} client: {}",
                        authorization.getPrincipalName(),
                        authorization.getRegisteredClientId());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "登出成功，授权记录已撤销");
                response.put("revoked_tokens", "access_token, refresh_token");

                return ResponseEntity.ok(response);
            } else {
                // 令牌不存在或已过期
                Map<String, Object> response = new HashMap<>();
                response.put("message", "登出成功，令牌已失效");

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Logout error", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "登出失败");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 刷新令牌 - 完整实现
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            javax.servlet.http.HttpServletRequest request) {
        try {
            String refreshTokenValue = refreshTokenRequest.getRefreshToken();
            // 从ClientContext获取clientId
            String clientId = ClientContext.getClientId();

            // 1. 验证请求参数
            if (refreshTokenValue == null || refreshTokenValue.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "刷新令牌不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            if (clientId == null || clientId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_client");
                errorResponse.put("error_description", "客户端ID不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 2. 根据刷新令牌查找授权记录
            OAuth2Authorization authorization = authorizationService.findByToken(refreshTokenValue,
                    OAuth2TokenType.REFRESH_TOKEN);

            if (authorization == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_grant");
                errorResponse.put("error_description", "刷新令牌无效或已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 3. 验证客户端ID
            // 直接通过clientId查找RegisteredClient，然后比较registeredClientId
            RegisteredClient registeredClientForValidation = registeredClientRepository.findByClientId(clientId);
            if (registeredClientForValidation == null
                    || !registeredClientForValidation.getId().equals(authorization.getRegisteredClientId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_client");
                errorResponse.put("error_description", "客户端ID不匹配");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 4. 检查刷新令牌是否过期
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
            if (refreshToken == null || (refreshToken.getToken().getExpiresAt() != null &&
                    refreshToken.getToken().getExpiresAt().isBefore(Instant.now()))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_grant");
                errorResponse.put("error_description", "刷新令牌已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 5. 获取注册客户端和用户信息
            RegisteredClient registeredClient = getRegisteredClient(clientId);

            // 6. 重新构建认证信息 - 需要从UserDetailsService重新加载用户权限
            String username = authorization.getPrincipalName();

            // 从Spring上下文中获取UserDetailsService
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService = org.springframework.web.context.support.WebApplicationContextUtils
                    .getRequiredWebApplicationContext(request.getServletContext())
                    .getBean(org.springframework.security.core.userdetails.UserDetailsService.class);

            // 加载完整的用户详情，包括权限
            org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                    .loadUserByUsername(username);

            // 使用完整的用户权限创建新的认证对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            // 7. 创建新的授权构建器
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.from(authorization);

            // 8. 生成新的访问令牌
            OAuth2AccessToken newAccessToken = generateAccessToken(authentication, registeredClient,
                    authorizationBuilder);

            // 9. 使用刷新令牌轮换机制
            // 每次刷新时生成新的refresh_token和新的授权记录
            log.info("使用token轮换机制，生成新的refresh_token，用户: {}, 客户端: {}",
                    authorization.getPrincipalName(), clientId);

            // 生成全新的刷新令牌，不再重用旧的
            OAuth2RefreshToken newRefreshToken = generateRefreshToken(authentication, registeredClient,
                    authorizationBuilder);

            // 仍然生成新的授权ID，避免覆盖原授权记录
            // 这样同时存在新旧两个授权记录，旧的会自然过期
            String newAuthorizationId = "refresh-" + UUID.randomUUID();
            authorizationBuilder.id(newAuthorizationId);

            // 10. 保存授权记录
            OAuth2Authorization newAuthorization = authorizationBuilder.build();
            authorizationService.save(newAuthorization);

            // 12. 计算过期时间
            long expiresIn = 0;
            if (newAccessToken.getExpiresAt() != null) {
                expiresIn = Duration.between(Instant.now(), newAccessToken.getExpiresAt()).getSeconds();
            }

            // 13. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", newAccessToken.getTokenValue());
            response.put("token_type", newAccessToken.getTokenType().getValue());
            response.put("expires_in", expiresIn);
            response.put("scope", String.join(" ", newAccessToken.getScopes()));
            response.put("refresh_token", newRefreshToken.getTokenValue());

            log.info("OAuth2 Token refreshed for user: {} client: {}",
                    authorization.getPrincipalName(), clientId);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("OAuth2 Refresh token client error: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_client");
            errorResponse.put("error_description", e.getMessage());

            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("Refresh token error", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "刷新令牌失败");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}
