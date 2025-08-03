package com.webapp.security.core.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class IllegalArgumentExtendedException extends BaseException  {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    public IllegalArgumentExtendedException(int code, String fieldName, String errorMessage) {
        super(code,  String.format("字段【%s】错误：%s", fieldName, errorMessage));
        this.fieldName = fieldName;
    }
}