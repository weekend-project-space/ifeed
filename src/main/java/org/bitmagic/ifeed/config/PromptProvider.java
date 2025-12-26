package org.bitmagic.ifeed.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/12/26
 **/
@Slf4j
@Component
public class PromptProvider {

    @Value("classpath:prompts/*.*")
    private Resource[] promptResources;

    private final Map<String, Object> promptCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (Resource resource : promptResources) {
            try {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                // 以文件名作为 Key (去掉后缀)，例如 "Persona2Query"
                String key = filename.substring(0, filename.lastIndexOf("."));
                if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
                    Yaml yaml = new Yaml();
                    Object data = yaml.load(content);
                    if (data instanceof Map<?, ?> map) {
                        promptCache.put(key, map);
                    } else {
                        throw new IllegalStateException("Invalid yml format");
                    }
                } else {
                    promptCache.put(key, content);

                }
                log.info("加载提示词成功: [{}]", key);
            } catch (IOException e) {
                log.error("加载资源失败: {}", resource.getFilename(), e);
            }
        }
    }

    public String getPrompt(String key) {
        Object data = promptCache.getOrDefault(key, "");
        if (data instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n"));
        } else if (data instanceof String) {
            return (String) data;
        } else {
            return data.toString();
        }
    }

    public String getPrompt(String group, String key) {
        Object data = promptCache.get(group);
        if (data instanceof Map<?, ?> map) {
            return map.get(key).toString();
        } else {
            throw new IllegalStateException("Invalid group: " + group);
        }
    }

    public Map<String, String> getPromptGroup(String group) {
        Object data = promptCache.get(group);
        if (data instanceof Map<?, ?> map) {
            return Collections.unmodifiableMap((Map<String, String>) map);
        }
        throw new IllegalStateException("Invalid group: " + group);
    }
}
