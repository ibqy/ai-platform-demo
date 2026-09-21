package com.xb.platform;

import com.xb.platform.cache.LlmCache;
import com.xb.platform.cache.EmbeddingCache;
import com.xb.platform.cache.RagResultCache;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三级缓存系统测试
 *
 * 心得：缓存设计的核心是 Key 粒度 + TTL + 主动失效。
 * LlmCache 用 tenantId|query|modelId|kbVersion 做 Key，保证租户隔离与同一知识库版本下相同查询命中缓存；
 * 知识库更新时通过 evictByKb 按后缀批量清除，避免脏读。
 */
class CacheSystemTest {

    private void setTtl(Object cache, int seconds) throws Exception {
        Field field = cache.getClass().getDeclaredField("ttlSeconds");
        field.setAccessible(true);
        field.setInt(cache, seconds);
    }

    @Test
    void testLlmCacheHitAndMiss() throws Exception {
        LlmCache cache = new LlmCache();
        setTtl(cache, 300);
        assertNull(cache.get("t1", "什么是RAG", "gpt-4o", "v1"));

        cache.put("t1", "什么是RAG", "gpt-4o", "v1", "RAG是检索增强生成");
        assertEquals("RAG是检索增强生成", cache.get("t1", "什么是RAG", "gpt-4o", "v1"));
    }

    @Test
    void testLlmCacheKeyIsolation() throws Exception {
        LlmCache cache = new LlmCache();
        setTtl(cache, 300);
        cache.put("t1", "你好", "gpt-4o", "v1", "回答A");
        cache.put("t1", "你好", "deepseek", "v1", "回答B");
        cache.put("t1", "你好", "gpt-4o", "v2", "回答C");
        cache.put("t2", "你好", "gpt-4o", "v1", "回答D");

        assertEquals("回答A", cache.get("t1", "你好", "gpt-4o", "v1"));
        assertEquals("回答B", cache.get("t1", "你好", "deepseek", "v1"));
        assertEquals("回答C", cache.get("t1", "你好", "gpt-4o", "v2"));
        assertEquals("回答D", cache.get("t2", "你好", "gpt-4o", "v1"));
    }

    @Test
    void testLlmCacheEvictByKb() throws Exception {
        LlmCache cache = new LlmCache();
        setTtl(cache, 300);
        cache.put("t1", "Q1", "gpt-4o", "kb-v1", "A1");
        cache.put("t1", "Q2", "gpt-4o", "kb-v1", "A2");
        cache.put("t1", "Q3", "gpt-4o", "kb-v2", "A3");

        cache.evictByKb("kb-v1");

        assertNull(cache.get("t1", "Q1", "gpt-4o", "kb-v1"));
        assertNull(cache.get("t1", "Q2", "gpt-4o", "kb-v1"));
        assertEquals("A3", cache.get("t1", "Q3", "gpt-4o", "kb-v2"));
    }

    @Test
    void testLlmCacheTtlExpiry() throws Exception {
        LlmCache cache = new LlmCache();
        setTtl(cache, 0);

        cache.put("t1", "Q", "m", "v", "A");
        Thread.sleep(10);
        assertNull(cache.get("t1", "Q", "m", "v"), "TTL=0 时缓存应立即过期");
    }

    @Test
    void testEmbeddingCachePutAndGet() throws Exception {
        EmbeddingCache cache = new EmbeddingCache();
        Field ttlField = EmbeddingCache.class.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        ttlField.setInt(cache, 600);

        assertNull(cache.get("hello"));

        float[] vec = {0.1f, 0.2f, 0.3f};
        cache.put("hello", vec);
        float[] result = cache.get("hello");

        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0], 0.001);
    }

    @Test
    void testEmbeddingCacheClear() throws Exception {
        EmbeddingCache cache = new EmbeddingCache();
        Field ttlField = EmbeddingCache.class.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        ttlField.setInt(cache, 600);

        cache.put("text1", new float[]{1.0f});
        cache.put("text2", new float[]{2.0f});
        cache.clear();

        assertNull(cache.get("text1"));
        assertNull(cache.get("text2"));
    }

    @Test
    void testRagResultCacheLifecycle() throws Exception {
        RagResultCache cache = new RagResultCache();
        setTtl(cache, 300);
        List<String> chunks = List.of("chunk-1", "chunk-2", "chunk-3");

        assertNull(cache.get("RAG是什么", "kb-v1"));

        cache.put("RAG是什么", "kb-v1", chunks);
        assertEquals(chunks, cache.get("RAG是什么", "kb-v1"));
    }

    @Test
    void testRagCacheEvictByKb() throws Exception {
        RagResultCache cache = new RagResultCache();
        setTtl(cache, 300);
        cache.put("Q1", "kb-alpha", List.of("c1"));
        cache.put("Q2", "kb-alpha", List.of("c2"));
        cache.put("Q3", "kb-beta", List.of("c3"));

        cache.evictByKb("kb-alpha");

        assertNull(cache.get("Q1", "kb-alpha"));
        assertNull(cache.get("Q2", "kb-alpha"));
        assertEquals(List.of("c3"), cache.get("Q3", "kb-beta"));
    }
}
