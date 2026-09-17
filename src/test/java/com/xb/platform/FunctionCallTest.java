package com.xb.platform;

import com.xb.platform.functioncall.ToolDefinition;
import com.xb.platform.functioncall.ToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Function Calling 工具注册测试")
class FunctionCallTest {

    @Nested
    @DisplayName("ToolDefinition")
    class DefinitionTests {

        @Test
        @DisplayName("of() 创建带 JSON Schema 的工具定义")
        void createWithSchema() {
            ToolDefinition tool = ToolDefinition.of(
                    "test_tool", "测试工具",
                    Map.of("query", Map.of("type", "string", "description", "查询内容")),
                    List.of("query")
            );

            assertEquals("test_tool", tool.name());
            assertEquals("测试工具", tool.description());
            assertNotNull(tool.parameters());
        }

        @Test
        @DisplayName("toOpenAiFunction() 生成 OpenAI 格式")
        void toOpenAiFormat() {
            ToolDefinition tool = ToolDefinition.of(
                    "get_weather", "查天气",
                    Map.of("city", Map.of("type", "string")),
                    List.of("city")
            );

            Map<String, Object> result = tool.toOpenAiFunction();
            assertEquals("function", result.get("type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) result.get("function");
            assertEquals("get_weather", function.get("name"));
            assertEquals("查天气", function.get("description"));
            assertNotNull(function.get("parameters"));
        }
    }

    @Nested
    @DisplayName("ToolRegistry")
    class RegistryTests {

        @Test
        @DisplayName("注册和查询工具")
        void registerAndGet() {
            ToolRegistry registry = new ToolRegistry();
            ToolDefinition tool = ToolDefinition.of("my_tool", "我的工具",
                    Map.of(), List.of());

            registry.register(tool, "custom");

            assertTrue(registry.get("my_tool").isPresent());
            assertEquals("my_tool", registry.get("my_tool").get().name());
            assertFalse(registry.get("nonexistent").isPresent());
        }

        @Test
        @DisplayName("按类别查询工具")
        void listByCategory() {
            ToolRegistry registry = new ToolRegistry();
            registry.register(ToolDefinition.of("w1", "天气1", Map.of(), List.of()), "weather");
            registry.register(ToolDefinition.of("w2", "天气2", Map.of(), List.of()), "weather");
            registry.register(ToolDefinition.of("s1", "搜索1", Map.of(), List.of()), "search");

            assertEquals(2, registry.listByCategory("weather").size());
            assertEquals(1, registry.listByCategory("search").size());
            assertEquals(0, registry.listByCategory("nonexistent").size());
        }

        @Test
        @DisplayName("createDefault() 包含 4 个内置工具")
        void defaultRegistryHasFourTools() {
            ToolRegistry registry = ToolRegistry.createDefault();

            assertEquals(4, registry.size());
            assertTrue(registry.get("get_weather").isPresent());
            assertTrue(registry.get("web_search").isPresent());
            assertTrue(registry.get("calculate").isPresent());
            assertTrue(registry.get("query_database").isPresent());
        }

        @Test
        @DisplayName("toOpenAiTools() 生成 OpenAI tools 数组格式")
        void toOpenAiTools() {
            ToolRegistry registry = ToolRegistry.createDefault();
            List<Map<String, Object>> tools = registry.toOpenAiTools();

            assertEquals(4, tools.size());
            for (Map<String, Object> tool : tools) {
                assertEquals("function", tool.get("type"));
                assertNotNull(tool.get("function"));
            }
        }

        @Test
        @DisplayName("listAll() 返回不可变副本")
        void listAllIsImmutable() {
            ToolRegistry registry = ToolRegistry.createDefault();
            List<ToolDefinition> all = registry.listAll();

            assertThrows(UnsupportedOperationException.class,
                    () -> all.add(ToolDefinition.of("hack", "", Map.of(), List.of())));
        }
    }
}
