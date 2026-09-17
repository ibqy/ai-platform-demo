package com.xb.platform;

import com.xb.platform.tenant.Tenant;
import com.xb.platform.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户 ThreadLocal 隔离测试
 *
 * 心得：多租户系统的关键是请求级别的隔离。
 * TenantContext 用 ThreadLocal 实现，每个请求线程持有自己的 Tenant 对象。
 * 关键陷阱：虚拟线程池复用线程时必须在请求结束时 clear()，
 * 否则下一个请求会读到上一个租户的信息（数据泄漏）。
 */
class TenantIsolationTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGet() {
        Tenant tenant = new Tenant("t1", "Tenant 1", "active", 10000L, Set.of());
        TenantContext.set(tenant);

        Tenant result = TenantContext.get();
        assertNotNull(result);
        assertEquals("t1", result.getTenantId());
    }

    @Test
    void testClear() {
        TenantContext.set(new Tenant("t1", "T1", "active", 1000L, Set.of()));
        assertNotNull(TenantContext.get());

        TenantContext.clear();
        assertNull(TenantContext.get());
    }

    @Test
    void testThreadIsolation() throws Exception {
        Tenant mainTenant = new Tenant("main-tenant", "Main", "active", 1000L, Set.of());
        TenantContext.set(mainTenant);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Tenant> otherThreadResult = new AtomicReference<>();

        Thread other = new Thread(() -> {
            otherThreadResult.set(TenantContext.get());
            latch.countDown();
        });
        other.start();
        latch.await();

        assertNull(otherThreadResult.get(), "其他线程不应读到主线程的 Tenant");
        assertEquals("main-tenant", TenantContext.get().getTenantId());
    }

    @Test
    void testOverwriteTenant() {
        TenantContext.set(new Tenant("t1", "T1", "active", 1000L, Set.of()));
        assertEquals("t1", TenantContext.get().getTenantId());

        TenantContext.set(new Tenant("t2", "T2", "active", 2000L, Set.of()));
        assertEquals("t2", TenantContext.get().getTenantId());
    }
}
