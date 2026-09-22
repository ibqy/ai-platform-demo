package com.xb.platform.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * ModelRegistry - 模型注册中心
 *
 * 演示 AI 平台的模型注册与管理：支持动态注册/注销模型，
 * 按类型查询、健康状态标记等操作。使用 ReentrantReadWriteLock
 * 保证读写并发安全——读多写少场景下读写锁比 synchronized 更高效。
 *
 * @author ibqy
 */
public class ModelRegistry {

    private final ConcurrentHashMap<String, ModelMeta> models = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 注册模型到注册中心
     * @param meta 模型元数据
     */
    public void register(ModelMeta meta) {
        lock.writeLock().lock();
        try {
            models.put(meta.getModelId(), meta);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从注册中心移除指定模型
     * @param modelId 模型 ID
     */
    public void unregister(String modelId) {
        lock.writeLock().lock();
        try {
            models.remove(modelId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取指定模型的元数据
     * @param modelId 模型 ID
     * @return 模型元数据，不存在返回 null
     */
    public ModelMeta get(String modelId) {
        lock.readLock().lock();
        try {
            return models.get(modelId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 按类型列出所有已注册模型
     * @param type 模型类型（CHAT/EMBEDDING 等）
     * @return 匹配类型的模型列表
     */
    public List<ModelMeta> listByType(ModelType type) {
        lock.readLock().lock();
        try {
            return models.values().stream()
                    .filter(m -> m.getType() == type)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查指定模型是否健康可用
     * @param modelId 模型 ID
     * @return 模型存在且健康返回 true
     */
    public boolean isHealthy(String modelId) {
        lock.readLock().lock();
        try {
            ModelMeta meta = models.get(modelId);
            return meta != null && meta.isHealthy();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 将指定模型标记为不健康，路由器将跳过此模型
     * @param modelId 模型 ID
     */
    public void markUnhealthy(String modelId) {
        lock.writeLock().lock();
        try {
            ModelMeta meta = models.get(modelId);
            if (meta != null) {
                meta.setHealthy(false);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 列出所有已注册的模型 */
    public List<ModelMeta> listAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(models.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}