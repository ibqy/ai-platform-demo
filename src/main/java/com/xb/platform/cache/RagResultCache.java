package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RagResultCache {

    @Value("${platform.cache.ttl-seconds:300}")
    private int ttlSeconds;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final List<String> chunkIds;
        final long cachedAtEpochMs;

        CacheEntry(List<String> chunkIds, long cachedAt) {
            this.chunkIds = chunkIds;
            this.cachedAtEpochMs = cachedAt;
        }
    }

    private String buildKey(String query, String kbVersion) {
        return query + "|" + kbVersion;
    }

    public List<String> get(String query, String kbVersion) {
        String key = buildKey(query, kbVersion);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlSeconds * 1000L) {
            cache.remove(key);
            return null;
        }
        return entry.chunkIds;
    }

    public void put(String query, String kbVersion, List<String> chunkIds) {
        String key = buildKey(query, kbVersion);
        cache.put(key, new CacheEntry(chunkIds, System.currentTimeMillis()));
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