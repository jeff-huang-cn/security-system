package com.webapp.security.sso.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * 令牌响应数据模型
 */
@Data
@Builder
public class TokenResponse {
    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 有效期（秒）
     */
    private long expiresIn;

    /**
     * 令牌类型
     */
    private String tokenType;
}