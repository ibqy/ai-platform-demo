package com.xb.platform.tenant;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TenantRegistry - 租户注册中心
 *
 * 演示多租户平台的租户管理：使用 ConcurrentHashMap 存储租户信息，
 * 支持注册、查询、列表操作。生产环境通常对接数据库或配置中心，
 * 此处教学简化为内存存储。
 *
 * @author ibqy
 */
@Component
public class TenantRegistry {

    private final ConcurrentHashMap<String, Tenant> tenants = new ConcurrentHashMap<>();

    /**
     * 获取指定租户信息
     * @param tenantId 租户 ID
     * @return 租户实体，不存在返回 null
     */
    public Tenant get(String tenantId) {
        return tenants.get(tenantId);
    }

    /**
     * 注册新租户
     * @param tenant 租户实体
     */
    public void register(Tenant tenant) {
        tenants.put(tenant.getTenantId(), tenant);
    }

    /** 列出所有已注册租户 */
    public List<Tenant> listAll() {
        return new ArrayList<>(tenants.values());
    }
}