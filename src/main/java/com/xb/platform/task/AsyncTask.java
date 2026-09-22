package com.xb.platform.task;

import java.util.UUID;

/**
 * AsyncTask - 异步任务实体
 *
 * 演示 AI 平台异步任务的状态机模型：QUEUED → PROCESSING → SUCCEEDED/FAILED，
 * 失败后可进入 RETRYING 状态重新排队，超过最大重试次数则进入 DEAD 状态。
 * 携带进度百分比和错误信息，便于前端轮询展示任务进展。
 *
 * @author ibqy
 */
public class AsyncTask {

    public enum TaskStatus {
        QUEUED, PROCESSING, SUCCEEDED, FAILED, RETRYING, DEAD, CANCELLED
    }

    private String taskId;
    private String type;
    private String payload;
    private TaskStatus status;
    private int retryCount;
    private int maxRetries = 3;
    private String errorMessage;
    private int progress;
    private long createdAtEpochMs;
    private long updatedAtEpochMs;

    public AsyncTask() {}

    public AsyncTask(String type, String payload, int maxRetries) {
        this.taskId = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.maxRetries = maxRetries;
        this.status = TaskStatus.QUEUED;
        this.progress = 0;
        long now = System.currentTimeMillis();
        this.createdAtEpochMs = now;
        this.updatedAtEpochMs = now;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public long getCreatedAtEpochMs() { return createdAtEpochMs; }
    public void setCreatedAtEpochMs(long createdAtEpochMs) { this.createdAtEpochMs = createdAtEpochMs; }
    public long getUpdatedAtEpochMs() { return updatedAtEpochMs; }
    public void setUpdatedAtEpochMs(long updatedAtEpochMs) { this.updatedAtEpochMs = updatedAtEpochMs; }
}