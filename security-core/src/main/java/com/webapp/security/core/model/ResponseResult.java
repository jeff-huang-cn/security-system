package com.webapp.security.core.model;

import java.io.Serializable;

/**
 * 统一API响应结果封装
 * 
 * @param <T> 数据类型
 */
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public ResponseResult() {
    }

    public ResponseResult(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResponseResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage());
    }

    /**
     * 成功返回结果
     * 
     * @param data 获取的数据
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回结果
     * 
     * @param data    获取的数据
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> success(T data, String message) {
        return new ResponseResult<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> ResponseResult<T> failed() {
        return new ResponseResult<>(ErrorCode.OPERATION_FAILED.getCode(),
                ErrorCode.OPERATION_FAILED.getMessage());
    }

    /**
     * 失败返回结果
     * 
     * @param errorCode 错误码
     */
    public static <T> ResponseResult<T> failed(ErrorCode errorCode) {
        return new ResponseResult<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败返回结果
     * 
     * @param code    状态码
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> failed(Integer code, String message) {
        return new ResponseResult<>(code, message);
    }

    /**
     * 失败返回结果
     * 
     * @param message 提示信息
     */
    public static <T> ResponseResult<T> failed(String message) {
        return new ResponseResult<>(ErrorCode.OPERATION_FAILED.getCode(), message);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}