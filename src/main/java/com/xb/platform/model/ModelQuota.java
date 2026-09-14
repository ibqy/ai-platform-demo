package com.xb.platform.model;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ModelQuota {

    private final ConcurrentHashMap<String, AtomicInteger> tpmCounters = new ConcurrentHashMap<>();
    private static final int DEFAULT_MAX_TPM = 100000;

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
    public void resetCounters() {
        tpmCounters.clear();
    }
}