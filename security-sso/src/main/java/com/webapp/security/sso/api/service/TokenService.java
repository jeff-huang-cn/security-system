package com.webapp.security.sso.api.service;

import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;

/**
 * 令牌服务接口
 */
public interface TokenService {
    /**
     * 验证客户端凭证并生成访问令牌
     * 
     * @param appId     应用ID
     * @param appSecret 应用密钥（明文）
     * @return 令牌响应
     * @throws InvalidCredentialException 如果凭证无效
     */
    TokenResponse generateToken(String appId, String appSecret) throws InvalidCredentialException;

    /**
     * 验证令牌有效性
     * 
     * @param token 访问令牌
     * @return 令牌是否有效
     */
    boolean validateToken(String token);

    /**
     * 获取令牌关联的应用ID
     * 
     * @param token 访问令牌
     * @return 应用ID，如果令牌无效则返回null
     */
    String getAppIdFromToken(String token);
}