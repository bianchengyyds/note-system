package com.ai.exception;

/**
 * 自定义业务异常，支持指定业务状态码
 */
public class BusinessException extends RuntimeException {
    private final int code;

    /**
     * 构造函数：指定状态码和消息
     * @param code 业务状态码（将作为 HTTP 响应码返回）
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数：仅指定消息，默认状态码为 400
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}