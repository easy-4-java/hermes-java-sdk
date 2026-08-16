package io.github.easy4j.hermes.api.sse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import io.github.easy4j.hermes.util.HermesObjectMapper;
import lombok.Data;

import java.util.Map;

/**
 * <p>Hermes SSE 事件值对象。</p>
 *
 * <p>保存事件类型、原始数据及扩展字段，并从不同服务端增量格式中提取文本。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {

    /**
     * SSE event 字段。
     */
    private String event;
    /**
     * 协议数据集合或 SSE 原始数据。
     */
    private String data;

    /**
     * <p>将 SSE data 解析为键值映射。</p>
     *
     * @return SSE data 解析得到的键值映射；无法解析时返回 {@code null}
     * @since 1.0.0
     */
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

    /**
     * <p>将 SSE data 解析为 Jackson 树节点。</p>
     *
     * @return 解析成功的 JSON 树；无法解析时返回 {@code null}
     * @since 1.0.0
     */
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

    /**
     * <p>从 SSE 事件中提取文本增量。</p>
     *
     * @return 当前事件携带的文本增量；事件不含文本时返回 {@code null}
     * @since 1.0.0
     */
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
