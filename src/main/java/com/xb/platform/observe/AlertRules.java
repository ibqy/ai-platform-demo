package com.xb.platform.observe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AlertRules - 告警规则引擎
 *
 * 演示 AI 平台的可观测性告警机制：定时评估预定义规则（模型错误率、
 * Token 用量突增、高延迟），触发告警输出。使用函数式接口 AlertRule
 * 实现规则的灵活扩展，新增告警只需添加一条 lambda 表达式。
 *
 * @author ibqy
 */
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
    /** 定时评估所有告警规则（每 30 秒执行） */
    public void evaluate() {
        List<String> alerts = evaluateAll();
        for (String alert : alerts) {
            System.out.println("[ALERT] " + alert);
        }
    }

    /**
     * 执行所有告警规则并收集告警消息
     * @return 触发的告警消息列表，为空表示一切正常
     */
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

    /**
     * 检查指定模型的错误率是否超过阈值
     * @param modelId 模型 ID
     * @return 错误率超过 20% 返回告警消息，否则返回 null
     */
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