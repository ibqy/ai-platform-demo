package com.xb.platform;

import com.xb.platform.gateway.GatewayException;
import com.xb.platform.gateway.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("错误码 → HTTP 状态码映射")
    class ErrorCodeMapping {

        @Test
        @DisplayName("限流类错误码映射为 429")
        void rateLimitCodes() {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("rate_limited"));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("quota_exceeded"));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("tpm_exceeded"));
        }

        @Test
        @DisplayName("鉴权错误码映射为 401")
        void unauthorizedCode() {
            assertEquals(HttpStatus.UNAUTHORIZED,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("unauthorized"));
        }

        @Test
        @DisplayName("安全类错误码映射为 403")
        void securityCodes() {
            assertEquals(HttpStatus.FORBIDDEN,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("injection_detected"));
            assertEquals(HttpStatus.FORBIDDEN,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("model_not_allowed"));
        }

        @Test
        @DisplayName("LLM 调用失败映射为 502")
        void llmError() {
            assertEquals(HttpStatus.BAD_GATEWAY,
                    GlobalExceptionHandler.ERROR_CODE_STATUS.get("llm_invoke_error"));
        }
    }

    @Nested
    @DisplayName("GatewayException 构建")
    class ExceptionBuilding {

        @Test
        @DisplayName("of() 工厂方法正确映射已知错误码")
        void ofKnownCode() {
            GatewayException ex = GatewayException.of("rate_limited");
            assertEquals("rate_limited", ex.getErrorCode());
            assertEquals(429, ex.getStatusCode());
        }

        @Test
        @DisplayName("of() 对未知错误码默认 400")
        void ofUnknownCode() {
            GatewayException ex = GatewayException.of("unknown_error");
            assertEquals("unknown_error", ex.getErrorCode());
            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("handleGatewayException 返回正确的 HTTP 状态")
        void handleReturnsCorrectStatus() {
            GatewayException ex = GatewayException.of("unauthorized");
            var response = handler.handleGatewayException(ex);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("unauthorized", body.get("code"));
            assertNotNull(body.get("correlationId"));
            assertNotNull(body.get("timestamp"));
        }
    }

    @Nested
    @DisplayName("错误响应格式")
    class ErrorFormat {

        @Test
        @DisplayName("错误响应包含所有必需字段")
        void errorBodyContainsAllFields() {
            GatewayException ex = GatewayException.of("quota_exceeded");
            var response = handler.handleGatewayException(ex);
            Map<String, Object> body = response.getBody();

            assertNotNull(body);
            assertEquals("quota_exceeded", body.get("code"));
            assertEquals(429, body.get("status"));
            assertNotNull(body.get("correlationId"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("未知异常返回 500 + 通用消息")
        void unknownExceptionReturns500() {
            var response = handler.handleUnknown(new RuntimeException("secret stack trace"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("internal_error", body.get("code"));
            assertEquals("服务内部错误", body.get("message"));
        }
    }
}
