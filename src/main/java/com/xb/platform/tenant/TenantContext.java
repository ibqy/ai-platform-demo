package com.xb.platform.tenant;

public class TenantContext {

    private static final ThreadLocal<Tenant> CONTEXT = new ThreadLocal<>();

    public static Tenant get() {
        return CONTEXT.get();
    }

    public static void set(Tenant tenant) {
        CONTEXT.set(tenant);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}