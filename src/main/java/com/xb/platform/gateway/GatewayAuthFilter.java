package com.xb.platform.gateway;

import org.springframework.stereotype.Component;

@Component
public class GatewayAuthFilter {

    public boolean authenticate(String token) {
        return token != null && token.startsWith("sk-");
    }

    public String extractTenantId(String token) {
        if (token == null || token.isEmpty()) return "unknown";
        String prefix = token.substring(0, Math.min(3, token.length()));
        return "tenant-" + prefix;
    }
}