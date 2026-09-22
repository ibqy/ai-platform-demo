package com.xb.platform.gateway;

import org.springframework.stereotype.Component;

/**
 * GatewayAuthFilter - AI 网关认证过滤器
 *
 * 演示 AI 网关的简化鉴权机制：通过 token 前缀 "sk-" 验证合法性，
 * 并从 token 中提取租户标识。生产环境应替换为 JWT/OAuth2 等标准方案。
 *
 * @author ibqy
 */
@Component
public class GatewayAuthFilter {

    /**
     * 验证 token 是否合法（简化：检查 sk- 前缀）
     * @param token 认证令牌
     * @return token 合法返回 true
     */
    public boolean authenticate(String token) {
        return token != null && token.startsWith("sk-");
    }

    /**
     * 从 token 中提取租户标识（教学简化：取前3字符作后缀）
     * @param token 认证令牌
     * @return 租户 ID 字符串
     */
    public String extractTenantId(String token) {
        if (token == null || token.isEmpty()) return "unknown";
        String prefix = token.substring(0, Math.min(3, token.length()));
        return "tenant-" + prefix;
    }
}