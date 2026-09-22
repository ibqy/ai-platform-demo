package com.xb.platform.task;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TaskPublisher - 异步任务发布者
 *
 * 演示生产者-消费者模式中的生产者角色：将新任务包装为 AsyncTask
 * 并放入并发队列。支持 requeue 方法用于失败重试场景，
 * 保留原任务 UUID 和重试计数，避免创建新任务导致状态混乱。
 *
 * @author ibqy
 */
@Component
public class TaskPublisher {

    private final ConcurrentLinkedQueue<AsyncTask> queue = new ConcurrentLinkedQueue<>();

    /**
     * 发布任务（默认最多重试 3 次）
     * @param type 任务类型
     * @param payload 任务载荷
     * @return 新创建的任务 ID
     */
    public String publish(String type, String payload) {
        return publish(type, payload, 3);
    }

    /**
     * 发布任务并指定最大重试次数
     * @param type 任务类型
     * @param payload 任务载荷
     * @param maxRetries 最大重试次数
     * @return 新创建的任务 ID
     */
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

    /**
     * 从队列中取出一个待处理任务
     * @return 队首任务，队列为空返回 null
     */
    public AsyncTask poll() {
        return queue.poll();
    }

    public int queueSize() {
        return queue.size();
    }
}