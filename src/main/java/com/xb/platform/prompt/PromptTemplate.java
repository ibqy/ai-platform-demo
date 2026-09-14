package com.xb.platform.prompt;

import java.util.List;
import java.util.Map;

public class PromptTemplate {

    private String templateId;
    private String version;
    private String systemPrompt;
    private List<String> requiredParams;
    private long createdAtEpochMs;
    private String createdBy;

    public PromptTemplate() {}

    public PromptTemplate(String templateId, String version, String systemPrompt,
                          List<String> requiredParams, long createdAtEpochMs, String createdBy) {
        this.templateId = templateId;
        this.version = version;
        this.systemPrompt = systemPrompt;
        this.requiredParams = requiredParams;
        this.createdAtEpochMs = createdAtEpochMs;
        this.createdBy = createdBy;
    }

    public String getRenderedPrompt(Map<String, String> params) {
        if (requiredParams != null) {
            for (String param : requiredParams) {
                if (!params.containsKey(param) || params.get(param) == null || params.get(param).isEmpty()) {
                    throw new IllegalArgumentException("Missing required param: " + param);
                }
            }
        }
        String result = systemPrompt;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public List<String> getRequiredParams() { return requiredParams; }
    public void setRequiredParams(List<String> requiredParams) { this.requiredParams = requiredParams; }
    public long getCreatedAtEpochMs() { return createdAtEpochMs; }
    public void setCreatedAtEpochMs(long createdAtEpochMs) { this.createdAtEpochMs = createdAtEpochMs; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}