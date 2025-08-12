package com.webapp.security.sso.api.controller;

import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;
import com.webapp.security.sso.api.service.TokenService;
import com.webapp.security.sso.api.util.BasicCredentialUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> issueToken(HttpServletRequest request,
            @RequestParam("grant_type") String grantType) {
        try {
            if (!"client_credentials".equals(grantType)) {
                return error(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "授权类型必须为client_credentials");
            }

            String base64 = BasicCredentialUtil.resolveBasicBase64(request);
            String[] creds = BasicCredentialUtil.decodeBasicPair(base64);
            if (creds == null || creds.length != 2) {
                return error(HttpStatus.UNAUTHORIZED, "invalid_client", "缺少或无效的Basic凭证");
            }

            String appId = creds[0];
            String appSecret = creds[1];

            TokenResponse tr = tokenService.generateToken(appId, appSecret);
            Map<String, Object> body = new HashMap<>();
            body.put("access_token", tr.getAccessToken());
            body.put("expires_in", tr.getExpiresIn());
            return ResponseEntity.ok(body);
        } catch (InvalidCredentialException e) {
            return error(HttpStatus.UNAUTHORIZED, "invalid_client", e.getMessage());
        } catch (Exception e) {
            log.error("发令牌发生异常", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", "服务器内部错误: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseResult<Map<String, Object>> validate(@RequestParam("token") String token) {
        try {
            boolean valid = tokenService.validateToken(token);
            if (!valid) {
                return ResponseResult.failed("无效的访问令牌");
            }
            String appId = tokenService.getAppIdFromToken(token);
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("appId", appId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("校验令牌异常", e);
            return ResponseResult.failed("令牌验证失败: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }
}