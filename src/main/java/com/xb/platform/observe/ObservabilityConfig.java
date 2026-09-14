package com.xb.platform.observe;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        System.out.println("Observability initialized: SimpleMeterRegistry + scheduled alerts");
    }
}