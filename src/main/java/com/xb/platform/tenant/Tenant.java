package com.xb.platform.tenant;

import java.util.Set;

public class Tenant {

    private String tenantId;
    private String name;
    private String status;
    private long expiryEpochMs;
    private Set<String> modelWhitelist;

    public Tenant() {}

    public Tenant(String tenantId, String name, String status, long expiryEpochMs, Set<String> modelWhitelist) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.expiryEpochMs = expiryEpochMs;
        this.modelWhitelist = modelWhitelist;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getExpiryEpochMs() { return expiryEpochMs; }
    public void setExpiryEpochMs(long expiryEpochMs) { this.expiryEpochMs = expiryEpochMs; }
    public Set<String> getModelWhitelist() { return modelWhitelist; }
    public void setModelWhitelist(Set<String> modelWhitelist) { this.modelWhitelist = modelWhitelist; }
}