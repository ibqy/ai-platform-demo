package com.xb.platform.model;

import java.util.HashSet;
import java.util.Set;

public class ModelMeta {

    private String modelId;
    private ModelType type;
    private String provider;
    private String version;
    private String endpoint;
    private String apiKey;
    private int maxContextWindow;
    private double pricePer1kTokens;
    private int maxConcurrency;
    private Set<String> labels = new HashSet<>();
    private boolean healthy = true;

    public ModelMeta() {}

    public ModelMeta(String modelId, ModelType type, String provider, String version,
                     String endpoint, String apiKey, int maxContextWindow,
                     double pricePer1kTokens, int maxConcurrency) {
        this.modelId = modelId;
        this.type = type;
        this.provider = provider;
        this.version = version;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.maxContextWindow = maxContextWindow;
        this.pricePer1kTokens = pricePer1kTokens;
        this.maxConcurrency = maxConcurrency;
    }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public ModelType getType() { return type; }
    public void setType(ModelType type) { this.type = type; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getMaxContextWindow() { return maxContextWindow; }
    public void setMaxContextWindow(int maxContextWindow) { this.maxContextWindow = maxContextWindow; }
    public double getPricePer1kTokens() { return pricePer1kTokens; }
    public void setPricePer1kTokens(double pricePer1kTokens) { this.pricePer1kTokens = pricePer1kTokens; }
    public int getMaxConcurrency() { return maxConcurrency; }
    public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    public Set<String> getLabels() { return labels; }
    public void setLabels(Set<String> labels) { this.labels = labels; }
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }
}