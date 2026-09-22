package com.xb.platform.prompt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PromptRenderer - Prompt 模板渲染器
 *
 * 演示 Prompt 模板的渲染流程：根据模板 ID 和租户信息选择合适版本的模板，
 * 将参数填充到 {{placeholder}} 占位符中生成最终 Prompt。
 * 支持多租户场景下不同租户使用不同版本的 Prompt（灰度发布）。
 *
 * @author ibqy
 */
@Component
public class PromptRenderer {

    @Autowired
    private PromptTemplateStore store;

    /**
     * 渲染 Prompt 模板：选择模板版本并填充参数
     * @param templateId 模板 ID
     * @param tenantId 租户 ID（用于灰度选择版本）
     * @param params 模板参数（Key 对应 {{placeholder}}）
     * @return 渲染后的完整 Prompt
     * @throws IllegalArgumentException 模板不存在或缺少必需参数
     */
    public String render(String templateId, String tenantId, Map<String, String> params) {
        PromptTemplate template = store.selectForTenant(templateId, tenantId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        return template.getRenderedPrompt(params);
    }
}