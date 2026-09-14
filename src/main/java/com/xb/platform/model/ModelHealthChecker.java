package com.xb.platform.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class ModelHealthChecker {

    @Autowired
    private ModelRegistry registry;

    private final Random random = new Random();

    @Scheduled(fixedRate = 30000)
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