package com.xb.platform;

import com.xb.platform.gateway.AiGatewayService;
import com.xb.platform.model.ModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用上下文加载测试 —— 真正的"冒烟测试"
 *
 * <p><b>为什么重写这个测试</b>：原版是 {@code assertNotNull(new PlatformApplication())}——
 * 直接 new 启动类<b>根本不会加载 Spring 上下文</>，装配错误（缺 Bean、循环依赖、条件注解失效）
 * 一个都测不出来。2026-10-16 就因此漏掉了一个真实缺陷：{@code ModelRegistry} 漏加
 * {@code @Component}，64 个单测全绿，应用一启动就 FAILED TO START。</p>
 *
 * <p>教训：<b>"冒烟测试"必须真的点火</b>。对 Spring 应用来说，点火就是
 * {@code @SpringBootTest} 加载完整上下文；new 一个启动类只是自欺欺人。</p>
 *
 * @author ibqy
 */
@SpringBootTest
class PlatformApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        // 上下文能加载完成 = 所有 Bean 装配成功（这一行就是测试的主体）
        assertThat(context).isNotNull();
    }

    @Test
    void coreBeansAreWired() {
        // 点名关键 Bean：曾经的漏网之鱼（ModelRegistry）与网关核心（AiGatewayService）
        assertThat(context.getBean(ModelRegistry.class)).isNotNull();
        assertThat(context.getBean(AiGatewayService.class)).isNotNull();
    }
}
