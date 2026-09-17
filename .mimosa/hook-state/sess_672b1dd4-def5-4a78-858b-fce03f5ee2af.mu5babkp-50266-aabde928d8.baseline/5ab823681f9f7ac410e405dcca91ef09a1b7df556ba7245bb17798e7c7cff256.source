package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <p>Hermes 会话消息模型。</p>
 *
 * <p>表示带会话归属、角色、内容和创建更新时间的持久化消息。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    /**
     * 对象唯一标识。
     */
    private String id;

    /**
     * 关联会话标识。
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 消息角色。
     */
    private String role;
    /**
     * 消息内容。
     */
    private String content;

    /**
     * 服务端返回的创建时间。
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 服务端返回的最后更新时间。
     */
    @JsonProperty("updated_at")
    private String updatedAt;
}
