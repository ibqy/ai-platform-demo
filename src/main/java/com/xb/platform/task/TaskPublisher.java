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

    public AsyncTask poll() {
        return queue.poll();
    }

    public int queueSize() {
        return queue.size();
    }
}