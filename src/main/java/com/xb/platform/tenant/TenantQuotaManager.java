package com.xb.platform.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * TenantQuotaManager - 租户配额管理器
 *
 * 演示多租户 AI 平台的配额控制：每个租户有每日请求数和 Token 用量上限，
 * 跨日自动重置。与 GatewayRateLimiter 的分钟级限流互补：
 * 限流器保护系统瞬时不崩溃，配额管理器控制长期成本。
 *
 * @author ibqy
 */
@Component
public class TenantQuotaManager {

    @Autowired
    private TenantRegistry registry;

    private final ConcurrentHashMap<String, TenantUsage> usages = new ConcurrentHashMap<>();

    private static final long DEFAULT_DAILY_REQUESTS = 10000;
    private static final long DEFAULT_DAILY_TOKENS = 1_000_000;

    /**
     * 检查租户当日配额是否充足（请求数 + Token 数双重校验）
     * @param tenantId 租户 ID
     * @return 配额充足返回 true，超限返回 false
     */
    public boolean checkQuota(String tenantId) {
        TenantUsage usage = getOrCreate(tenantId);
        maybeReset(usage);
        return usage.getDailyRequests().get() < DEFAULT_DAILY_REQUESTS
                && usage.getDailyInputTokens().get() + usage.getDailyOutputTokens().get() < DEFAULT_DAILY_TOKENS;
    }

    /**
     * 记录一次请求的用量（请求数 + Token 数）
     * @param tenantId 租户 ID
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
    public void recordUsage(String tenantId, int inputTokens, int outputTokens) {
        TenantUsage usage = getOrCreate(tenantId);
        maybeReset(usage);
        usage.getDailyRequests().incrementAndGet();
        usage.getDailyInputTokens().addAndGet(inputTokens);
        usage.getDailyOutputTokens().addAndGet(outputTokens);
    }

    private TenantUsage getOrCreate(String tenantId) {
        return usages.computeIfAbsent(tenantId, k -> new TenantUsage());
    }

    private void maybeReset(TenantUsage usage) {
        long now = System.currentTimeMillis();
        long dayMs = 24 * 60 * 60 * 1000L;
        if (now - usage.getResetTimeEpochMs() > dayMs) {
            usage.reset();
            usage.setResetTimeEpochMs(now);
        }
    }
}