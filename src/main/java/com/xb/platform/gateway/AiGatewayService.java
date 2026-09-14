package com.xb.platform.gateway;

import com.xb.platform.model.*;
import com.xb.platform.tenant.*;
import com.xb.platform.cache.LlmCache;
import com.xb.platform.prompt.PromptRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiGatewayService {

    @Autowired
    private GatewayAuthFilter authFilter;
    @Autowired
    private GatewayRateLimiter rateLimiter;
    @Autowired
    private ContentSafetyFilter safetyFilter;
    @Autowired
    private PiiMasker piiMasker;
    @Autowired
    private LlmCache cache;
    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ModelRouter router;
    @Autowired
    private ModelQuota modelQuota;
    @Autowired
    private TenantRegistry tenantRegistry;
    @Autowired
    private TenantQuotaManager quotaManager;
    @Autowired
    private PromptRenderer promptRenderer;

    public static class AiGatewayRequest {
        public String token;
        public String tenantId;
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

        // 6. 缓存查询
        if (request.modelId != null && request.kbVersion != null) {
            String cached = cache.get(maskedQuery, request.modelId, request.kbVersion);
            if (cached != null) {
                quotaManager.recordUsage(tenantId, 0, 0);
                return AiGatewayResponse.ok(cached, request.modelId, 0, true);
            }
        }

        // 7. 模型路由
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

        // 8. 租户模型白名单
        if (tenant != null && tenant.getModelWhitelist() != null
                && !tenant.getModelWhitelist().isEmpty()
                && !tenant.getModelWhitelist().contains(selectedModelId)) {
            return AiGatewayResponse.error("model_not_allowed");
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

        // 11. 模拟模型调用
        String result = "模拟AI响应: " + maskedQuery;
        if (renderedPrompt != null) {
            result = "模拟AI响应(模板:" + request.templateId + "): " + maskedQuery;
        }

        // 12. 输出过滤
        result = safetyFilter.filterOutput(result);

        // 13. 计量
        long inTokens = request.estimatedTokens > 0 ? request.estimatedTokens : 100;
        long outTokens = inTokens / 2;
        quotaManager.recordUsage(tenantId, (int) inTokens, (int) outTokens);

        // 14. 回写缓存
        if (request.modelId != null && request.kbVersion != null) {
            cache.put(maskedQuery, request.modelId, request.kbVersion, result);
        }

        return AiGatewayResponse.ok(result, selectedModelId, inTokens + outTokens, false);
    }
}