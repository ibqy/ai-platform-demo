package com.xb.platform.prompt;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PromptInitializer - Prompt 模板初始化器
 *
 * 演示 AI 平台启动时预置常用 Prompt 模板：RAG 问答、Agent 规划、
 * 文本摘要、自我反思。这些模板是 Prompt 工程的基础组件，
 * 支持通过模板引擎渲染，实现 Prompt 的版本化管理与灰度发布。
 *
 * @author ibqy
 */
@Component
public class PromptInitializer {

    @Autowired
    private PromptTemplateStore store;

    /**
     * 启动时预置常用 Prompt 模板到模板仓库。
     *
     * <p>通过 @PostConstruct 在容器就绪后自动执行，
     * 预置 RAG 问答、Agent 规划、摘要、自我反思等模板，
     * 供后续 Prompt 渲染和版本管理使用。</p>
     */
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