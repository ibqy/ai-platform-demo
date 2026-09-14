package com.xb.platform.tenant;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantRegistry {

    private final ConcurrentHashMap<String, Tenant> tenants = new ConcurrentHashMap<>();

    public Tenant get(String tenantId) {
        return tenants.get(tenantId);
    }

    public void register(Tenant tenant) {
        tenants.put(tenant.getTenantId(), tenant);
    }

    public List<Tenant> listAll() {
        return new ArrayList<>(tenants.values());
    }
}