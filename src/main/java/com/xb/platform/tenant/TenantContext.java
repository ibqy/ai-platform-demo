package com.xb.platform.tenant;

/**
 * TenantContext - 租户上下文（ThreadLocal 实现）
 *
 * 演示多租户隔离的经典方案：使用 ThreadLocal 在每个请求线程中
 * 持有独立的 Tenant 对象，实现请求级别的租户隔离。
 * 关键：请求结束时必须 clear()，防止线程池复用导致数据泄漏。
 *
 * @author ibqy
 */
public class TenantContext {

    private static final ThreadLocal<Tenant> CONTEXT = new ThreadLocal<>();

    /** 获取当前线程绑定的租户信息 */
    public static Tenant get() {
        return CONTEXT.get();
    }

    /** 将租户信息绑定到当前线程 */
    public static void set(Tenant tenant) {
        CONTEXT.set(tenant);
    }

    /** 清除当前线程的租户绑定（防止线程复用时数据泄漏） */
    public static void clear() {
        CONTEXT.remove();
    }
}