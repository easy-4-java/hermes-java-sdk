package io.github.easy4j.hermes.api.sse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

    /** SSE id 字段；服务端未提供时保持 null。 */
    private String id;

    /**
     * SSE event 字段。
     */
    private String event;
    /**
     * 协议数据集合或 SSE 原始数据。
     */
    private String data;

    /** 本地接收时间，不作为服务端事件身份。 */
    private long receivedAtEpochMillis;

    /**
     * 从已保留的原始 SSE 帧创建兼容事件视图。
     */
    public static SseEvent fromFrame(SseFrame frame) {
        SseEvent event = new SseEvent();
        event.setId(frame.getId());
        event.setEvent(frame.getEvent());
        event.setData(frame.getData());
        event.setReceivedAtEpochMillis(frame.getReceivedAtEpochMillis());
        return event;
    }

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
            return extractDeltaText(HermesObjectMapper.INSTANCE.readTree(data));
        } catch (Exception error) {
            return null;
        }
    }

    private String extractDeltaText(JsonNode root) {
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

        JsonNode nestedData = root.path("data");
        if (nestedData.isTextual()) {
            try {
                return extractDeltaText(HermesObjectMapper.INSTANCE.readTree(nestedData.asText()));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
