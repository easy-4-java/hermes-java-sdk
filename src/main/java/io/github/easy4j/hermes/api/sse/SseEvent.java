package io.github.easy4j.hermes.api.sse;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.easy4j.hermes.util.HermesObjectMapper;
import lombok.Data;

import java.util.Map;

/**
 * Hermes SSE 事件值对象，封装 {@code event:}、{@code data:} 及增量解析。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {

    private String event;
    private String data;

    public Map<String, Object> getDataAsMap() {
        if (data == null) {
            return null;
        }
        try {
            return HermesObjectMapper.INSTANCE.readValue(data, new TypeReference<Map<String, Object>>() { });
        } catch (Exception error) {
            return null;
        }
    }

    public JsonNode getDataAsNode() {
        if (data == null) {
            return null;
        }
        try {
            return HermesObjectMapper.INSTANCE.readTree(data);
        } catch (Exception error) {
            return null;
        }
    }

    public String deltaText() {
        if (data == null) {
            return null;
        }
        try {
            JsonNode root = HermesObjectMapper.INSTANCE.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("delta").path("content");
                if (!content.isMissingNode() && !content.isNull()) {
                    return content.asText();
                }
            }
            JsonNode delta = root.path("delta");
            if (delta.isTextual()) {
                return delta.asText();
            }
            if (delta.isObject() && delta.path("text").isTextual()) {
                return delta.path("text").asText();
            }
            return null;
        } catch (Exception error) {
            return null;
        }
    }
}
