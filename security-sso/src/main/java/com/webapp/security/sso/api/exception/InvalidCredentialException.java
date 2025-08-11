package com.webapp.security.sso.api.exception;

/**
 * 客户端凭证验证失败异常
 */
public class InvalidCredentialException extends RuntimeException {
    public InvalidCredentialException(String message) {
        super(message);
    }

    public InvalidCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}