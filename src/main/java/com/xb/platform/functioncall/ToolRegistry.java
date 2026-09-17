package com.xb.platform.functioncall;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心 —— 管理网关可用的 Function Calling 工具
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p><b>高阶知识点</b>：
 * <ul>
 *     <li>工具注册中心是 AI 网关的核心组件——管理所有可调用的外部工具</li>
 *     <li>支持按类别分组（weather / search / database / calculator 等）</li>
 *     <li>支持按租户/模型维度控制工具可见性（工具白名单）</li>
 *     <li>内置 4 个教学示例工具，展示 JSON Schema 参数定义</li>
 * </ul>
 *
 * <p><b>生产环境扩展</b>：
 * <ul>
 *     <li>工具执行器：根据 tool_call 的 name + arguments 反射调用实际服务</li>
 *     <li>工具沙箱：限制工具执行时间、资源访问范围</li>
 *     <li>工具版本管理：同一工具的多个版本共存，灰度切换</li>
 * </ul>
 */
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();

    public void register(ToolDefinition tool, String category) {
        tools.put(tool.name(), tool);
        categoryIndex.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(tool.name());
    }

    public Optional<ToolDefinition> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<ToolDefinition> listByCategory(String category) {
        Set<String> names = categoryIndex.getOrDefault(category, Set.of());
        return names.stream()
                .map(tools::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ToolDefinition> listAll() {
        return List.copyOf(tools.values());
    }

    public List<Map<String, Object>> toOpenAiTools() {
        return tools.values().stream()
                .map(ToolDefinition::toOpenAiFunction)
                .toList();
    }

    public int size() {
        return tools.size();
    }

    /**
     * 初始化内置教学工具
     */
    public static ToolRegistry createDefault() {
        ToolRegistry registry = new ToolRegistry();

        registry.register(ToolDefinition.of(
                "get_weather",
                "查询指定城市的当前天气信息",
                Map.of(
                        "city", Map.of("type", "string", "description", "城市名称，如：北京、上海"),
                        "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"),
                                "description", "温度单位")
                ),
                List.of("city")
        ), "weather");

        registry.register(ToolDefinition.of(
                "web_search",
                "搜索互联网获取实时信息",
                Map.of(
                        "query", Map.of("type", "string", "description", "搜索关键词"),
                        "max_results", Map.of("type", "integer", "description", "最大返回条数",
                                "default", 5)
                ),
                List.of("query")
        ), "search");

        registry.register(ToolDefinition.of(
                "calculate",
                "执行数学计算表达式",
                Map.of(
                        "expression", Map.of("type", "string",
                                "description", "数学表达式，如：2 + 3 * 4")
                ),
                List.of("expression")
        ), "calculator");

        registry.register(ToolDefinition.of(
                "query_database",
                "查询业务数据库（只读）",
                Map.of(
                        "sql", Map.of("type", "string", "description", "SELECT 查询语句"),
                        "database", Map.of("type", "string", "description", "数据库名称",
                                "default", "main")
                ),
                List.of("sql")
        ), "database");

        return registry;
    }
}
