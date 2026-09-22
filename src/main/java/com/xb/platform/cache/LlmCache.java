package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LlmCache - LLM 对话结果的缓存
 *
 * 演示 AI 网关中响应缓存的设计：以 tenantId|query|modelId|kbVersion
 * 作为复合 Key，在保证租户隔离的前提下最大化缓存命中率。
 * 缓存位于鉴权与白名单检查之后，防止绕过权限控制。
 *
 * @author ibqy
 */
@Component
public class LlmCache {

    @Value("${platform.cache.ttl-seconds:300}")
    private int ttlSeconds;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String response;
        final long cachedAtEpochMs;

        CacheEntry(String response, long cachedAt) {
            this.response = response;
            this.cachedAtEpochMs = cachedAt;
        }
    }

    private String buildKey(String tenantId, String query, String modelId, String kbVersion) {
        return tenantId + "|" + query + "|" + modelId + "|" + kbVersion;
    }

    /**
     * 查询 LLM 响应缓存，按租户+查询+模型+知识库版本隔离
     * @param tenantId 租户 ID
     * @param query 用户查询
     * @param modelId 模型 ID
     * @param kbVersion 知识库版本
     * @return 缓存的 LLM 响应，未命中或已过期返回 null
     */
    public String get(String tenantId, String query, String modelId, String kbVersion) {
        String key = buildKey(tenantId, query, modelId, kbVersion);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlSeconds * 1000L) {
            cache.remove(key);
            return null;
        }
        return entry.response;
    }

    /**
     * 将 LLM 响应写入缓存
     * @param tenantId 租户 ID
     * @param query 用户查询
     * @param modelId 模型 ID
     * @param kbVersion 知识库版本
     * @param response LLM 响应内容
     */
    public void put(String tenantId, String query, String modelId, String kbVersion, String response) {
        String key = buildKey(tenantId, query, modelId, kbVersion);
        cache.put(key, new CacheEntry(response, System.currentTimeMillis()));
    }

    /**
     * 按知识库版本批量失效缓存，避免脏读
     * @param kbVersion 需失效的知识库版本后缀
     */
    public void evictByKb(String kbVersion) {
        String suffix = "|" + kbVersion;
        Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CacheEntry> entry = it.next();
            if (entry.getKey().endsWith(suffix)) {
                it.remove();
            }
        }
    }
}