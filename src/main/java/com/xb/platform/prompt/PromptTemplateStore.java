package com.xb.platform.prompt;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PromptTemplateStore - Prompt 模板存储
 *
 * 演示 Prompt 模板的版本管理：同一模板可存储多个版本，
 * 支持获取最新版本、指定版本、以及按租户灰度选择版本。
 * 灰度策略：90% 流量命中首版本，10% 命中最新版本，实现平滑升级。
 *
 * @author ibqy
 */
@Component
public class PromptTemplateStore {

    private final ConcurrentHashMap<String, List<PromptTemplate>> store = new ConcurrentHashMap<>();

    /**
     * 保存 Prompt 模板（支持多版本共存）
     * @param template 模板实体
     */
    public void save(PromptTemplate template) {
        store.computeIfAbsent(template.getTemplateId(), k -> new ArrayList<>())
                .add(template);
    }

    /**
     * 获取指定模板的最新版本
     * @param templateId 模板 ID
     * @return 最新版本模板，不存在返回 null
     */
    public PromptTemplate getLatest(String templateId) {
        List<PromptTemplate> versions = store.get(templateId);
        if (versions == null || versions.isEmpty()) return null;
        return versions.get(versions.size() - 1);
    }

    /**
     * 获取指定模板的特定版本
     * @param templateId 模板 ID
     * @param version 版本号
     * @return 匹配版本的模板，不存在返回 null
     */
    public PromptTemplate getVersion(String templateId, String version) {
        List<PromptTemplate> versions = store.get(templateId);
        if (versions == null) return null;
        return versions.stream()
                .filter(t -> t.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    /**
     * 按租户灰度选择模板版本（90% 命中首版本，10% 命中最新版）
     * @param templateId 模板 ID
     * @param tenantId 租户 ID（用于哈希分流）
     * @return 选中的模板版本
     */
    public PromptTemplate selectForTenant(String templateId, String tenantId) {
        List<PromptTemplate> versions = store.get(templateId);
        if (versions == null || versions.isEmpty()) return null;
        if (versions.size() == 1) return versions.get(0);

        int hash = Math.abs(tenantId.hashCode() % 100);
        if (hash < 90) {
            return versions.get(0);
        } else {
            return versions.get(versions.size() - 1);
        }
    }
}