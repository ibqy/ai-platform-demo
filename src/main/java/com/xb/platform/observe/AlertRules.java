package com.xb.platform.observe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertRules {

    @Autowired
    private PlatformMetrics metrics;

    @Autowired
    private TokenMeter tokenMeter;

    private final ConcurrentHashMap<String, Long> prevTokenCount = new ConcurrentHashMap<>();

    @FunctionalInterface
    interface AlertRule {
        String check();
    }

    private final List<AlertRule> rules = new ArrayList<>();

    public AlertRules() {
        rules.add(() -> checkHighModelErrorRate());
        rules.add(() -> checkTokenSpike());
        rules.add(() -> checkHighLatency());
    }

    @Scheduled(fixedRate = 30000)
    public void evaluate() {
        List<String> alerts = evaluateAll();
        for (String alert : alerts) {
            System.out.println("[ALERT] " + alert);
        }
    }

    public List<String> evaluateAll() {
        List<String> result = new ArrayList<>();
        for (AlertRule rule : rules) {
            String msg = rule.check();
            if (msg != null) result.add(msg);
        }
        return result;
    }

    private String checkHighModelErrorRate() {
        return checkModelErrorRate("deepseek-chat");
    }

    public String checkModelErrorRate(String modelId) {
        Map<String, Object> m = metrics.getModelMetrics(modelId);
        double rate = (Double) m.getOrDefault("successRate", 100.0);
        if (rate < 80.0) {
            return String.format("High error rate on model %s: success rate %.1f%%", modelId, rate);
        }
        return null;
    }

    private String checkTokenSpike() {
        return null;
    }

    private String checkHighLatency() {
        return null;
    }
}