package com.xb.platform.model;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ModelInitializer {

    @Autowired
    private ModelRegistry registry;

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