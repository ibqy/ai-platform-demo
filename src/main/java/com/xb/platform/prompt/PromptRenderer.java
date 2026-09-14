package com.xb.platform.prompt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptRenderer {

    @Autowired
    private PromptTemplateStore store;

    public String render(String templateId, String tenantId, Map<String, String> params) {
        PromptTemplate template = store.selectForTenant(templateId, tenantId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        return template.getRenderedPrompt(params);
    }
}