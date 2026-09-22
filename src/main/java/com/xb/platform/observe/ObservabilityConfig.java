package com.xb.platform.observe;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * ObservabilityConfig - 可观测性基础设施配置
 *
 * 演示 Micrometer 指标采集的 Spring 配置：注册 MeterRegistry Bean
 * 作为指标收集的入口。教学使用 SimpleMeterRegistry（内存存储），
 * 生产环境替换为 Prometheus MeterRegistry 对接 Grafana 可视化。
 *
 * @author ibqy
 */
@Configuration
public class ObservabilityConfig {

    /**
     * 注册 Micrometer 指标采集器 Bean。
     *
     * <p>教学使用 SimpleMeterRegistry（内存存储），
     * 生产环境可替换为 PrometheusMeterRegistry 对接 Grafana。</p>
     *
     * @return MeterRegistry 实例
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * 应用就绪后的回调，打印可观测性初始化完成信息。
     *
     * <p>用于确认指标采集和告警调度已就绪，
     * 便于教学演示时验证基础设施启动状态。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        System.out.println("Observability initialized: SimpleMeterRegistry + scheduled alerts");
    }
}