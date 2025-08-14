package com.webapp.security.sso.api.controller;

import com.webapp.security.sso.api.service.TokenIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 令牌自省控制器
 * 处理令牌自省请求
 */
@RestController
public class TokenIntrospectionController {

    private static final Logger logger = LoggerFactory.getLogger(TokenIntrospectionController.class);

    private final TokenIntrospectionService introspectionService;

    @Autowired
    public TokenIntrospectionController(TokenIntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
        logger.info("TokenIntrospectionController initialized");
    }

    /**
     * 处理令牌自省请求
     * 这个端点将被资源服务器调用，用于验证令牌的有效性和获取权限信息
     *
     * @param token 令牌值
     * @return 令牌自省结果
     */
    @PostMapping("/v1/oauth2/introspect")
    public ResponseEntity<Map<String, Object>> introspect(@RequestParam("token") String token) {
        logger.info("Received introspection request for token: {}...", token.substring(0, Math.min(token.length(), 8)));
        Map<String, Object> response = introspectionService.introspect(token);
        return ResponseEntity.ok(response);
    }
}