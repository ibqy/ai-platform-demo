package com.xb.platform.model;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ModelInitializer - 模型初始化器
 *
 * 演示 AI 平台启动时如何预注册可用模型：包括 Chat 模型（DeepSeek、Qwen）
 * 和 Embedding 模型（BGE）。每个模型携带端点、价格、上下文窗口等元信息，
 * 供路由器和配额管理器使用。
 *
 * @author ibqy
 */
@Component
public class ModelInitializer {

    @Autowired
    private ModelRegistry registry;

    /**
     * 启动时预注册所有可用模型到注册中心。
     *
     * <p>通过 @PostConstruct 在 Spring 容器就绪后自动执行，
     * 确保后续请求到达时模型路由器已有可用候选。</p>
     */
    @PostConstruct
    public void init() {
        ModelMeta deepseek = new ModelMeta(
                "deepseek-chat", ModelType.CHAT, "DeepSeek", "v3",
                "https://api.deepseek.com/v1/chat", "sk-ds-key", 32768,
                0.5, 100
        );
        registry.register(deepseek);

        ModelMeta qwen = new ModelMeta(
                "qwen-turbo", ModelType.CHAT, "Alibaba", "v2",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat", "sk-qw-key", 8192,
                0.3, 200
        );
        qwen.getLabels().add("低成本");
        registry.register(qwen);

        ModelMeta bge = new ModelMeta(
                "bge-embedding", ModelType.EMBEDDING, "BAAI", "v1.5",
                "http://localhost:11434/api/embed", "", 512,
                0.1, 50
        );
        registry.register(bge);
    }
}