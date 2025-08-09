package com.webapp.security.sso.custom.oauth2.controller;

import com.webapp.security.sso.custom.oauth2.service.CustomJwtAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义JWT认证控制器
 * 提供JWT令牌的生成、验证、刷新等接口
 */
@RestController
@RequestMapping("/api/custom/jwt")
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class CustomJwtController {

    private static final Logger log = LoggerFactory.getLogger(CustomJwtController.class);

    private final CustomJwtAuthenticationService jwtAuthenticationService;

    public CustomJwtController(CustomJwtAuthenticationService jwtAuthenticationService) {
        this.jwtAuthenticationService = jwtAuthenticationService;
    }

    /**
     * 用户登录并获取JWT令牌
     * POST /api/custom/jwt/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Custom JWT login attempt for user: {}", loginRequest.getUsername());

            Map<String, Object> response = jwtAuthenticationService.authenticateAndGenerateToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Custom JWT login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 刷新JWT令牌
     * POST /api/custom/jwt/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            log.info("Custom JWT token refresh attempt");

            Map<String, Object> response = jwtAuthenticationService.refreshToken(request.getRefreshToken());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Custom JWT token refresh failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 验证JWT令牌
     * POST /api/custom/jwt/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody ValidateTokenRequest request) {
        try {
            boolean isValid = jwtAuthenticationService.validateToken(request.getToken());

            if (isValid) {
                Map<String, Object> userInfo = jwtAuthenticationService.getUserInfoFromToken(request.getToken());
                return ResponseEntity.ok(userInfo);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid token");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Custom JWT token validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 撤销JWT令牌
     * POST /api/custom/jwt/revoke
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken(@RequestBody RevokeTokenRequest request) {
        try {
            jwtAuthenticationService.revokeToken(request.getToken());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Token revoked successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Custom JWT token revocation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取用户信息
     * GET /api/custom/jwt/userinfo
     */
    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Map<String, Object> userInfo = jwtAuthenticationService.getUserInfoFromToken(token);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Failed to get user info from custom JWT: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 请求和响应类
    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        // Getters and Setters
        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class ValidateTokenRequest {
        private String token;

        // Getters and Setters
        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class RevokeTokenRequest {
        private String token;

        // Getters and Setters
        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}