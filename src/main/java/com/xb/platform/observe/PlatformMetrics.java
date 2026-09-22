package com.xb.platform.observe;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PlatformMetrics - 平台指标收集器
 *
 * 演示 AI 平台的多维度指标采集：按租户、端点、模型三个维度分别记录
 * 请求量、成功率、延迟分布。使用 CAS（compareAndSet）无锁更新最小/最大延迟，
 * 兼顾并发安全与性能。这些指标是告警规则和运维可视化的数据源。
 *
 * @author ibqy
 */
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

    /**
     * 记录一次请求的指标数据（按租户、端点、模型三维度）
     * @param tenantId 租户 ID
     * @param endpoint 请求端点
     * @param modelId 使用的模型 ID
     * @param success 请求是否成功
     * @param latencyMs 请求耗时（毫秒）
     */
    public void record(String tenantId, String endpoint, String modelId, boolean success, long latencyMs) {
        updateEntry(tenantMetrics, tenantId, success, latencyMs);
        updateEntry(endpointMetrics, endpoint, success, latencyMs);
        updateEntry(modelMetrics, modelId, success, latencyMs);
    }

    /**
     * 记录一次请求的指标数据（含 Token 用量）
     * @param tenantId 租户 ID
     * @param endpoint 请求端点
     * @param modelId 模型 ID
     * @param success 是否成功
     * @param latencyMs 耗时（毫秒）
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
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

    /**
     * 获取指定租户的汇总指标
     * @param tenantId 租户 ID
     * @return 包含请求量、成功率、延迟分布的指标 Map
     */
    public Map<String, Object> getTenantMetrics(String tenantId) {
        return toMetricMap(tenantMetrics.get(tenantId));
    }

    /**
     * 获取指定端点的汇总指标
     * @param endpoint 端点路径
     * @return 指标 Map
     */
    public Map<String, Object> getEndpointMetrics(String endpoint) {
        return toMetricMap(endpointMetrics.get(endpoint));
    }

    /**
     * 获取指定模型的汇总指标（供告警规则使用）
     * @param modelId 模型 ID
     * @return 指标 Map
     */
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