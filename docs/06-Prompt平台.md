# Prompt 模板平台 — 学习笔记

## 为什么需要管理

Prompt 硬编码在代码里，改一个词要编译重启。版本对比更是无从谈起。

## 模板版本管理

`PromptTemplateStore` 按 templateId 存储版本列表。模板包含 `requiredParams` 和占位符。

## 灰度发布

`selectForTenant` 用租户 ID 哈希分流：90% 走稳定版，10% 走灰度版。

## 参数校验

缺参硬失败——Prompt 参数缺失会导致模型理解偏差，不如直接报错让调用方补传。

## 关键踩坑

- 变量替换要避免误替换 <span v-pre>`{{`</span> 字面量
- 同一租户必须稳定走同一版本
- 缺参错误信息避免泄露模板变量名

代码位置: `src/main/java/com/xb/platform/prompt/`