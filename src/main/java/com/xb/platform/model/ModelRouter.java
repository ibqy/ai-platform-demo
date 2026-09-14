package com.xb.platform.model;

import java.util.List;
import java.util.Map;

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

    public RouteResult selectModel(RouteRequest req, List<ModelMeta> candidates) {
        return selectModel(req, candidates, defaultStrategy);
    }

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