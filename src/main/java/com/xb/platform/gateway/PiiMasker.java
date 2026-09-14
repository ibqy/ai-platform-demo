package com.xb.platform.gateway;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PiiMasker {

    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[\\dXx]");
    private static final Pattern BANK_CARD = Pattern.compile("\\d{16,19}");

    public boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String mask(String text) {
        if (!enabled || text == null) return text;
        text = maskPattern(text, PHONE);
        text = maskPattern(text, ID_CARD);
        text = maskPattern(text, BANK_CARD);
        return text;
    }

    private String maskPattern(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String g = m.group();
            int keep = Math.min(4, g.length());
            m.appendReplacement(sb, "****" + g.substring(g.length() - keep));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}