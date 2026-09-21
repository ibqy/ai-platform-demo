package com.xb.platform.task;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class TaskPublisher {

    private final ConcurrentLinkedQueue<AsyncTask> queue = new ConcurrentLinkedQueue<>();

    public String publish(String type, String payload) {
        return publish(type, payload, 3);
    }

    public String publish(String type, String payload, int maxRetries) {
        AsyncTask task = new AsyncTask(type, payload, maxRetries);
        queue.add(task);
        return task.getTaskId();
    }

    /**
     * 将已存在的任务重新放回队列（保留原 UUID 与 retryCount）。
     * 用于失败重试场景，避免 publish 创建新任务导致原任务永远 RETRYING。
     */
    public void requeue(AsyncTask task) {
        queue.add(task);
    }

    public AsyncTask poll() {
        return queue.poll();
    }

    public int queueSize() {
        return queue.size();
    }
}