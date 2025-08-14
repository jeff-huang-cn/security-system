package com.webapp.security.sso.api.controller;

import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.service.SysClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAPI Token控制器
 * 处理客户端凭证授权流程，将appid/appsecret转换为OAuth2客户端凭证
 */
@RestController
@RequestMapping("/v1/oauth2")
@RequiredArgsConstructor
public class OpenApiTokenController {

    private static final Logger log = LoggerFactory.getLogger(OpenApiTokenController.class);

    private final SysClientCredentialService credentialService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    @Value("${oauth2.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // 固定的OAuth2客户端配置
    private static final String OAUTH2_CLIENT_ID = "openapi";
    private static final String OAUTH2_CLIENT_SECRET = "IPSG-YbDDJ4C_tscD-OuYfrfSmVW8UKV";

    /**
     * OpenAPI Token获取端点
     * 兼容标准OAuth2客户端凭证流程
     *
     * @param request   HTTP请求
     * @param grantType 授权类型，必须为client_credentials
     * @return Token响应
     */
    @PostMapping("/token")
    public ResponseEntity<?> getToken(
            HttpServletRequest request,
            @RequestParam("grant_type") String grantType) {

        try {
            log.info("OpenAPI token request received, grant_type: {}", grantType);

            // 1. 验证授权类型
            if (!"client_credentials".equals(grantType)) {
                return createErrorResponse("unsupported_grant_type",
                        "grant_type " + grantType + " is not supported", 400);
            }

            // 2. 解析Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return createErrorResponse("invalid_client",
                        "Missing or invalid Authorization header", 401);
            }

            // 3. 解码Basic认证
            String encodedCredentials = authHeader.substring(6); // 去掉"Basic "
            String decodedCredentials;
            try {
                decodedCredentials = new String(
                        Base64.getDecoder().decode(encodedCredentials),
                        StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return createErrorResponse("invalid_client",
                        "Invalid Base64 encoding in Authorization header", 401);
            }

            // 4. 分离appid和appsecret
            String[] credentials = decodedCredentials.split(":", 2);
            if (credentials.length != 2) {
                return createErrorResponse("invalid_client",
                        "Invalid credentials format", 401);
            }

            String appId = credentials[0];
            String appSecret = credentials[1];

            log.info("Parsed credentials - appId: {}", appId);

            // 5. 验证appid和appsecret
            SysClientCredential credential = credentialService.findByAppId(appId);
            if (credential == null) {
                log.warn("AppId not found: {}", appId);
                return createErrorResponse("invalid_client",
                        "Invalid client credentials", 401);
            }

            // 检查凭证状态
            if (credential.getStatus() == null || credential.getStatus() != 1) {
                log.warn("AppId disabled: {}", appId);
                return createErrorResponse("invalid_client",
                        "Client credentials disabled", 401);
            }

            // 验证密钥
            if (!passwordEncoder.matches(appSecret, credential.getAppSecret())) {
                log.warn("Invalid appSecret for appId: {}", appId);
                return createErrorResponse("invalid_client",
                        "Invalid client credentials", 401);
            }

            // 6. 验证clientId匹配
            if (!OAUTH2_CLIENT_ID.equals(credential.getClientId())) {
                log.warn("ClientId mismatch for appId: {}, expected: {}, actual: {}",
                        appId, OAUTH2_CLIENT_ID, credential.getClientId());
                return createErrorResponse("invalid_client",
                        "Client configuration error", 401);
            }

            log.info("Credentials validated successfully for appId: {}", appId);

            // 7. 构造OAuth2客户端凭证
            String oauth2Credentials = OAUTH2_CLIENT_ID + ":" + OAUTH2_CLIENT_SECRET;
            String oauth2BasicAuth = "Basic " + Base64.getEncoder()
                    .encodeToString(oauth2Credentials.getBytes(StandardCharsets.UTF_8));

            // 8. 调用Spring Security OAuth2端点
            String tokenUrl = serverBaseUrl + "/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", oauth2BasicAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("token_format", "opaque");

            // 添加额外的日志，查看请求详情
            log.info("Calling OAuth2 token endpoint: {} with token_format=opaque", tokenUrl);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                // 添加日志，查看响应内容
                log.info("Token response received: {}", tokenResponse);

                // 检查令牌类型
                String accessToken = (String) tokenResponse.get("access_token");
                if (accessToken != null && accessToken.startsWith("ey")) {
                    log.warn("Received JWT token instead of opaque token");
                }

                // 9. 构造标准响应格式
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token"));
                result.put("expires_in", tokenResponse.get("expires_in"));

                log.info("Token generated successfully for appId: {}", appId);
                return ResponseEntity.ok(result);
            } else {
                log.error("OAuth2 token endpoint returned error: {}", response.getStatusCode());
                return createErrorResponse("server_error",
                        "Failed to generate token", 500);
            }

        } catch (Exception e) {
            log.error("Error processing token request", e);
            return createErrorResponse("server_error",
                    "Internal server error", 500);
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(
            String error, String description, int status) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> errorDetail = new HashMap<>();
        errorDetail.put("code", String.valueOf(status).substring(0, 1) + "0001"); // 简化错误码
        errorDetail.put("message", description);
        errorResponse.put("error", errorDetail);

        return ResponseEntity.status(status).body(errorResponse);
    }
}