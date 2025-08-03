package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BizException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BizException(int code, String message) {
        super(code, message);
    }
}