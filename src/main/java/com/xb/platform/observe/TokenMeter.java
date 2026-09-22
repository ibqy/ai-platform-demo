package com.xb.platform.observe;

import com.xb.platform.model.ModelMeta;
import com.xb.platform.model.ModelRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TokenMeter - Token 计量与成本核算器
 *
 * 演示 AI 平台的成本控制中心：每次 LLM 调用都按模型单价记账，
 * 以租户 x 模型为维度记录输入/输出 Token 数及费用。
 * 用 AtomicLong 保证并发安全，支持生成租户级别的详细费用报告。
 *
 * @author ibqy
 */
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

    /**
     * 记录一次 LLM 调用的 Token 用量及费用
     * @param tenantId 租户 ID
     * @param modelId 模型 ID
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
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

    /**
     * 查询指定租户在某模型上的累计费用
     * @param tenantId 租户 ID
     * @param modelId 模型 ID
     * @return 累计费用（单位：元）
     */
    public double getCost(String tenantId, String modelId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0.0;
        Usage u = tenantUsage.get(modelId);
        return u != null ? u.costMicro.get() / 10000.0 : 0.0;
    }

    /**
     * 查询指定租户的总输入 Token 数（跨模型汇总）
     * @param tenantId 租户 ID
     * @return 总输入 Token 数
     */
    public long getTotalInputTokens(String tenantId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0;
        return tenantUsage.values().stream().mapToLong(u -> u.inputTokens.get()).sum();
    }

    /**
     * 查询指定租户的总输出 Token 数（跨模型汇总）
     * @param tenantId 租户 ID
     * @return 总输出 Token 数
     */
    public long getTotalOutputTokens(String tenantId) {
        ConcurrentHashMap<String, Usage> tenantUsage = meter.get(tenantId);
        if (tenantUsage == null) return 0;
        return tenantUsage.values().stream().mapToLong(u -> u.outputTokens.get()).sum();
    }

    /**
     * 生成指定租户的详细费用报告（按模型分组 + 汇总）
     * @param tenantId 租户 ID
     * @return 包含各模型明细和汇总的 Map
     */
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