package com.webapp.security.sso.api.controller;

import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;
import com.webapp.security.sso.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 令牌控制器
 * 处理客户端凭证认证和令牌生成
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    /**
     * 获取访问令牌
     * 客户端通过Basic认证方式发送appId和appSecret
     * 
     * @param request HTTP请求
     * @return 令牌响应或错误信息
     */
    @GetMapping("/token")
    public ResponseResult<TokenResponse> getToken(HttpServletRequest request) {
        try {
            // 获取Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                log.warn("请求缺少Basic认证头");
                return ResponseResult.failed("缺少Basic认证头");
            }

            // 解码Basic认证信息
            String base64Credentials = authHeader.substring(6).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

            // 分割appId和appSecret
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                log.warn("无效的认证格式");
                return ResponseResult.failed("无效的认证格式");
            }

            String appId = parts[0];
            String appSecret = parts[1];

            log.info("接收到来自appId: {} 的令牌请求", appId);

            // 验证凭证并生成令牌
            TokenResponse tokenResponse = tokenService.generateToken(appId, appSecret);
            return ResponseResult.success(tokenResponse);

        } catch (InvalidCredentialException e) {
            log.error("凭证验证失败: {}", e.getMessage());
            return ResponseResult.failed(e.getMessage());
        } catch (Exception e) {
            log.error("处理令牌请求时发生错误", e);
            return ResponseResult.failed("服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 验证令牌有效性
     * 供OpenAPI服务调用
     *
     * @param token 访问令牌
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseResult<Map<String, Object>> validateToken(@RequestParam String token) {
        try {
            boolean isValid = tokenService.validateToken(token);
            if (!isValid) {
                return ResponseResult.failed("无效的访问令牌");
            }

            String appId = tokenService.getAppIdFromToken(token);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("appId", appId);

            log.info("验证令牌成功，appId: {}", appId);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("验证令牌时发生错误", e);
            return ResponseResult.failed("令牌验证失败: " + e.getMessage());
        }
    }
}