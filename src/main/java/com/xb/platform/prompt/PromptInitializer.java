package com.xb.platform.prompt;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptInitializer {

    @Autowired
    private PromptTemplateStore store;

    @PostConstruct
    public void init() {
        long now = System.currentTimeMillis();

        store.save(new PromptTemplate(
                "rag-qa", "1.0",
                "你是一个知识库问答助手，请基于以下上下文回答用户问题。\n上下文：{{context}}\n用户问题：{{query}}",
                List.of("context", "query"), now, "system"
        ));

        store.save(new PromptTemplate(
                "agent-plan", "1.0",
                "请为以下用户目标制定执行计划。\n目标：{{goal}}\n可用工具：{{tools}}",
                List.of("goal", "tools"), now, "system"
        ));

        store.save(new PromptTemplate(
                "summary", "1.0",
                "请对以下文本进行摘要。\n文本：{{text}}",
                List.of("text"), now, "system"
        ));

        store.save(new PromptTemplate(
                "self-reflection", "1.0",
                "请检查你的回答是否准确、完整，如有不足请修正。\n原始问题：{{query}}\n你的回答：{{answer}}",
                List.of("query", "answer"), now, "system"
        ));
    }
}