package com.webapp.security.sso.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("User login attempt: {}", loginRequest.getUsername());
            
            // 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            // 生成JWT令牌
            String token = generateToken(authentication);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("token_type", "Bearer");
            response.put("expires_in", 3600); // 1小时
            response.put("username", authentication.getName());
            
            log.info("User login successful: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: " + loginRequest.getUsername(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_grant");
            errorResponse.put("error_description", "用户名或密码错误");
            
            return ResponseEntity.status(401).body(errorResponse);
        } catch (Exception e) {
            log.error("Login error for user: " + loginRequest.getUsername(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "服务器内部错误");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // 由于使用JWT无状态认证，登出主要由客户端处理（删除token）
        // 服务端可以记录登出日志或执行其他业务逻辑
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "登出成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 生成JWT令牌
     */
    private String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(authentication.getName())
                .claim("scope", "read write")
                .build();
        
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 登录请求DTO
     */
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
}

