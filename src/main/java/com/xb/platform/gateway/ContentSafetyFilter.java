package com.xb.platform.gateway;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ContentSafetyFilter - AI 网关内容安全过滤器
 *
 * 演示 AI 应用中的输入/输出安全策略：输入侧检测 Prompt 注入攻击
 * （如"忽略你的指令"），输出侧过滤敏感信息（如密码、密钥、银行卡号）。
 * 这是生产 AI 网关必不可少的安全防线。
 *
 * @author ibqy
 */
@Component
public class ContentSafetyFilter {

    private static final Pattern INJECTION_PATTERNS = Pattern.compile(
            "ignore your instructions|forget your prompt|system prompt|【忽略】",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SENSITIVE_WORDS = Pattern.compile(
            "密码:|secret:|\\d{16}",
            Pattern.CASE_INSENSITIVE
    );

    public boolean injectionCheck = true;

    public void setInjectionCheck(boolean enabled) {
        this.injectionCheck = enabled;
    }

    /**
     * 检测输入文本是否包含 Prompt 注入攻击
     * @param text 待检测的用户输入
     * @return 检测到注入返回错误码，否则返回 null
     */
    public String checkInput(String text) {
        if (!injectionCheck) return null;
        if (text == null) return null;
        if (INJECTION_PATTERNS.matcher(text).find()) {
            return "INJECTION_DETECTED";
        }
        return null;
    }

    /**
     * 判断输入是否包含注入攻击（checkInput 的布尔简化版）
     * @param text 待检测文本
     * @return 检测到注入返回 true
     */
    public boolean isInjectionDetected(String text) {
        return checkInput(text) != null;
    }

    /**
     * 过滤 LLM 输出中的敏感信息（密码、密钥、银行卡号）
     * @param text LLM 原始输出
     * @return 脱敏后的文本
     */
    public String filterOutput(String text) {
        if (text == null) return null;
        return SENSITIVE_WORDS.matcher(text).replaceAll("***");
    }
}