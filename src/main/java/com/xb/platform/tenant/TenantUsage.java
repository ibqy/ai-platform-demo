package com.xb.platform.tenant;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 租户用量统计 —— 记录单个租户的每日 API 调用量与 Token 消耗
 *
 * <p>使用 AtomicLong 保证并发安全，网关限流过滤器每次请求时原子递增计数。</p>
 *
 * @author ibqy
 */
public class TenantUsage {

    private final AtomicLong dailyRequests = new AtomicLong();
    private final AtomicLong dailyInputTokens = new AtomicLong();
    private final AtomicLong dailyOutputTokens = new AtomicLong();
    private long resetTimeEpochMs = System.currentTimeMillis();

    public AtomicLong getDailyRequests() { return dailyRequests; }
    public AtomicLong getDailyInputTokens() { return dailyInputTokens; }
    public AtomicLong getDailyOutputTokens() { return dailyOutputTokens; }
    public long getResetTimeEpochMs() { return resetTimeEpochMs; }
    public void setResetTimeEpochMs(long t) { this.resetTimeEpochMs = t; }

    public void reset() {
        dailyRequests.set(0);
        dailyInputTokens.set(0);
        dailyOutputTokens.set(0);
    }
}