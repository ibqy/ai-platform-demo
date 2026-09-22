package com.xb.platform.task;

import com.xb.platform.task.AsyncTask.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TaskController - 异步任务 REST 控制器
 *
 * 演示异步任务的 HTTP 接口设计：提交任务、查询任务状态、按条件列表查询。
 * 支持按状态和类型筛选，将内部实体转换为 Map 返回给前端。
 * 这是 AI 平台中批量文档处理、向量化等耗时任务的管理入口。
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskPublisher publisher;

    @Autowired
    private TaskStore store;

    /**
     * 提交异步任务
     * @param type 任务类型
     * @param payload 任务载荷（JSON 字符串）
     * @return 包含 taskId 的响应
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submit(@RequestParam String type, @RequestParam String payload) {
        String taskId = publisher.publish(type, payload);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

    /**
     * 根据 ID 查询任务详情
     * @param taskId 任务 ID
     * @return 任务详情，不存在返回 404
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        AsyncTask task = store.get(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toMap(task));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        List<AsyncTask> tasks = store.listAll();
        if (status != null) {
            TaskStatus st = TaskStatus.valueOf(status.toUpperCase());
            tasks = tasks.stream().filter(t -> t.getStatus() == st).collect(Collectors.toList());
        }
        if (type != null) {
            tasks = tasks.stream().filter(t -> t.getType().equals(type)).collect(Collectors.toList());
        }
        List<Map<String, Object>> result = tasks.stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> toMap(AsyncTask task) {
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", task.getTaskId());
        m.put("type", task.getType());
        m.put("status", task.getStatus().name());
        m.put("progress", task.getProgress());
        m.put("retryCount", task.getRetryCount());
        m.put("maxRetries", task.getMaxRetries());
        m.put("errorMessage", task.getErrorMessage());
        m.put("createdAtEpochMs", task.getCreatedAtEpochMs());
        m.put("updatedAtEpochMs", task.getUpdatedAtEpochMs());
        return m;
    }
}