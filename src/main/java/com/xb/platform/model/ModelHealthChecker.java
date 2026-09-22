package com.xb.platform.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * ModelHealthChecker - 模型健康检查器
 *
 * 演示 AI 平台中模型可用性监控的设计：定时探测每个模型的健康状态，
 * 不健康的模型会被路由器自动跳过。教学简化：用随机数模拟探测结果，
 * 生产环境应替换为真实的 HTTP 心跳探测。
 *
 * @author ibqy
 */
@Component
public class ModelHealthChecker {

    @Autowired
    private ModelRegistry registry;

    private final Random random = new Random();

    @Scheduled(fixedRate = 30000)
    /** 定时检查所有模型的健康状态（每 30 秒执行） */
    public void checkHealth() {
        for (ModelMeta meta : registry.listAll()) {
            boolean healthy = simulateHealthCheck(meta);
            meta.setHealthy(healthy);
        }
    }

    boolean simulateHealthCheck(ModelMeta meta) {
        return random.nextInt(100) < 95;
    }
}