package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class InternalServerException extends BaseException {

    private static final long serialVersionUID = 1L;

    public InternalServerException(int code, String message) {
        super(code, message);
    }
}