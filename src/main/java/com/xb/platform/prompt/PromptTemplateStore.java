package com.xb.platform.prompt;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptTemplateStore {

    private final ConcurrentHashMap<String, List<PromptTemplate>> store = new ConcurrentHashMap<>();

    public void save(PromptTemplate template) {
        store.computeIfAbsent(template.getTemplateId(), k -> new ArrayList<>())
                .add(template);
    }

    public PromptTemplate getLatest(String templateId) {
        List<PromptTemplate> versions = store.get(templateId);
        if (versions == null || versions.isEmpty()) return null;
        return versions.get(versions.size() - 1);
    }

    public PromptTemplate getVersion(String templateId, String version) {
        List<PromptTemplate> versions = store.get(templateId);
        if (versions == null) return null;
        return versions.stream()
                .filter(t -> t.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

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