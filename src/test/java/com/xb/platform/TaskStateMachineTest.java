package com.xb.platform;

import com.xb.platform.task.AsyncTask;
import com.xb.platform.task.AsyncTask.TaskStatus;
import com.xb.platform.task.TaskStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步任务状态机测试
 *
 * 心得：任务状态机是异步平台的核心。
 * QUEUED → PROCESSING → SUCCEEDED / FAILED → RETRYING → DEAD
 * 关键设计：maxRetries 控制重试上限，超过后进入 DEAD 状态，
 * 避免无限重试消耗资源。TaskStore 用 ConcurrentHashMap 做内存存储，
 * 生产环境应替换为 Redis 或数据库。
 */
class TaskStateMachineTest {

    private TaskStore store;

    @BeforeEach
    void setUp() {
        store = new TaskStore();
    }

    @Test
    void testNewTaskInitialState() {
        AsyncTask task = new AsyncTask("doc-parse", "{\"file\":\"test.pdf\"}", 3);

        assertNotNull(task.getTaskId());
        assertEquals(TaskStatus.QUEUED, task.getStatus());
        assertEquals("doc-parse", task.getType());
        assertEquals(0, task.getRetryCount());
        assertEquals(0, task.getProgress());
        assertEquals(3, task.getMaxRetries());
    }

    @Test
    void testStatusTransition() {
        AsyncTask task = new AsyncTask("vectorize", "payload", 3);

        task.setStatus(TaskStatus.PROCESSING);
        assertEquals(TaskStatus.PROCESSING, task.getStatus());

        task.setProgress(50);
        assertEquals(50, task.getProgress());

        task.setStatus(TaskStatus.SUCCEEDED);
        task.setProgress(100);
        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
        assertEquals(100, task.getProgress());
    }

    @Test
    void testRetryUntilDead() {
        AsyncTask task = new AsyncTask("parse", "data", 2);

        task.setStatus(TaskStatus.PROCESSING);
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage("timeout");
        task.setRetryCount(1);

        task.setStatus(TaskStatus.RETRYING);
        assertEquals(1, task.getRetryCount());

        task.setStatus(TaskStatus.PROCESSING);
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage("timeout again");
        task.setRetryCount(2);

        assertTrue(task.getRetryCount() >= task.getMaxRetries());
        task.setStatus(TaskStatus.DEAD);
        assertEquals(TaskStatus.DEAD, task.getStatus());
    }

    @Test
    void testTaskStoreSaveAndGet() {
        AsyncTask task = new AsyncTask("embed", "text-data", 3);
        store.save(task);

        AsyncTask retrieved = store.get(task.getTaskId());
        assertNotNull(retrieved);
        assertEquals(task.getTaskId(), retrieved.getTaskId());
        assertEquals("embed", retrieved.getType());
    }

    @Test
    void testTaskStoreListByStatus() {
        AsyncTask t1 = new AsyncTask("parse", "a", 3);
        AsyncTask t2 = new AsyncTask("parse", "b", 3);
        AsyncTask t3 = new AsyncTask("vectorize", "c", 3);

        store.save(t1);
        store.save(t2);
        store.save(t3);

        t2.setStatus(TaskStatus.PROCESSING);
        store.save(t2);

        assertEquals(2, store.listByStatus(TaskStatus.QUEUED).size());
        assertEquals(1, store.listByStatus(TaskStatus.PROCESSING).size());
        assertEquals(0, store.listByStatus(TaskStatus.DEAD).size());
    }

    @Test
    void testTaskStoreListByType() {
        store.save(new AsyncTask("parse", "a", 3));
        store.save(new AsyncTask("parse", "b", 3));
        store.save(new AsyncTask("vectorize", "c", 3));

        assertEquals(2, store.listByType("parse").size());
        assertEquals(1, store.listByType("vectorize").size());
    }

    @Test
    void testTaskCancellation() {
        AsyncTask task = new AsyncTask("parse", "data", 3);
        store.save(task);

        task.setStatus(TaskStatus.CANCELLED);
        store.save(task);

        assertEquals(TaskStatus.CANCELLED, store.get(task.getTaskId()).getStatus());
    }
}
