package com.xb.platform.gateway;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

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

    public String checkInput(String text) {
        if (!injectionCheck) return null;
        if (text == null) return null;
        if (INJECTION_PATTERNS.matcher(text).find()) {
            return "INJECTION_DETECTED";
        }
        return null;
    }

    public boolean isInjectionDetected(String text) {
        return checkInput(text) != null;
    }

    public String filterOutput(String text) {
        if (text == null) return null;
        return SENSITIVE_WORDS.matcher(text).replaceAll("***");
    }
}