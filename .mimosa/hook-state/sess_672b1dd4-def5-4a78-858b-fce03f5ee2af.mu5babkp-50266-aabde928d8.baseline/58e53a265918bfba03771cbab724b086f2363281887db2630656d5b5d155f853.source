package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * <p>Hermes Chat Completion 响应模型。</p>
 *
 * <p>承载响应标识、候选消息、完成原因以及 token 使用量。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
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
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 聊天候选响应列表。
     */
    private List<Choice> choices;
    /**
     * 本次调用的 token 使用量。
     */
    private Usage usage;

    /**
     * <p>聊天候选结果。</p>
     *
     * <p>包含候选序号、消息与服务端完成原因。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        /**
         * 候选响应序号。
         */
        private Integer index;
        /**
         * 可用性报告或异常消息。
         */
        private Message message;

        /**
         * 候选响应结束原因。
         */
        @JsonProperty("finish_reason")
        private String finishReason;

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
        public static class Message {
            /**
             * 消息角色。
             */
            private String role;
            /**
             * 消息内容。
             */
            private String content;
        }
    }

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
         * 输入提示消耗的 token 数。
         */
        @JsonProperty("prompt_tokens")
        private Long promptTokens;

        /**
         * 生成内容消耗的 token 数。
         */
        @JsonProperty("completion_tokens")
        private Long completionTokens;

        /**
         * 输入与输出 token 总数。
         */
        @JsonProperty("total_tokens")
        private Long totalTokens;
    }
}
