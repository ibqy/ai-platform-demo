package com.xb.platform.tenant;

import java.util.concurrent.atomic.AtomicLong;

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