package com.xb.platform.observe;

import com.xb.platform.model.ModelMeta;
import com.xb.platform.model.ModelRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TokenMeter {

    @Autowired
    private ModelRegistry registry;

    static class Usage {
        final AtomicLong inputTokens = new AtomicLong();
        final AtomicLong outputTokens = new AtomicLong();
        final AtomicLong costMicro = new AtomicLong();
    }

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Usage>> meter = new ConcurrentHashMap<>();

    public void record(String tenantId, String modelId, int inputTokens, int outputTokens) {
        Usage u = meter.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(modelId, k -> new Usage());
        u.inputTokens.addAndGet(inputTokens);
        u.outputTokens.addAndGet(outputTokens);

        ModelMeta meta = registry.get(modelId);
        if (meta != null) {
            double cost = (double) (inputTokens + outputTokens) * meta.getPricePer1kTokens() / 1000.0;
            u.costMicro.addAndGet((long) (cost * 10000));
        }
    }

    public double getCost(String tenantId, String modelId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0.0;
        Usage u = tenantUsage.get(modelId);
        return u != null ? u.costMicro.get() / 10000.0 : 0.0;
    }

    public long getTotalInputTokens(String tenantId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0;
        return tenantUsage.values().stream().mapToLong(u -> u.inputTokens.get()).sum();
    }

    public long getTotalOutputTokens(String tenantId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0;
        return tenantUsage.values().stream().mapToLong(u -> u.outputTokens.get()).sum();
    }

    public Map<String, Object> getTenantReport(String tenantId) {
        Map<String, Object> report = new HashMap<>();
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return report;

        long totalIn = 0, totalOut = 0;
        double totalCost = 0;

        for (Map.Entry<String, Usage> entry : tenantUsage.entrySet()) {
            Usage u = entry.getValue();
            Map<String, Object> detail = new HashMap<>();
            detail.put("inputTokens", u.inputTokens.get());
            detail.put("outputTokens", u.outputTokens.get());
            double cost = u.costMicro.get() / 10000.0;
            detail.put("cost", Math.round(cost * 10000.0) / 10000.0);
            report.put(entry.getKey(), detail);
            totalIn += u.inputTokens.get();
            totalOut += u.outputTokens.get();
            totalCost += cost;
        }

        report.put("_totalInputTokens", totalIn);
        report.put("_totalOutputTokens", totalOut);
        report.put("_totalCost", Math.round(totalCost * 10000.0) / 10000.0);
        return report;
    }
}