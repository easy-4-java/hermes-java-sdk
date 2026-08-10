package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <p>Hermes Agent Run 状态模型。</p>
 *
 * <p>承载运行标识、状态、会话、输出和 token 使用量。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunStatus {
    /**
     * OpenAI 兼容协议中的对象类型。
     */
    private String object;

    /**
     * Agent Run 标识。
     */
    @JsonProperty("run_id")
    private String runId;

    /**
     * 服务端状态。
     */
    private String status;

    /**
     * 关联会话标识。
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 服务端生成的结构化输出。
     */
    private String output;
    /**
     * 本次调用的 token 使用量。
     */
    private Usage usage;

    /**
     * <p>Token 使用量。</p>
     *
     * <p>记录输入、输出和总 token 数；具体字段随所属响应模型而定。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        /**
         * 输入消耗的 token 数。
         */
        @JsonProperty("input_tokens")
        private Long inputTokens;

        /**
         * 输出消耗的 token 数。
         */
        @JsonProperty("output_tokens")
        private Long outputTokens;

        /**
         * 输入与输出 token 总数。
         */
        @JsonProperty("total_tokens")
        private Long totalTokens;
    }
}
