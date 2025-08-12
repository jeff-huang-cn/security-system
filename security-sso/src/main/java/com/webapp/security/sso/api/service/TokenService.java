package com.webapp.security.sso.api.service;

import com.webapp.security.sso.api.exception.InvalidCredentialException;
import com.webapp.security.sso.api.model.TokenResponse;

public interface TokenService {
    TokenResponse generateToken(String appId, String appSecret) throws InvalidCredentialException;

    boolean validateToken(String token);

    String getAppIdFromToken(String token);
}