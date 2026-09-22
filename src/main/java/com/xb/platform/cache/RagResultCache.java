package com.xb.platform.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RagResultCache - RAG 检索结果的缓存
 *
 * 演示 RAG 管道中检索阶段的缓存设计：相同查询 + 相同知识库版本
 * 可以直接返回已检索的文档片段 ID 列表，避免重复向量检索。
 * 支持按知识库版本批量失效，确保知识库更新后不会读到脏数据。
 *
 * @author ibqy
 */
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

    /**
     * 查询 RAG 检索结果缓存
     * @param query 用户查询文本
     * @param kbVersion 知识库版本号
     * @return 文档片段 ID 列表，未命中或已过期返回 null
     */
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

    /**
     * 将 RAG 检索结果写入缓存
     * @param query 用户查询文本
     * @param kbVersion 知识库版本号
     * @param chunkIds 命中的文档片段 ID 列表
     */
    public void put(String query, String kbVersion, List<String> chunkIds) {
        String key = buildKey(query, kbVersion);
        cache.put(key, new CacheEntry(chunkIds, System.currentTimeMillis()));
    }

    /**
     * 按知识库版本批量失效缓存条目
     * @param kbVersion 需失效的知识库版本
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