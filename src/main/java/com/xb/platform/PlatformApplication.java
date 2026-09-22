package com.xb.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PlatformApplication - AI 平台教学演示启动入口
 *
 * 本项目演示了一个完整的 AI 网关平台架构，涵盖模型路由、多租户隔离、
 * 缓存策略、Prompt 管理、异步任务、可观测性等核心模块。
 * 通过 @EnableScheduling 开启定时任务，驱动健康检查与告警评估。
 *
 * @author ibqy
 */
@SpringBootApplication(scanBasePackages = "com.xb.platform")
@EnableScheduling
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}