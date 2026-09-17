package com.xb.platform.gateway;

import com.xb.platform.gateway.AiGatewayService.AiGatewayRequest;
import com.xb.platform.gateway.AiGatewayService.AiGatewayResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 网关控制器
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p><b>高阶知识点</b>：
 * <ul>
 *     <li>{@code POST /api/ai/chat} —— 同步对话（完整响应）</li>
 *     <li>{@code POST /api/ai/chat/stream} —— SSE 流式输出（逐 Token 推送）</li>
 *     <li>{@code @Valid} 触发 Jakarta Bean Validation，校验失败由 {@link GlobalExceptionHandler} 处理</li>
 *     <li>SSE 流式输出是生产 AI 网关的标配——用户无需等待完整回答，体验接近 ChatGPT</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
public class AiGatewayController {

    @Autowired
    private AiGatewayService gatewayService;

    private final ExecutorService sseExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 同步对话 —— 等待 LLM 完整回答后一次性返回
     */
    @PostMapping("/chat")
    public ResponseEntity<AiGatewayResponse> chat(@Valid @RequestBody AiGatewayRequest request) {
        AiGatewayResponse response = gatewayService.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * SSE 流式对话 —— 逐 Token 推送，模拟 ChatGPT 打字机效果
     *
     * <p><b>实现原理</b>：
     * <ol>
     *     <li>创建 SseEmitter（超时 60s），立即返回给客户端</li>
     *     <li>在虚拟线程中调用 LLM 流式 API，每收到一个 chunk 就 send 一个 SSE 事件</li>
     *     <li>结束时发送 [DONE] 标记，客户端据此关闭连接</li>
     * </ol>
     *
     * <p><b>生产注意</b>：
     * <ul>
     *     <li>需要网关/代理（Nginx）关闭 buffering，否则 SSE 会被攒批发送</li>
     *     <li>超时时间应根据模型最大输出长度动态调整</li>
     *     <li>需要处理客户端提前断开连接（onCompletion / onError 回调）</li>
     * </ul>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiGatewayRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        sseExecutor.submit(() -> {
            try {
                gatewayService.processStream(request, chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
