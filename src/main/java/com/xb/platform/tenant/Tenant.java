package com.xb.platform.tenant;

import java.util.Set;

/**
 * Tenant - 租户实体
 *
 * 描述 AI 平台中一个租户的完整信息：租户 ID、名称、状态、过期时间、
 * 模型白名单。模型白名单用于控制租户可访问的模型范围，
 * 是多租户 AI 平台实现差异化服务的关键配置。
 *
 * @author ibqy
 */
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