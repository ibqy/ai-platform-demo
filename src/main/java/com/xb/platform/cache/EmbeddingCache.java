package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmbeddingCache {

    @Value("${platform.cache.embedding-ttl:600}")
    private int ttlSeconds;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final float[] embedding;
        final long cachedAtEpochMs;

        CacheEntry(float[] embedding, long cachedAt) {
            this.embedding = embedding;
            this.cachedAtEpochMs = cachedAt;
        }
    }

    public float[] get(String text) {
        CacheEntry entry = cache.get(text);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlSeconds * 1000L) {
            cache.remove(text);
            return null;
        }
        return entry.embedding;
    }

    public void put(String text, float[] embedding) {
        cache.put(text, new CacheEntry(embedding, System.currentTimeMillis()));
    }

    public void clear() {
        cache.clear();
    }
}