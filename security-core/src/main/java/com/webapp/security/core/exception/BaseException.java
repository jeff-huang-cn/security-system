package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected final int code;
    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }
}