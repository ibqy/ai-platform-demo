package com.xb.platform.task;

import com.xb.platform.task.AsyncTask.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskPublisher publisher;

    @Autowired
    private TaskStore store;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submit(@RequestParam String type, @RequestParam String payload) {
        String taskId = publisher.publish(type, payload);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

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