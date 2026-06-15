package io.github.hiwepy.hermes.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Hermes AI 响应中解析 JSON。
 * <p>
 * 采用三段式策略从 AI 文本响应中提取 JSON：
 * 1. 优先从 ```json ... ``` 代码块中提取
 * 2. 尝试直接解析整个文本为 JSON
 * 3. 回退到正则匹配裸 JSON 对象
 * </p>
 */
@Slf4j
public class HermesJsonParser {

    private static final ObjectMapper MAPPER = HermesObjectMapper.INSTANCE;

    /**
     * 匹配 ```json ... ``` 代码块中的 JSON。
     */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```json\\s*\\n?(\\{.*?})\\s*\\n?```", Pattern.DOTALL);

    /**
     * 匹配裸 JSON 对象（以 { 开头，以 } 结尾）。
     */
    private static final Pattern BARE_JSON_PATTERN = Pattern.compile(
            "(\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*})", Pattern.DOTALL);

    /**
     * 从纯文本中尝试提取并解析 JSON。
     *
     * @param text AI 响应文本
     * @return 解析后的 JSON Map，无法解析则返回 null
     */
    public Map<String, Object> parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 策略 1: 从 ```json ... ``` 代码块中提取
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse JSON from code block: {}", json, e);
            }
        }

        // 策略 2: 尝试直接解析整个文本为 JSON
        try {
            return MAPPER.readValue(text.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
        }

        // 策略 3: 尝试提取裸 JSON 对象
        Matcher bareMatcher = BARE_JSON_PATTERN.matcher(text);
        while (bareMatcher.find()) {
            String json = bareMatcher.group(1);
            try {
                Map<String, Object> parsed = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
                // 至少要有有效字段才认为是可解析的 JSON
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
