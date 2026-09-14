package com.xb.platform.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantQuotaManager {

    @Autowired
    private TenantRegistry registry;

    private final ConcurrentHashMap<String, TenantUsage> usages = new ConcurrentHashMap<>();

    private static final long DEFAULT_DAILY_REQUESTS = 10000;
    private static final long DEFAULT_DAILY_TOKENS = 1_000_000;

    public boolean checkQuota(String tenantId) {
        TenantUsage usage = getOrCreate(tenantId);
        maybeReset(usage);
        return usage.getDailyRequests().get() < DEFAULT_DAILY_REQUESTS
                && usage.getDailyInputTokens().get() + usage.getDailyOutputTokens().get() < DEFAULT_DAILY_TOKENS;
    }

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