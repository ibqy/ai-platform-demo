# AI Platform Demo

> 企业 AI 能力中台学习总结项目 · 作者：xb · 日期：2026-09

本项目是一个**模拟企业级 AI 网关中台**的教学项目。它不依赖任何云厂商的网关产品，用纯 Java + Spring Boot 从零实现了多模型路由、多租户隔离、安全过滤、代价计量、多级缓存等核心能力。所有模块基于学习笔记驱动，代码带详细中文注释。

---

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 基础语言（虚拟线程、record 等） |
| Spring Boot | 4.1.0 | Web 框架 + 自动配置 |
| Spring AI | 2.0.1 | OpenAI 兼容 LLM 调用（ChatClient） |
| Redis | — | 多级缓存（LLM / Embedding / RAG） |
| Kafka | — | 异步任务队列（文档解析、批量向量化） |
| Resilience4j | — | 熔断、重试、限流 |
| Micrometer + OTEL | — | 可观测（Metrics + Tracing） |

---

## 模块地图

```
ai-platform-demo/
├── model/        模型管理层 — 注册、探活、路由、A/B、TPM配额
├── tenant/       多租户 — 行级隔离、配额、白名单、熔断
├── gateway/      AI 网关 — 管道编排、限流、鉴权、注入检测、PII、LLM调用、SSE流式、全局异常处理
├── functioncall/ Function Calling — 工具定义、工具注册中心、OpenAI 格式输出
├── cache/        多级缓存 — LLM回答、Embedding、RAG检索
├── task/         异步任务 — 文档解析、批量向量化、状态机+重试
├── prompt/       Prompt平台 — 模板版本管理、灰度发布、参数校验
└── observe/      可观测计量 — 三维指标、Token成本、告警规则
```

---

## 快速开始

### 前置条件

- JDK 21+
- Maven 3.9+
- （可选）Redis 和 Kafka 用于缓存和异步任务
- 一个 OpenAI 兼容的 API 端点（OpenAI / DeepSeek / Ollama / 智谱 等）

### 配置

```bash
# 必需：API Key
export OPENAI_API_KEY=sk-your-key-here

# 可选：自定义 Base URL 和模型
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_MODEL=deepseek-chat
```

### 启动

```bash
cd ai-platform-demo
mvn spring-boot:run
```

服务启动后访问 `http://localhost:8080`。

### 测试

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "token": "demo-token",
    "tenantId": "tenant-a",
    "query": "什么是 RAG？",
    "modelId": "gpt-4o-mini"
  }'
```

> 当前项目为教学 Demo，认证 token 使用占位值即可通过。

---

## 架构概览

```
                     ┌─────────────┐
                     │   Client    │
                     └──────┬──────┘
                            │ HTTP POST
                            ▼
              ┌─────────────────────────┐
              │    AiGatewayService     │
              │  ┌───────────────────┐  │
              │  │ 1. 限流            │  │  ← Resilience4j RateLimiter
              │  │ 2. 租户配额        │  │  ← TenantQuotaManager
              │  │ 3. JWT 鉴权        │  │  ← GatewayAuthFilter
              │  │ 4. 注入检测        │  │  ← ContentSafetyFilter
              │  │ 5. PII 脱敏        │  │  ← PiiMasker
              │  │ 6. 缓存查询        │  │  ← LlmCache (Redis)
              │  │ 7. 模型路由        │  │  ← ModelRouter (COST/PERF/QUALITY)
              │  │ 8. 白名单校验      │  │  ← Tenant.modelWhitelist
              │  │ 9. TPM 配额        │  │  ← ModelQuota
              │  │ 10. Prompt 渲染    │  │  ← PromptRenderer
              │  │ 11. LLM 调用       │  │  ← ChatClient (Spring AI)
              │  │ 12. 输出过滤       │  │  ← ContentSafetyFilter
              │  │ 13. 计量记账       │  │  ← TokenMeter
              │  │ 14. 回写缓存       │  │  ← LlmCache
              │  └───────────────────┘  │
              └─────────────────────────┘
                            │
                            ▼
                     ┌─────────────┐
                     │   Client    │
                     └─────────────┘
```

管道顺序从便宜到贵排列：被挡掉的请求不消耗任何模型调用费用。

---

## 模型路由策略

| 策略 | 选型逻辑 | 适用场景 |
|------|----------|----------|
| `COST` | 选最便宜的可用模型 | 批量处理、非敏感任务 |
| `PERFORMANCE` | 选延迟最低的 | 实时对话、在线服务 |
| `QUALITY` | 选上下文窗口最大的 | 复杂推理、长文档处理 |

路由支持 A/B 测试：用租户 ID 哈希做灰度分流，同一租户多次请求稳定命中同一组。

---

## 缓存策略

| 缓存层 | Key | TTL | 失效策略 |
|--------|-----|-----|----------|
| LLM 回答 | query + modelId + kbVersion | 300s | TTL + 知识库更新主动清除 |
| Embedding | text 原文 | 600s | TTL 自然过期 |
| RAG 检索 | query + kbVersion | 300s | 知识库更新主动清除 |

---

## 安全防线

| 防线 | 位置 | 作用 |
|------|------|------|
| 提示注入检测 | 管道前置（步骤4） | 识别 Prompt Injection 攻击 |
| PII 脱敏 | 管道中置（步骤5） | 手机号/身份证/银行卡脱敏 |
| 输出过滤 | 管道后置（步骤12） | 屏蔽模型返回的敏感内容 |

---

## 学习资源

每篇文档都是该模块的实战学习笔记，记录了设计决策、踩坑经验和面试高频问题：

| 文档 | 内容 |
|------|------|
| [01-模型管理](docs/01-模型管理.md) | 注册中心、健康探测、路由策略、TPM 配额 |
| [02-多租户架构](docs/02-多租户架构.md) | 行级隔离、TenantContext、白名单、配额熔断 |
| [03-AI 网关](docs/03-AI网关.md) | 管道编排、三层限流、与普通网关的区别 |
| [04-多级缓存](docs/04-多级缓存.md) | 三级缓存设计、Key 设计、失效策略 |
| [05-异步任务](docs/05-异步任务.md) | 任务状态机、DEAD 状态、虚拟线程 |
| [06-Prompt 平台](docs/06-Prompt平台.md) | 版本管理、灰度发布、参数校验 |
| [07-安全层](docs/07-安全层.md) | 注入检测、PII 脱敏、输出过滤 |
| [08-可观测计量](docs/08-可观测计量.md) | 三维指标、Token 成本、告警规则 |
| [09-面试踩坑总结](docs/09-面试踩坑总结.md) | AtomicDouble、replaceAll、ThreadLocal 踩坑合集 |

---

## 实现边界

### 已实现 ✅

- 8 大模块：模型管理、多租户、AI 网关、Function Calling、多级缓存、异步任务、Prompt 平台、可观测计量
- 14 步管道编排（限流→鉴权→注入检测→PII 脱敏→缓存→路由→Prompt 渲染→LLM 调用→计量→回写）
- SSE 流式输出（`/api/ai/chat/stream`）—— 逐 Token 推送，虚拟线程异步执行
- 全局异常处理器 —— 错误码→HTTP 状态码映射 + correlationId 链路追踪
- Jakarta Bean Validation —— `@Valid` + `@NotBlank` 请求参数校验
- Function Calling —— 工具定义（JSON Schema）+ 工具注册中心 + OpenAI 格式输出
- 三级缓存（LLM 回答 / Embedding / RAG 检索）+ 知识库版本主动失效
- 任务状态机（QUEUED→PROCESSING→SUCCEEDED/FAILED→RETRYING→DEAD）
- 多租户 ThreadLocal 隔离 + 配额管理
- 模型路由（COST/PERFORMANCE/QUALITY）+ A/B 灰度分流
- Token 计量与成本核算（租户×模型二维表）
- 64 个单元测试（JUnit 5），覆盖核心逻辑

### 教学简化 ⚠️

- **缓存存储**：使用 `ConcurrentHashMap` 内存缓存，生产应为 Redis
- **任务队列**：内存存储 + 虚拟线程消费，生产应为 Kafka + 持久化
- **鉴权**：Token 校验使用占位值，未接入真实 JWT/OAuth
- **注入检测**：基于正则模式匹配，未使用分类模型
- **PII 脱敏**：仅覆盖手机号/身份证/银行卡，规则可扩展

### 未实现 ❌

- 分布式部署（Session 共享、分布式锁）
- 向量数据库集成（Milvus/Qdrant）
- 真实的多模型 A/B 测试数据分析
- 前端管理控制台
- 审计日志与合规报表

## 测试

```bash
mvn test    # 64 个单元测试，约 25 秒
```

| 测试类 | 数量 | 覆盖范围 |
|--------|------|---------|
| PlatformApplicationTests | 1 | 启动类冒烟测试 |
| PlatformCoreTest | 9 | 路由/健康探活/A-B 分流/配额/白名单/Prompt/PII/注入检测 |
| CacheSystemTest | 8 | 三级缓存命中/隔离/TTL 过期/按 KB 失效 |
| TaskStateMachineTest | 7 | 任务状态机/重试/DEAD/TaskStore CRUD |
| TokenMeterTest | 8 | Token 计量/成本核算/多租户隔离/报表 |
| TenantIsolationTest | 4 | ThreadLocal 隔离/跨线程不可见 |
| GatewayInfraTest | 7 | 网关限流（全局+租户）/模型 TPM 配额 |
| GlobalExceptionHandlerTest | 9 | 错误码→HTTP 映射/GatewayException/错误响应格式 |
| FunctionCallTest | 7 | 工具定义/OpenAI 格式/注册中心/按类别查询/不可变列表 |
| StreamingTest | 4 | SSE 流式 chunk 推送/计量/注入拦截/鉴权拦截 |

---

## 相关链接

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot 文档](https://docs.spring.io/spring-boot/reference/)
- [回到我的主页](https://github.com/ibqy)
