package io.github.easy4j.hermes.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Hermes AI 文本 JSON 解析工具。</p>
 *
 * <p>依次尝试 JSON 代码块、完整文本和裸 JSON 对象，无法解析时返回空结果。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesJsonParser {

    /**
     * 复用 SDK 统一 JSON 配置的只读解析器。
     */
    private static final ObjectMapper MAPPER = HermesObjectMapper.INSTANCE;

    /**
     * 提取 Markdown JSON 代码块内对象文本的跨行正则表达式。
     */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```json\\s*\\n?(\\{.*?})\\s*\\n?```", Pattern.DOTALL);

    /**
     * 从普通文本中提取有限嵌套 JSON 对象的回退正则表达式。
     */
    private static final Pattern BARE_JSON_PATTERN = Pattern.compile(
            "(\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*})", Pattern.DOTALL);

    /**
     * <p>从 AI 文本响应中解析第一个有效 JSON 对象。</p>
     *
     * @param text 可能包含 JSON 的 AI 文本响应
     * @return 首个非空 JSON 对象；没有有效对象时返回 {@code null}
     * @since 1.0.0
     */
    public Map<String, Object> parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 策略 1：优先解析模型明确标注的 Markdown JSON 代码块，减少正文说明文字干扰。
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse JSON from code block: contentLength={}", json.length(), e);
            }
        }

        // 策略 2：代码块不存在或无效时，把去除首尾空白后的完整文本视为 JSON。
        try {
            return MAPPER.readValue(text.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
        }

        // 策略 3：最后扫描正文中的裸对象；空对象不作为有效业务结果，继续寻找下一个候选。
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
