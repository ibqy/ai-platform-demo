package com.xb.platform.prompt;

import java.util.List;
import java.util.Map;

/**
 * PromptTemplate - Prompt 模板实体
 *
 * 封装一个 Prompt 模板的完整信息：模板 ID、版本号、系统提示词、
 * 必需参数列表。支持 {{placeholder}} 占位符替换渲染，
 * 并在渲染前校验必需参数是否齐全，避免运行时出现未替换的占位符。
 *
 * @author ibqy
 */
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

    /**
     * 将参数填充到模板占位符 {{key}} 中，生成最终 Prompt
     * @param params 参数 Map
     * @return 渲染后的 Prompt 文本
     * @throws IllegalArgumentException 缺少必需参数时抛出
     */
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