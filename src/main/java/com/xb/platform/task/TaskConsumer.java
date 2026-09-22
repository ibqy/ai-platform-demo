package com.xb.platform.task;

import com.xb.platform.task.AsyncTask.TaskStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * TaskConsumer - 异步任务消费者
 *
 * 演示生产者-消费者模式中的消费者角色：使用虚拟线程轮询任务队列，
 * 模拟任务处理过程并更新进度。处理失败时根据重试策略决定是否
 * 重新入队（RETRYING）或标记为死信（DEAD），实现优雅的错误恢复。
 *
 * @author ibqy
 */
@Component
public class TaskConsumer {

    @Autowired
    private TaskPublisher publisher;

    @Autowired
    private TaskStore store;

    private final Random random = new Random();

    @PostConstruct
    public void start() {
        Thread.ofVirtual().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    AsyncTask task = publisher.poll();
                    if (task != null) {
                        process(task);
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // 处理单个任务：更新进度 → 模拟执行 → 成功/失败 → 重试或标记死信
    private void process(AsyncTask task) {
        task.setStatus(TaskStatus.PROCESSING);
        task.setProgress(10);
        store.save(task);

        try {
            int sleepMs = 500 + random.nextInt(1500);
            Thread.sleep(sleepMs);
            task.setProgress(70);
            store.save(task);
            Thread.sleep(200);
            task.setProgress(100);

            boolean success = random.nextInt(100) < 90;
            if (success) {
                task.setStatus(TaskStatus.SUCCEEDED);
            } else {
                throw new RuntimeException("Task processing failed");
            }
        } catch (Exception e) {
            task.setRetryCount(task.getRetryCount() + 1);
            task.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

            if (task.getRetryCount() < task.getMaxRetries()) {
                task.setStatus(TaskStatus.RETRYING);
                publisher.requeue(task);
            } else {
                task.setStatus(TaskStatus.DEAD);
            }
        }

        store.save(task);
    }
}