package com.xb.platform.model;

/**
 * ModelType - AI 模型类型枚举
 *
 * 定义平台支持的模型类型：对话（CHAT）、向量嵌入（EMBEDDING）、
 * 重排序（RERANK）、多模态（MULTIMODAL）。路由器按类型筛选候选模型。
 *
 * @author ibqy
 */
public enum ModelType {
    CHAT,
    EMBEDDING,
    RERANK,
    MULTIMODAL
}