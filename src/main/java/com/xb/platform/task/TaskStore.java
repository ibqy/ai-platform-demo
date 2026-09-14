package com.xb.platform.task;

import com.xb.platform.task.AsyncTask.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TaskStore {

    private final ConcurrentHashMap<String, AsyncTask> store = new ConcurrentHashMap<>();

    public void save(AsyncTask task) {
        task.setUpdatedAtEpochMs(System.currentTimeMillis());
        store.put(task.getTaskId(), task);
    }

    public AsyncTask get(String taskId) {
        return store.get(taskId);
    }

    public List<AsyncTask> listByStatus(TaskStatus status) {
        return store.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<AsyncTask> listByType(String type) {
        return store.values().stream()
                .filter(t -> t.getType().equals(type))
                .collect(Collectors.toList());
    }

    public List<AsyncTask> listAll() {
        return new ArrayList<>(store.values());
    }
}