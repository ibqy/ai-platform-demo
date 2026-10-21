---
layout: home

hero:
  name: AI 平台中台实战
  text: 从 0 搭建企业级 AI 网关
  tagline: 14 步网关管道 + 智能路由（COST / PERFORMANCE / QUALITY）+ 三级缓存 + 虚拟线程，9 篇学习笔记拆解每个设计决策
  actions:
    - theme: brand
      text: 开始学习 →
      link: /01-模型管理
    - theme: alt
      text: GitHub 源码
      link: https://github.com/ibqy/ai-platform-demo

features:
  - icon: 🚪
    title: 14 步网关管道
    details: 请求从进入网关到返回的完整责任链拆解，每步只做一件事
  - icon: 🧭
    title: 智能路由
    details: COST / PERFORMANCE / QUALITY 三种策略选模型，兼顾成本与效果
  - icon: 🗂️
    title: 多级缓存
    details: 精确匹配 + 语义相似度两级命中，省钱又提速
  - icon: 🧵
    title: 虚拟线程
    details: Java 21 虚拟线程承载高并发模型调用
  - icon: 🏢
    title: 多租户架构
    details: 租户隔离、配额与模型白名单设计
  - icon: 🛡️
    title: 安全层
    details: Prompt 注入防护、敏感词过滤、内容审计
  - icon: 📊
    title: 可观测与计量
    details: Token 用量统计、调用链追踪、成本报表
  - icon: 💼
    title: 面试踩坑总结
    details: 把踩过的坑整理成面试可讲的故事
---

## 课程结构

| 章节 | 主题 | 对应代码 |
|:---|:---|:---|
| [01 · 模型管理](/01-模型管理) | 注册中心、健康探测、三种路由策略、A/B 测试 | [`ModelRegistry`](../src/main/java/com/xb/platform/model/ModelRegistry.java) / [`ModelRouter`](../src/main/java/com/xb/platform/model/ModelRouter.java) |
| [02 · 多租户架构](/02-多租户架构) | 行级隔离、ThreadLocal 上下文、配额熔断 | [`TenantContext`](../src/main/java/com/xb/platform/tenant/TenantContext.java) / [`TenantQuotaManager`](../src/main/java/com/xb/platform/tenant/TenantQuotaManager.java) |
| [03 · AI 网关](/03-AI网关) | 14 步管道、三层限流、失败快速提前返回 | [`AiGatewayService`](../src/main/java/com/xb/platform/gateway/AiGatewayService.java) |
| [04 · 多级缓存](/04-多级缓存) | LLM / Embedding / RAG 三级缓存与失效 | [`LlmCache`](../src/main/java/com/xb/platform/cache/LlmCache.java) / [`RagResultCache`](../src/main/java/com/xb/platform/cache/RagResultCache.java) |
| [05 · 异步任务](/05-异步任务) | 7 状态机、虚拟线程消费、重试与 DEAD | [`AsyncTask`](../src/main/java/com/xb/platform/task/AsyncTask.java) / [`TaskConsumer`](../src/main/java/com/xb/platform/task/TaskConsumer.java) |
| [06 · Prompt 平台](/06-Prompt平台) | 模板版本、变量渲染、灰度发布 | [`PromptTemplate`](../src/main/java/com/xb/platform/prompt/PromptTemplate.java) / [`PromptRenderer`](../src/main/java/com/xb/platform/prompt/PromptRenderer.java) |
| [07 · 安全层](/07-安全层) | 注入检测、PII 脱敏、输出过滤 | [`ContentSafetyFilter`](../src/main/java/com/xb/platform/gateway/ContentSafetyFilter.java) / [`PiiMasker`](../src/main/java/com/xb/platform/gateway/PiiMasker.java) |
| [08 · 可观测计量](/08-可观测计量) | 三维指标、Token 计费、告警收敛 | [`PlatformMetrics`](../src/main/java/com/xb/platform/observe/PlatformMetrics.java) / [`TokenMeter`](../src/main/java/com/xb/platform/observe/TokenMeter.java) |
| [09 · 面试踩坑总结](/09-面试踩坑总结) | 选型、架构、工程验证三类踩坑 | — |

## 代码地图

| 包 | 关键类 | 职责 |
|:---|:---|:---|
| 根包 | [`PlatformApplication`](../src/main/java/com/xb/platform/PlatformApplication.java) | 启动入口 |
| `model` | [`ModelRegistry`](../src/main/java/com/xb/platform/model/ModelRegistry.java) / [`ModelRouter`](../src/main/java/com/xb/platform/model/ModelRouter.java) / [`ModelHealthChecker`](../src/main/java/com/xb/platform/model/ModelHealthChecker.java) / [`ModelQuota`](../src/main/java/com/xb/platform/model/ModelQuota.java) | 模型注册、健康探测、路由与 TPM 配额 |
| `tenant` | [`TenantContext`](../src/main/java/com/xb/platform/tenant/TenantContext.java) / [`TenantQuotaManager`](../src/main/java/com/xb/platform/tenant/TenantQuotaManager.java) | 租户上下文与每日配额 |
| `gateway` | [`AiGatewayService`](../src/main/java/com/xb/platform/gateway/AiGatewayService.java) / [`GatewayRateLimiter`](../src/main/java/com/xb/platform/gateway/GatewayRateLimiter.java) / [`ContentSafetyFilter`](../src/main/java/com/xb/platform/gateway/ContentSafetyFilter.java) / [`PiiMasker`](../src/main/java/com/xb/platform/gateway/PiiMasker.java) | 14 步管道、限流、安全过滤、脱敏 |
| `cache` | [`LlmCache`](../src/main/java/com/xb/platform/cache/LlmCache.java) / [`EmbeddingCache`](../src/main/java/com/xb/platform/cache/EmbeddingCache.java) / [`RagResultCache`](../src/main/java/com/xb/platform/cache/RagResultCache.java) | 三级缓存与懒过期 |
| `task` | [`AsyncTask`](../src/main/java/com/xb/platform/task/AsyncTask.java) / [`TaskPublisher`](../src/main/java/com/xb/platform/task/TaskPublisher.java) / [`TaskConsumer`](../src/main/java/com/xb/platform/task/TaskConsumer.java) / [`TaskStore`](../src/main/java/com/xb/platform/task/TaskStore.java) | 异步任务队列与状态机 |
| `prompt` | [`PromptTemplate`](../src/main/java/com/xb/platform/prompt/PromptTemplate.java) / [`PromptTemplateStore`](../src/main/java/com/xb/platform/prompt/PromptTemplateStore.java) / [`PromptRenderer`](../src/main/java/com/xb/platform/prompt/PromptRenderer.java) | Prompt 版本管理与渲染 |
| `functioncall` | [`ToolDefinition`](../src/main/java/com/xb/platform/functioncall/ToolDefinition.java) / [`ToolRegistry`](../src/main/java/com/xb/platform/functioncall/ToolRegistry.java) | 工具定义与注册表 |
| `observe` | [`PlatformMetrics`](../src/main/java/com/xb/platform/observe/PlatformMetrics.java) / [`TokenMeter`](../src/main/java/com/xb/platform/observe/TokenMeter.java) / [`AlertRules`](../src/main/java/com/xb/platform/observe/AlertRules.java) | 三维指标、成本记账、告警规则 |

## 在路线中的位置

> **Java × AI 进阶路线** · 阶段 4「生产级 AI 工程」第 2 站 · 🏁 路线终点  
> 上一站：[RAG 混合检索高阶](https://ibqy.github.io/rag-hybrid-search-demo/) → **企业级 AI 平台**  
> 完整路线：[ibqy.github.io/curriculum](https://ibqy.github.io/curriculum)


