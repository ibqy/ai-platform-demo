package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private String buildKey(String query, String modelId, String kbVersion) {
        return query + "|" + modelId + "|" + kbVersion;
    }

    public String get(String query, String modelId, String kbVersion) {
        String key = buildKey(query, modelId, kbVersion);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlSeconds * 1000L) {
            cache.remove(key);
            return null;
        }
        return entry.response;
    }

    public void put(String query, String modelId, String kbVersion, String response) {
        String key = buildKey(query, modelId, kbVersion);
        cache.put(key, new CacheEntry(response, System.currentTimeMillis()));
    }

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