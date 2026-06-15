package io.github.hiwepy.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hiwepy.hermes.util.HermesObjectMapper;
import lombok.Data;

import java.util.Map;

/**
 * SSE 事件值对象 — 封装 SSE event: / data: 行及其解析行为。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {

    private String event;
    private String data;

    /** 将 {@link #data} JSON 解析为 {@code Map<String, Object>}，解析失败返回 null。 */
    public Map<String, Object> getDataAsMap() {
        if (data == null) {
            return null;
        }
        try {
            return HermesObjectMapper.INSTANCE.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) { return null; }
    }

    /** 将 {@link #data} JSON 解析为 {@link JsonNode}，解析失败返回 null。 */
    public JsonNode getDataAsNode() {
        if (data == null) {
            return null;
        }
        try { return HermesObjectMapper.INSTANCE.readTree(data); } catch (Exception e) { return null; }
    }

    /**
     * 从不同 SSE 事件格式中提取文本增量，无法提取时返回 null：
     * <ul>
     *   <li>Chat Completion: {@code choices[0].delta.content}</li>
     *   <li>Response: {@code delta} (response.output_text.delta)</li>
     *   <li>Session: {@code delta} (assistant.delta)</li>
     *   <li>Response output_text.delta: {@code delta.text}</li>
     * </ul>
     */
    public String deltaText() {
        if (data == null) {
            return null;
        }
        try {
            JsonNode root = HermesObjectMapper.INSTANCE.readTree(data);

            // Chat Completions: choices[0].delta.content
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                if (!delta.isMissingNode()) {
                    JsonNode content = delta.path("content");
                    if (!content.isMissingNode() && !content.isNull()) {
                        return content.asText();
                    }
                }
            }

            // Responses / Session: delta (string)
            JsonNode delta = root.path("delta");
            if (delta.isTextual()) {
                return delta.asText();
            }

            // Response output_text.delta: delta.text
            if (delta.isObject()) {
                JsonNode text = delta.path("text");
                if (text.isTextual()) {
                    return text.asText();
                }
            }

            return null;
        } catch (Exception e) { return null; }
    }
}
