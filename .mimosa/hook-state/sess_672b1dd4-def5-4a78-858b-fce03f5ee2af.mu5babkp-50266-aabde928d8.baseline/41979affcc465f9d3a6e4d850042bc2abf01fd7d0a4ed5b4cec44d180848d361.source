package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * <p>Hermes 模型列表响应。</p>
 *
 * <p>承载 OpenAI 兼容的对象类型和模型元数据集合。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelsResponse {
    /**
     * OpenAI 兼容协议中的对象类型。
     */
    private String object;
    /**
     * 协议数据集合或 SSE 原始数据。
     */
    private List<ModelData> data;

    /**
     * <p>Hermes 模型元数据。</p>
     *
     * <p>描述模型标识、对象类型、创建时间和所有者。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelData {
        /**
         * 对象唯一标识。
         */
        private String id;
        /**
         * OpenAI 兼容协议中的对象类型。
         */
        private String object;
        /**
         * Unix 创建时间戳。
         */
        private Long created;

        /**
         * 模型所有者。
         */
        @JsonProperty("owned_by")
        private String ownedBy;
    }
}
