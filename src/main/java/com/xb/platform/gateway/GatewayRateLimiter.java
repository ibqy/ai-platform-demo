package com.xb.platform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GatewayRateLimiter {

    @Value("${platform.gateway.rate-limit.global:1000}")
    private int globalMax;

    @Value("${platform.gateway.rate-limit.per-tenant:100}")
    private int perTenantMax;

    private final AtomicInteger globalCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> tenantCounters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String tenantId) {
        if (globalCounter.incrementAndGet() > globalMax) {
            return false;
        }
        AtomicInteger tenantCounter = tenantCounters.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        return tenantCounter.incrementAndGet() <= perTenantMax;
    }

    @Scheduled(fixedRate = 60000)
    public void reset() {
        globalCounter.set(0);
        tenantCounters.clear();
    }
}