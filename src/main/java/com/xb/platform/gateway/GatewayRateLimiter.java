package com.xb.platform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GatewayRateLimiter - AI 网关双层限流器
 *
 * 演示 AI 网关的全局 + 租户双层限流策略：全局计数器保护系统不被打爆，
 * 租户计数器防止单一租户独占资源。使用 AtomicInteger 保证并发安全，
 * 定时任务每分钟重置计数器，实现滑动窗口式限流。
 *
 * @author ibqy
 */
@Component
public class GatewayRateLimiter {

    @Value("${platform.gateway.rate-limit.global:1000}")
    private int globalMax;

    @Value("${platform.gateway.rate-limit.per-tenant:100}")
    private int perTenantMax;

    private final AtomicInteger globalCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> tenantCounters = new ConcurrentHashMap<>();

    /**
     * 尝试获取请求配额（全局 + 租户双重检查）
     * @param tenantId 租户标识
     * @return 配额充足返回 true，超限返回 false
     */
    public boolean tryAcquire(String tenantId) {
        if (globalCounter.incrementAndGet() > globalMax) {
            return false;
        }
        AtomicInteger tenantCounter = tenantCounters.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        return tenantCounter.incrementAndGet() <= perTenantMax;
    }

    @Scheduled(fixedRate = 60000)
    /** 每分钟重置所有计数器（由 @Scheduled 驱动） */
    public void reset() {
        globalCounter.set(0);
        tenantCounters.clear();
    }
}