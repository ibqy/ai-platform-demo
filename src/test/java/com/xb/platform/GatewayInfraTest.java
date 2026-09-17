package com.xb.platform;

import com.xb.platform.gateway.GatewayRateLimiter;
import com.xb.platform.model.ModelQuota;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网关限流与模型配额测试
 *
 * 心得：AI 网关需要两层限流：
 * 1. GatewayRateLimiter — 全局 + 租户维度的 QPM 限流，保护系统不被打爆
 * 2. ModelQuota — 模型维度的 TPM 限流，控制每个模型的 token 消耗成本
 * 两者互补：前者保护系统稳定性，后者控制成本。
 */
class GatewayInfraTest {

    @Test
    void testRateLimiterPerTenantLimit() throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter();
        setField(limiter, "globalMax", 10000);
        setField(limiter, "perTenantMax", 5);

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("tenant-a"), "第 " + (i + 1) + " 次应通过");
        }
        assertFalse(limiter.tryAcquire("tenant-a"), "第 6 次应被限流");
    }

    @Test
    void testRateLimiterTenantIsolation() throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter();
        setField(limiter, "globalMax", 10000);
        setField(limiter, "perTenantMax", 3);

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("tenant-a"));
        }
        assertFalse(limiter.tryAcquire("tenant-a"));

        assertTrue(limiter.tryAcquire("tenant-b"), "tenant-b 应有独立配额");
    }

    @Test
    void testRateLimiterGlobalLimit() throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter();
        setField(limiter, "globalMax", 5);
        setField(limiter, "perTenantMax", 100);

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("tenant-" + i), "全局第 " + (i + 1) + " 次应通过");
        }
        assertFalse(limiter.tryAcquire("tenant-x"), "超出全局限制应拒绝");
    }

    @Test
    void testRateLimiterReset() throws Exception {
        GatewayRateLimiter limiter = new GatewayRateLimiter();
        setField(limiter, "globalMax", 2);
        setField(limiter, "perTenantMax", 2);

        assertTrue(limiter.tryAcquire("t1"));
        assertTrue(limiter.tryAcquire("t1"));
        assertFalse(limiter.tryAcquire("t1"));

        limiter.reset();

        assertTrue(limiter.tryAcquire("t1"), "重置后应重新可用");
    }

    @Test
    void testModelQuotaAcquire() {
        ModelQuota quota = new ModelQuota();

        assertTrue(quota.tryAcquire("gpt-4o", 50000));
        assertTrue(quota.tryAcquire("gpt-4o", 50000));
        assertFalse(quota.tryAcquire("gpt-4o", 1), "超过 100000 TPM 应拒绝");
    }

    @Test
    void testModelQuotaPerModel() {
        ModelQuota quota = new ModelQuota();

        assertTrue(quota.tryAcquire("gpt-4o", 90000));
        assertFalse(quota.tryAcquire("gpt-4o", 20000));

        assertTrue(quota.tryAcquire("qwen-turbo", 90000), "不同模型应有独立配额");
    }

    @Test
    void testModelQuotaReset() {
        ModelQuota quota = new ModelQuota();
        quota.tryAcquire("gpt-4o", 100000);
        assertFalse(quota.tryAcquire("gpt-4o", 1));

        quota.resetCounters();
        assertTrue(quota.tryAcquire("gpt-4o", 1), "重置后应重新可用");
    }

    private void setField(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }
}
