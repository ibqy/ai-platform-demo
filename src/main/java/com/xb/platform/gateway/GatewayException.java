package com.xb.platform.gateway;

import org.springframework.http.HttpStatus;

/**
 * 网关业务异常 —— 携带错误码 + HTTP 状态码
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p>管道中各步骤抛出的异常统一用此类，由 {@link GlobalExceptionHandler} 映射为 HTTP 响应。
 * 错误码与 {@link GlobalExceptionHandler#ERROR_CODE_STATUS} 配合使用。</p>
 */
public class GatewayException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public GatewayException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public static GatewayException of(String errorCode) {
        HttpStatus status = GlobalExceptionHandler.ERROR_CODE_STATUS.getOrDefault(errorCode, HttpStatus.BAD_REQUEST);
        return new GatewayException(errorCode, errorCode, status.value());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
