package com.xb.platform;

import com.xb.platform.model.ModelMeta;
import com.xb.platform.model.ModelRegistry;
import com.xb.platform.model.ModelType;
import com.xb.platform.observe.TokenMeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token 计量与成本核算测试
 *
 * 心得：TokenMeter 是 AI 平台成本控制的核心。
 * 每次 LLM 调用都要记账：inputTokens + outputTokens → 按模型单价算费用。
 * 用 AtomicLong 保证并发安全，ConcurrentHashMap 做租户×模型二维表。
 * 生产环境这些数据会导出到 Prometheus + Grafana 做可视化。
 */
class TokenMeterTest {

    private TokenMeter meter;
    private ModelRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        meter = new TokenMeter();
        registry = new ModelRegistry();

        registry.register(new ModelMeta("gpt-4o", ModelType.CHAT, "OpenAI", "v4",
                "", "", 128000, 0.5, 100));
        registry.register(new ModelMeta("qwen-turbo", ModelType.CHAT, "Alibaba", "v2",
                "", "", 8000, 0.01, 200));

        java.lang.reflect.Field registryField = TokenMeter.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        registryField.set(meter, registry);
    }

    @Test
    void testRecordAndCost() {
        meter.record("tenant-a", "gpt-4o", 1000, 500);

        double cost = meter.getCost("tenant-a", "gpt-4o");
        assertTrue(cost > 0);
    }

    @Test
    void testCostProportionalToPrice() {
        meter.record("tenant-a", "gpt-4o", 10000, 5000);
        meter.record("tenant-a", "qwen-turbo", 10000, 5000);

        double gptCost = meter.getCost("tenant-a", "gpt-4o");
        double qwenCost = meter.getCost("tenant-a", "qwen-turbo");

        assertTrue(gptCost > qwenCost, "gpt-4o 单价 0.5 应比 qwen-turbo 0.01 贵");
    }

    @Test
    void testMultiTenantIsolation() {
        meter.record("tenant-a", "gpt-4o", 1000, 500);
        meter.record("tenant-b", "gpt-4o", 2000, 1000);

        assertEquals(1000, meter.getTotalInputTokens("tenant-a"));
        assertEquals(2000, meter.getTotalInputTokens("tenant-b"));
    }

    @Test
    void testTotalTokens() {
        meter.record("tenant-a", "gpt-4o", 100, 50);
        meter.record("tenant-a", "qwen-turbo", 200, 100);

        assertEquals(300, meter.getTotalInputTokens("tenant-a"));
        assertEquals(150, meter.getTotalOutputTokens("tenant-a"));
    }

    @Test
    void testUnknownTenantReturnsZero() {
        assertEquals(0, meter.getTotalInputTokens("nonexistent"));
        assertEquals(0.0, meter.getCost("nonexistent", "gpt-4o"));
    }

    @Test
    void testTenantReport() {
        meter.record("tenant-a", "gpt-4o", 1000, 500);
        meter.record("tenant-a", "qwen-turbo", 2000, 1000);

        Map<String, Object> report = meter.getTenantReport("tenant-a");

        assertFalse(report.isEmpty());
        assertEquals(3000L, report.get("_totalInputTokens"));
        assertEquals(1500L, report.get("_totalOutputTokens"));
        assertTrue((double) report.get("_totalCost") > 0);

        assertTrue(report.containsKey("gpt-4o"));
        assertTrue(report.containsKey("qwen-turbo"));
    }

    @Test
    void testEmptyTenantReport() {
        Map<String, Object> report = meter.getTenantReport("nobody");
        assertTrue(report.isEmpty());
    }

    @Test
    void testAccumulateMultipleRecords() {
        meter.record("tenant-a", "gpt-4o", 100, 50);
        meter.record("tenant-a", "gpt-4o", 200, 100);

        assertEquals(300, meter.getTotalInputTokens("tenant-a"));
        assertEquals(150, meter.getTotalOutputTokens("tenant-a"));
    }
}
