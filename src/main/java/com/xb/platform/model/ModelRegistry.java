package com.xb.platform.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ModelRegistry {

    private final ConcurrentHashMap<String, ModelMeta> models = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void register(ModelMeta meta) {
        lock.writeLock().lock();
        try {
            models.put(meta.getModelId(), meta);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregister(String modelId) {
        lock.writeLock().lock();
        try {
            models.remove(modelId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ModelMeta get(String modelId) {
        lock.readLock().lock();
        try {
            return models.get(modelId);
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public boolean isHealthy(String modelId) {
        lock.readLock().lock();
        try {
            ModelMeta meta = models.get(modelId);
            return meta != null && meta.isHealthy();
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public List<ModelMeta> listAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(models.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}