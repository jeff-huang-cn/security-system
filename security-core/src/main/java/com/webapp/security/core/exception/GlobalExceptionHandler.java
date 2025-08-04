package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import com.webapp.security.core.model.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResponseResult<Void> handleBusinessException(BizException e, HttpServletRequest request) {
        log.warn("业务异常：{}，请求URL：{}", e.getMessage(), request.getRequestURI());
        return ResponseResult.failed(ErrorCode.INTERNAL, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseResult<Void> handleRuntimeException(Exception e, HttpServletRequest request) {
        log.error("系统异常：{}，请求URL：{}", e.getMessage(), request.getRequestURI(), e);
        return ResponseResult.failed(ErrorCode.INTERNAL, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常：{}，请求URL：{}", e.getMessage(), request.getRequestURI(), e);
        return ResponseResult.failed(ErrorCode.INTERNAL, "系统异常");
    }
}