package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EmbeddingCache - 向量嵌入结果的内存缓存
 *
 * 演示 AI 平台中 Embedding 缓存的设计：相同文本的向量化结果可复用，
 * 避免重复调用 Embedding 模型，节省计算资源和响应时间。
 * 使用 ConcurrentHashMap + TTL 过期策略实现线程安全的惰性淘汰。
 *
 * @author ibqy
 */
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

    /**
     * 查询文本的缓存嵌入向量，过期则返回 null
     * @param text 原始文本
     * @return 嵌入向量，未命中或已过期返回 null
     */
    public float[] get(String text) {
        CacheEntry entry = cache.get(text);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.cachedAtEpochMs > ttlSeconds * 1000L) {
            cache.remove(text);
            return null;
        }
        return entry.embedding;
    }

    /**
     * 将文本的嵌入向量写入缓存
     * @param text 原始文本
     * @param embedding 嵌入向量
     */
    public void put(String text, float[] embedding) {
        cache.put(text, new CacheEntry(embedding, System.currentTimeMillis()));
    }

    /** 清空所有嵌入缓存 */
    public void clear() {
        cache.clear();
    }
}