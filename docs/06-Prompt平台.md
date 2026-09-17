# Prompt 模板平台 — 学习笔记

## 为什么需要管理 Prompt

之前做 RAG 和 Agent 项目时，Prompt 都是硬编码在代码里的——一个 `String.format` 拼完就发给模型了。改一个措辞要编译重启，版本对比更是无从谈起。到了中台场景，问题被放大：

- 不同租户可能需要不同的 Prompt 话术（合规要求不同）
- 同一个 Prompt 需要灰度发布新版，验证效果
- Prompt 缺参会拼出残缺请求——应该直接报错让调用方补传

## 模板数据模型

`PromptTemplate` 是核心实体，关键字段：
- `templateId`：模板唯一标识（如 `rag-qa`、`agent-plan`）
- `version`：版本号
- `systemPrompt`：模板正文，含 `{{变量名}}` 占位符
- `requiredParams`：必填参数列表
- `createdAtEpochMs` / `createdBy`：版本追踪信息

系统预置四个模板，通过 `PromptInitializer`（`@PostConstruct`）启动时注册：

| templateId | 用途 | 必填参数 |
|------------|------|----------|
| `rag-qa` | 知识库问答 | `context`, `query` |
| `agent-plan` | Agent 执行计划 | `goal`, `tools` |
| `summary` | 文本摘要 | `text` |
| `self-reflection` | 自我反思校验 | `query`, `answer` |

## 版本存储与检索

`PromptTemplateStore` 用 `ConcurrentHashMap<String, List<PromptTemplate>>` 存储——key 是 templateId，value 是该模板的版本列表。提供三种检索：
- `getLatest`：取最新版本（列表末尾）
- `getVersion`：精确匹配版本号
- `selectForTenant`：灰度分流选择

## 渲染流程

`PromptRenderer` 位于管道第 10 步（TPM 之后、LLM 调用之前）。渲染分两步：
1. **参数校验**：遍历 `requiredParams`，缺参直接抛异常
2. **变量替换**：把 `{{key}}` 替换为对应 value

```java
public String getRenderedPrompt(Map<String, String> params) {
    for (String param : requiredParams) {
        if (!params.containsKey(param) || params.get(param) == null || params.get(param).isEmpty()) {
            throw new IllegalArgumentException("Missing required param: " + param);
        }
    }
    String result = systemPrompt;
    for (Map.Entry<String, String> entry : params.entrySet()) {
        result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
}
```

网关中，渲染结果作为 `userMessage` 发给 LLM；没指定 templateId 则用脱敏后的原始 query：

```java
String userMessage = renderedPrompt != null ? renderedPrompt : maskedQuery;
```

## 灰度发布

`selectForTenant` 用租户 ID 哈希取模分流：

```java
int hash = Math.abs(tenantId.hashCode() % 100);
if (hash < 90) {
    return versions.get(0);           // 90% 走稳定版
} else {
    return versions.get(versions.size() - 1);  // 10% 走最新版
}
```

只有一个版本时直接返回不分流。和模型层 A/B 测试思路一致——同一租户稳定命中同一版本。90/10 而非 50/50 是因为 Prompt 灰度要保守，新版翻车影响面小。

## 错误处理

渲染失败返回 `prompt_render_error`，映射 HTTP 400（`BAD_REQUEST`）——这是调用方的问题（缺参或模板不存在），不是服务端故障。

## 关键踩坑

### 变量替换要避免误替换

当前用 `String.replace`，如果参数值里包含 `{{另一个变量}}`，会被后续迭代吃掉。生产环境应用单次遍历替换（如 `Matcher.appendReplacement`）。

### 缺参硬失败 vs 静默跳过

选硬失败是因为参数缺失会让模型收到不完整 Prompt，产生理解偏差。直接报错让调用方补传，问题定位更快。

### 同一租户必须稳定走同一版本

哈希取模保证这一点。如果改成随机分流，体验割裂，灰度数据也没法分析。

### 错误信息不泄露模板内部结构

`IllegalArgumentException` 消息含参数名，但网关层 catch 后统一返回 `prompt_render_error`，不会把内部信息暴露给终端用户。

### Prompt 渲染放在管道第 10 步

鉴权、限流、安全检查都可能拒绝请求，先做拦截避免对无效请求浪费渲染计算。

## 面试高频问题

1. **Prompt 为什么版本管理而不是硬编码？**
 硬编码改一个词要编译重启，版本管理支持热更新和灰度，可以快速迭代而不影响线上稳定性。

2. **灰度怎么保证同一用户稳定命中同一版本？**
 租户 ID 哈希取模，`Math.abs(tenantId.hashCode() % 100)` 对同一租户始终返回相同值。

3. **参数缺失为什么选硬失败而不是默认值？**
 缺失参数会让模型收到不完整 Prompt，硬失败让问题在调用方就暴露，而不是等输出质量下降才被发现。

4. **`String.replace` 做变量替换有什么隐患？**
 参数值含 `{{变量名}}` 格式文本时会被误替换，生产环境应改用单次遍历的模板引擎。

5. **Prompt 渲染为什么放在管道靠后？**
 鉴权、限流、安全检查先拦截无效请求，避免浪费渲染计算。

代码位置: `src/main/java/com/xb/platform/prompt/`
