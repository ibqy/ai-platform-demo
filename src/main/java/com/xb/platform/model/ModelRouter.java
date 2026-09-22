package com.xb.platform.model;

import java.util.List;
import java.util.Map;

/**
 * ModelRouter - 智能模型路由器
 *
 * 演示 AI 网关的多策略模型路由：支持按成本（COST）、性能（PERFORMANCE）、
 * 质量（QUALITY）三种策略选择最优模型。还提供 A/B 测试流量分割能力，
 * 可按租户哈希将流量分配到不同模型组，实现灰度发布。
 *
 * @author ibqy
 */
public class ModelRouter {

    public enum RouterStrategy {
        COST,
        PERFORMANCE,
        QUALITY
    }

    public RouterStrategy defaultStrategy = RouterStrategy.COST;

    public void setDefaultStrategy(RouterStrategy strategy) {
        this.defaultStrategy = strategy;
    }

    public static class RouteRequest {
        private final String tenantId;
        private final String query;
        private final ModelType type;
        private final int estimatedTokens;
        private final Map<String, String> labels;

        public RouteRequest(String tenantId, String query, ModelType type,
                            int estimatedTokens, Map<String, String> labels) {
            this.tenantId = tenantId;
            this.query = query;
            this.type = type;
            this.estimatedTokens = estimatedTokens;
            this.labels = labels;
        }

        public String getTenantId() { return tenantId; }
        public String getQuery() { return query; }
        public ModelType getType() { return type; }
        public int getEstimatedTokens() { return estimatedTokens; }
        public Map<String, String> getLabels() { return labels; }
    }

    public static class RouteResult {
        private final String modelId;
        private final ModelMeta meta;
        private final String strategy;

        public RouteResult(String modelId, ModelMeta meta, String strategy) {
            this.modelId = modelId;
            this.meta = meta;
            this.strategy = strategy;
        }

        public String getModelId() { return modelId; }
        public ModelMeta getMeta() { return meta; }
        public String getStrategy() { return strategy; }
    }

    /**
     * 使用默认策略从候选模型中选择最优模型
     * @param req 路由请求
     * @param candidates 候选模型列表
     * @return 选中的模型结果，无健康模型返回 null
     */
    public RouteResult selectModel(RouteRequest req, List<ModelMeta> candidates) {
        return selectModel(req, candidates, defaultStrategy);
    }

    /**
     * 按指定策略从健康候选模型中选择最优模型
     * @param req 路由请求
     * @param candidates 候选模型列表
     * @param strategy 路由策略（COST/PERFORMANCE/QUALITY）
     * @return 选中的模型结果，无健康模型返回 null
     */
    public RouteResult selectModel(RouteRequest req, List<ModelMeta> candidates, RouterStrategy strategy) {
        List<ModelMeta> healthy = candidates.stream()
                .filter(ModelMeta::isHealthy)
                .toList();

        if (healthy.isEmpty()) {
            return null;
        }

        ModelMeta selected;
        switch (strategy) {
            case COST:
                selected = healthy.stream()
                        .min((a, b) -> Double.compare(a.getPricePer1kTokens(), b.getPricePer1kTokens()))
                        .orElse(healthy.get(0));
                break;
            case PERFORMANCE:
                selected = healthy.get(0);
                break;
            case QUALITY:
                selected = healthy.stream()
                        .max((a, b) -> Integer.compare(a.getMaxContextWindow(), b.getMaxContextWindow()))
                        .orElse(healthy.get(0));
                break;
            default:
                selected = healthy.get(0);
        }

        return new RouteResult(selected.getModelId(), selected, strategy.name());
    }

    /**
     * A/B 测试流量分割：按租户哈希分配流量到两组模型
     * @param tenantId 租户 ID（用于哈希分流）
     * @param groupA A 组候选模型
     * @param groupB B 组候选模型
     * @param trafficPercentA A 组流量百分比（0-100）
     * @return 路由结果，策略标记包含 AB_A_ 或 AB_B_ 前缀
     */
    public RouteResult selectModelWithAB(String tenantId, List<ModelMeta> groupA,
                                          List<ModelMeta> groupB, int trafficPercentA) {
        int hash = Math.abs(tenantId.hashCode() % 100);
        boolean useA = hash < trafficPercentA;

        RouteRequest req = new RouteRequest(tenantId, "", null, 0, Map.of());
        if (useA) {
            RouteResult r = selectModel(req, groupA);
            return r != null ? new RouteResult(r.getModelId(), r.getMeta(), "AB_A_" + defaultStrategy) : null;
        } else {
            RouteResult r = selectModel(req, groupB);
            return r != null ? new RouteResult(r.getModelId(), r.getMeta(), "AB_B_" + defaultStrategy) : null;
        }
    }
}