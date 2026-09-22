package com.xb.platform.functioncall;

import java.util.List;
import java.util.Map;

/**
 * 工具定义 —— Function Calling 的核心模型
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p><b>高阶知识点</b>：
 * <ul>
 *     <li>Function Calling（工具调用）是当前 LLM 最热门的能力之一</li>
 *     <li>工具定义遵循 OpenAI function calling 规范：name + description + parameters（JSON Schema）</li>
 *     <li>网关层管理可用工具列表，按租户/模型维度控制工具可见性</li>
 * </ul>
 *
 * <p><b>工作流程</b>：
 * <ol>
 *     <li>客户端请求时携带 tools 列表（或网关自动注入）</li>
 *     <li>LLM 判断需要调用工具时，返回 tool_call（函数名 + 参数 JSON）</li>
 *     <li>网关/客户端执行工具，将结果作为 tool message 回传给 LLM</li>
 *     <li>LLM 基于工具结果生成最终回答</li>
 * </ol>
 *
 * @author ibqy
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {

    /**
     * 快捷构建一个带 JSON Schema 参数的工具
     */
    public static ToolDefinition of(String name, String description,
                                    Map<String, Object> properties,
                                    List<String> required) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", required
        );
        return new ToolDefinition(name, description, schema);
    }

    /**
     * 转为 OpenAI tools 数组中的 function 对象
     */
    public Map<String, Object> toOpenAiFunction() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }
}
