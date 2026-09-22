package com.xb.platform.task;

import com.xb.platform.task.AsyncTask.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TaskStore - 异步任务存储
 *
 * 演示异步任务的内存存储层：使用 ConcurrentHashMap 保证并发安全，
 * 支持按状态、类型等维度过滤查询。教学使用内存存储，
 * 生产环境应替换为 Redis 或数据库，并支持持久化与分布式协调。
 *
 * @author ibqy
 */
@Component
public class TaskStore {

    private final ConcurrentHashMap<String, AsyncTask> store = new ConcurrentHashMap<>();

    /** 保存或更新任务（自动更新 updatedAt 时间戳） */
    public void save(AsyncTask task) {
        task.setUpdatedAtEpochMs(System.currentTimeMillis());
        store.put(task.getTaskId(), task);
    }

    /**
     * 根据 ID 获取任务
     * @param taskId 任务 ID
     * @return 任务实体，不存在返回 null
     */
    public AsyncTask get(String taskId) {
        return store.get(taskId);
    }

    /**
     * 按状态过滤任务列表
     * @param status 目标状态
     * @return 匹配状态的任务列表
     */
    public List<AsyncTask> listByStatus(TaskStatus status) {
        return store.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 按类型过滤任务列表
     * @param type 任务类型
     * @return 匹配类型的任务列表
     */
    public List<AsyncTask> listByType(String type) {
        return store.values().stream()
                .filter(t -> t.getType().equals(type))
                .collect(Collectors.toList());
    }

    /** 列出所有任务 */
    public List<AsyncTask> listAll() {
        return new ArrayList<>(store.values());
    }
}