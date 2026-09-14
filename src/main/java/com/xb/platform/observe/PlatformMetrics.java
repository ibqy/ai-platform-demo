package com.xb.platform.observe;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlatformMetrics {

    static class MetricEntry {
        final AtomicLong totalRequests = new AtomicLong();
        final AtomicLong successCount = new AtomicLong();
        final AtomicLong errorCount = new AtomicLong();
        final AtomicLong totalLatencyMs = new AtomicLong();
        final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxLatencyMs = new AtomicLong(Long.MIN_VALUE);
    }

    private final ConcurrentHashMap<String, MetricEntry> tenantMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MetricEntry> endpointMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MetricEntry> modelMetrics = new ConcurrentHashMap<>();

    public void record(String tenantId, String endpoint, String modelId, boolean success, long latencyMs) {
        updateEntry(tenantMetrics, tenantId, success, latencyMs);
        updateEntry(endpointMetrics, endpoint, success, latencyMs);
        updateEntry(modelMetrics, modelId, success, latencyMs);
    }

    public void record(String tenantId, String endpoint, String modelId, boolean success, long latencyMs,
                       int inputTokens, int outputTokens) {
        record(tenantId, endpoint, modelId, success, latencyMs);
    }

    private void updateEntry(ConcurrentHashMap<String, MetricEntry> map, String key, boolean success, long latencyMs) {
        MetricEntry e = map.computeIfAbsent(key, k -> new MetricEntry());
        e.totalRequests.incrementAndGet();
        if (success) {
            e.successCount.incrementAndGet();
        } else {
            e.errorCount.incrementAndGet();
        }
        e.totalLatencyMs.addAndGet(latencyMs);

        // CAS update min
        long minVal = e.minLatencyMs.get();
        while (latencyMs < minVal) {
            if (e.minLatencyMs.compareAndSet(minVal, latencyMs)) break;
            minVal = e.minLatencyMs.get();
        }

        // CAS update max
        long maxVal = e.maxLatencyMs.get();
        while (latencyMs > maxVal) {
            if (e.maxLatencyMs.compareAndSet(maxVal, latencyMs)) break;
            maxVal = e.maxLatencyMs.get();
        }
    }

    public Map<String, Object> getTenantMetrics(String tenantId) {
        return toMetricMap(tenantMetrics.get(tenantId));
    }

    public Map<String, Object> getEndpointMetrics(String endpoint) {
        return toMetricMap(endpointMetrics.get(endpoint));
    }

    public Map<String, Object> getModelMetrics(String modelId) {
        return toMetricMap(modelMetrics.get(modelId));
    }

    private Map<String, Object> toMetricMap(MetricEntry e) {
        Map<String, Object> m = new HashMap<>();
        if (e == null) {
            m.put("totalRequests", 0);
            m.put("successRate", 0.0);
            m.put("avgLatencyMs", 0.0);
            return m;
        }
        m.put("totalRequests", e.totalRequests.get());
        double rate = e.totalRequests.get() > 0
                ? Math.round(e.successCount.get() * 10000.0 / e.totalRequests.get()) / 100.0
                : 0.0;
        m.put("successRate", rate);
        double avg = e.totalRequests.get() > 0
                ? e.totalLatencyMs.get() / (double) e.totalRequests.get()
                : 0.0;
        m.put("avgLatencyMs", Math.round(avg * 100.0) / 100.0);
        m.put("minLatencyMs", e.totalRequests.get() > 0 ? e.minLatencyMs.get() : 0);
        m.put("maxLatencyMs", e.totalRequests.get() > 0 ? e.maxLatencyMs.get() : 0);
        return m;
    }
}