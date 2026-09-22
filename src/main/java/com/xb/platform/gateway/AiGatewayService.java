package com.xb.platform.gateway;

import com.xb.platform.model.*;
import com.xb.platform.tenant.*;
import com.xb.platform.cache.LlmCache;
import com.xb.platform.prompt.PromptRenderer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 网关服务 —— 14 步管道编排
 *
 * <p>作者：xb | 日期：2026-09</p>
 *
 * <p>管道顺序从便宜到贵排列：被挡掉的请求不消耗任何模型调用费用。</p>
 *
 * @author ibqy
 */
@Service
public class AiGatewayService {

    private final GatewayAuthFilter authFilter;
    private final GatewayRateLimiter rateLimiter;
    private final ContentSafetyFilter safetyFilter;
    private final PiiMasker piiMasker;
    private final LlmCache cache;
    private final ModelRegistry registry;
    private final ModelRouter router;
    private final ModelQuota modelQuota;
    private final TenantRegistry tenantRegistry;
    private final TenantQuotaManager quotaManager;
    private final PromptRenderer promptRenderer;
    private final ChatClient.Builder chatClientBuilder;

    @Autowired
    public AiGatewayService(GatewayAuthFilter authFilter,
                            GatewayRateLimiter rateLimiter,
                            ContentSafetyFilter safetyFilter,
                            PiiMasker piiMasker,
                            LlmCache cache,
                            ModelRegistry registry,
                            ModelRouter router,
                            ModelQuota modelQuota,
                            TenantRegistry tenantRegistry,
                            TenantQuotaManager quotaManager,
                            PromptRenderer promptRenderer,
                            ChatClient.Builder chatClientBuilder) {
        this.authFilter = authFilter;
        this.rateLimiter = rateLimiter;
        this.safetyFilter = safetyFilter;
        this.piiMasker = piiMasker;
        this.cache = cache;
        this.registry = registry;
        this.router = router;
        this.modelQuota = modelQuota;
        this.tenantRegistry = tenantRegistry;
        this.quotaManager = quotaManager;
        this.promptRenderer = promptRenderer;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 网关请求 DTO（带 Jakarta Validation 校验）
     *
     * <p><b>高阶知识点</b>：
     * <ul>
     *     <li>{@code @NotBlank} —— 字符串不能为 null 且 trim 后长度 > 0</li>
     *     <li>{@code @NotNull} —— 对象不能为 null</li>
     *     <li>message 属性支持 i18n，生产环境可抽取到 messages.properties</li>
     *     <li>校验由 Controller 的 {@code @Valid} 触发，失败由 {@link GlobalExceptionHandler} 统一处理</li>
     * </ul>
     */
    public static class AiGatewayRequest {
        @NotBlank(message = "token 不能为空")
        public String token;

        @NotBlank(message = "tenantId 不能为空")
        public String tenantId;

        @NotBlank(message = "query 不能为空")
        public String query;

        public String modelId;
        public String kbVersion;
        public String templateId;
        public Map<String, String> templateParams;
        public int estimatedTokens;
    }

    public static class AiGatewayResponse {
        public String result;
        public String modelId;
        public long costTokens;
        public boolean cacheHit;
        public String errorCode;

        public static AiGatewayResponse error(String code) {
            AiGatewayResponse r = new AiGatewayResponse();
            r.errorCode = code;
            return r;
        }

        public static AiGatewayResponse ok(String result, String modelId, long tokens, boolean hit) {
            AiGatewayResponse r = new AiGatewayResponse();
            r.result = result;
            r.modelId = modelId;
            r.costTokens = tokens;
            r.cacheHit = hit;
            return r;
        }
    }

    public AiGatewayResponse process(AiGatewayRequest request) {
        String tenantId = request.tenantId;

        // 1. 限流
        if (!rateLimiter.tryAcquire(tenantId)) {
            return AiGatewayResponse.error("rate_limited");
        }

        // 2. 租户配额
        if (!quotaManager.checkQuota(tenantId)) {
            return AiGatewayResponse.error("quota_exceeded");
        }

        // 3. JWT鉴权
        if (!authFilter.authenticate(request.token)) {
            return AiGatewayResponse.error("unauthorized");
        }

        // 4. 注入检测
        String injection = safetyFilter.checkInput(request.query);
        if (injection != null) {
            return AiGatewayResponse.error("injection_detected");
        }

        // 5. PII脱敏
        String maskedQuery = piiMasker.mask(request.query);

        // 6. 模型路由（缓存前完成路由与鉴权，防止缓存绕过权限）
        Tenant tenant = tenantRegistry.get(tenantId);
        List<ModelMeta> candidates = registry.listByType(ModelType.CHAT);
        ModelRouter.RouteRequest routeReq = new ModelRouter.RouteRequest(
                tenantId, maskedQuery, ModelType.CHAT, request.estimatedTokens, Map.of()
        );
        ModelRouter.RouteResult routeResult = router.selectModel(routeReq, candidates);
        if (routeResult == null) {
            return AiGatewayResponse.error("no_available_model");
        }
        String selectedModelId = routeResult.getModelId();

        // 7. 租户模型白名单
        if (tenant != null && tenant.getModelWhitelist() != null
                && !tenant.getModelWhitelist().isEmpty()
                && !tenant.getModelWhitelist().contains(selectedModelId)) {
            return AiGatewayResponse.error("model_not_allowed");
        }

        // 8. 缓存查询（在鉴权与白名单之后，携带 tenantId 隔离租户）
        if (request.modelId != null && request.kbVersion != null) {
            String cached = cache.get(tenantId, maskedQuery, request.modelId, request.kbVersion);
            if (cached != null) {
                quotaManager.recordUsage(tenantId, 0, 0);
                return AiGatewayResponse.ok(cached, request.modelId, 0, true);
            }
        }

        // 9. 模型TPM
        if (request.estimatedTokens > 0 && !modelQuota.tryAcquire(selectedModelId, request.estimatedTokens)) {
            return AiGatewayResponse.error("tpm_exceeded");
        }

        // 10. Prompt模板渲染
        String renderedPrompt = null;
        if (request.templateId != null && request.templateParams != null) {
            try {
                renderedPrompt = promptRenderer.render(request.templateId, tenantId, request.templateParams);
            } catch (Exception e) {
                return AiGatewayResponse.error("prompt_render_error");
            }
        }

        // 11. LLM 调用
        ChatClient chatClient = chatClientBuilder.build();
        String userMessage = renderedPrompt != null ? renderedPrompt : maskedQuery;
        String result;
        try {
            result = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            return AiGatewayResponse.error("llm_invoke_error");
        }

        // 12. 输出过滤
        result = safetyFilter.filterOutput(result);

        // 13. 计量
        long inTokens = request.estimatedTokens > 0 ? request.estimatedTokens : 100;
        long outTokens = inTokens / 2;
        quotaManager.recordUsage(tenantId, (int) inTokens, (int) outTokens);

        // 14. 回写缓存
        if (request.modelId != null && request.kbVersion != null) {
            cache.put(tenantId, maskedQuery, request.modelId, request.kbVersion, result);
        }

        return AiGatewayResponse.ok(result, selectedModelId, inTokens + outTokens, false);
    }

    /**
     * 流式处理 —— SSE 逐 Token 推送
     *
     * <p><b>与同步 process 的区别</b>：
     * <ul>
     *     <li>前置管道（限流/鉴权/注入检测/PII）完全复用——安全检查不因为是流式就跳过</li>
     *     <li>LLM 调用使用 {@code stream()} 而非 {@code call()}，返回 Flux 逐 chunk 消费</li>
     *     <li>每个 chunk 通过 Consumer 回调推送给 SSE emitter</li>
     *     <li>流式模式不写缓存（回答是碎片化的，无法直接缓存）</li>
     * </ul>
     *
     * <p><b>教学简化</b>：由于无真实 LLM 连接，此处模拟逐字符推送。</p>
     */
    public void processStream(AiGatewayRequest request, Consumer<String> chunkConsumer) {
        String tenantId = request.tenantId;

        // 前置管道（1-5 步）与同步模式完全一致
        if (!rateLimiter.tryAcquire(tenantId)) {
            throw GatewayException.of("rate_limited");
        }
        if (!quotaManager.checkQuota(tenantId)) {
            throw GatewayException.of("quota_exceeded");
        }
        if (!authFilter.authenticate(request.token)) {
            throw GatewayException.of("unauthorized");
        }
        String injection = safetyFilter.checkInput(request.query);
        if (injection != null) {
            throw GatewayException.of("injection_detected");
        }
        String maskedQuery = piiMasker.mask(request.query);

        // LLM 流式调用（教学模拟：逐字符推送）
        // 生产环境替换为：chatClient.prompt().user(maskedQuery).stream().content()
        //     .doOnNext(chunkConsumer)
        //     .blockLast();
        String simulatedResponse = "[流式响应] 这是模型对「" + maskedQuery + "」的回答。"
                + "生产环境中，每个 Token 会通过 SSE 实时推送到客户端，"
                + "实现类似 ChatGPT 的打字机效果。";

        for (char c : simulatedResponse.toCharArray()) {
            chunkConsumer.accept(String.valueOf(c));
        }

        // 计量
        long inTokens = request.estimatedTokens > 0 ? request.estimatedTokens : 100;
        long outTokens = inTokens / 2;
        quotaManager.recordUsage(tenantId, (int) inTokens, (int) outTokens);
    }
}
