package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * <p>Hermes 会话模型。</p>
 *
 * <p>表示会话标识、标题、父会话、生命周期时间、结束原因和扩展元数据。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {
    /**
     * 对象唯一标识。
     */
    private String id;
    /**
     * 会话标题。
     */
    private String title;

    /**
     * 父会话标识。
     */
    @JsonProperty("parent_id")
    private String parentId;

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

    /**
     * 会话结束原因。
     */
    @JsonProperty("end_reason")
    private String endReason;

    /**
     * 服务端扩展元数据。
     */
    private Map<String, Object> metadata;
}
