package com.xb.platform.model;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ModelQuota - 模型 TPM（Tokens Per Minute）配额管理
 *
 * 演示模型维度的流量控制：每个模型有独立的 TPM 上限，
 * 超出后拒绝请求以控制成本。与 GatewayRateLimiter 的 QPM 限流互补：
 * QPM 保护系统稳定性，TPM 控制模型调用成本。
 *
 * @author ibqy
 */
@Component
public class ModelQuota {

    private final ConcurrentHashMap<String, AtomicInteger> tpmCounters = new ConcurrentHashMap<>();
    private static final int DEFAULT_MAX_TPM = 100000;

    /**
     * 尝试获取模型的 TPM 配额
     * @param modelId 模型 ID
     * @param tokens 预估消耗 Token 数
     * @return 配额充足返回 true，超限返回 false
     */
    public boolean tryAcquire(String modelId, int tokens) {
        AtomicInteger counter = tpmCounters.computeIfAbsent(modelId, k -> new AtomicInteger(0));
        int current = counter.get();
        if (current + tokens > DEFAULT_MAX_TPM) {
            return false;
        }
        counter.addAndGet(tokens);
        return true;
    }

    @Scheduled(fixedRate = 60000)
    /** 每分钟重置所有模型的 TPM 计数器 */
    public void resetCounters() {
        tpmCounters.clear();
    }
}