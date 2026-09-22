package com.xb.platform.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI 网关全局异常处理器
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p><b>高阶知识点</b>：
 * <ul>
 *     <li>将管道中的错误码映射为正确的 HTTP 状态码（而非全部返回 200）</li>
 *     <li>统一错误响应格式：code + message + correlationId + timestamp</li>
 *     <li>correlationId 用于链路追踪，方便排查问题</li>
 *     <li>Jakarta Validation 校验失败 → 400，带字段级错误详情</li>
 * </ul>
 *
 * <p><b>设计决策</b>：
 * <ul>
 *     <li>业务异常（GatewayException）携带错误码 + HTTP 状态，一处定义全局使用</li>
 *     <li>参数校验异常提取字段名 + 默认消息，前端可直接展示</li>
 *     <li>未知异常返回 500 + 通用消息，不泄露堆栈信息</li>
 * </ul>
 *
 * @author ibqy
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final Map<String, HttpStatus> ERROR_CODE_STATUS = Map.of(
            "rate_limited", HttpStatus.TOO_MANY_REQUESTS,
            "quota_exceeded", HttpStatus.TOO_MANY_REQUESTS,
            "tpm_exceeded", HttpStatus.TOO_MANY_REQUESTS,
            "unauthorized", HttpStatus.UNAUTHORIZED,
            "injection_detected", HttpStatus.FORBIDDEN,
            "model_not_allowed", HttpStatus.FORBIDDEN,
            "no_available_model", HttpStatus.SERVICE_UNAVAILABLE,
            "llm_invoke_error", HttpStatus.BAD_GATEWAY,
            "prompt_render_error", HttpStatus.BAD_REQUEST
    );

    /**
     * 处理网关业务异常，按错误码映射 HTTP 状态码
     * @param e 网关异常
     * @return 带错误码、correlationId 的统一错误响应
     */
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGatewayException(GatewayException e) {
        Map<String, Object> body = buildErrorBody(
                e.getErrorCode(),
                e.getMessage(),
                HttpStatus.valueOf(e.getStatusCode())
        );
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    /**
     * 处理 Jakarta Bean Validation 校验失败，返回字段级错误详情
     * @param e 校验异常
     * @return 400 响应，包含各字段的具体错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = buildErrorBody("validation_failed", "请求参数校验失败", HttpStatus.BAD_REQUEST);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 兜底处理所有未捕获异常，返回 500 且不泄露堆栈
     * @param e 未知异常
     * @return 500 通用错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        Map<String, Object> body = buildErrorBody("internal_error", "服务内部错误", HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> buildErrorBody(String code, String message, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());
        body.put("correlationId", UUID.randomUUID().toString().substring(0, 8));
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
