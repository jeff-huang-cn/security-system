package com.webapp.security.sso.api.endpoint;

import com.webapp.security.sso.api.token.OpaqueTokenIntrospectionResponseEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义令牌自省端点
 * 替代默认的/oauth2/introspect端点
 */
@RestController
public class CustomTokenIntrospectionEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomTokenIntrospectionEndpoint.class);

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository clientRepository;

    @Autowired
    private OpaqueTokenIntrospectionResponseEnhancer responseEnhancer;

    /**
     * 自定义令牌自省端点
     * 处理令牌自省请求，并使用OpaqueTokenIntrospectionResponseEnhancer增强响应
     */
    @PostMapping("/oauth2/introspect")
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token,
            Authentication authentication) {

        logger.info("Custom token introspection endpoint called for token: {}...",
                token.substring(0, Math.min(token.length(), 8)));
        logger.info("Authentication: {}", authentication);

        try {
            // 验证客户端认证
            if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
                logger.warn("Invalid authentication type: {}", authentication.getClass().getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String clientId = authentication.getName();
            logger.info("Client ID from authentication: {}", clientId);

            // 查找令牌
            OAuth2Authorization authorization = authorizationService.findByToken(
                    token, OAuth2TokenType.ACCESS_TOKEN);

            if (authorization == null) {
                logger.warn("Token not found: {}...", token.substring(0, Math.min(token.length(), 8)));
                Map<String, Object> response = new HashMap<>();
                response.put("active", false);
                return ResponseEntity.ok(response);
            }

            logger.info("Token found for client: {}", authorization.getRegisteredClientId());

            // 创建基本响应
            Map<String, Object> claims = new HashMap<>();
            claims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
            claims.put(OAuth2TokenIntrospectionClaimNames.CLIENT_ID, authorization.getRegisteredClientId());

            if (authorization.getPrincipalName() != null) {
                claims.put(OAuth2TokenIntrospectionClaimNames.USERNAME, authorization.getPrincipalName());
            }

            // 获取令牌属性
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

            logger.info("Base claims before enhancement: {}", claims);

            // 使用增强器添加权限信息
            Map<String, Object> enhancedClaims = responseEnhancer.enhance(token, claims);

            logger.info("Enhanced claims after enhancement: {}", enhancedClaims);

            return ResponseEntity.ok(enhancedClaims);

        } catch (Exception e) {
            logger.error("Error processing introspection request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}