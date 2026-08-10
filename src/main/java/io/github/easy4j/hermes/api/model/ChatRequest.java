package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * <p>Hermes Chat Completion 请求模型。</p>
 *
 * <p>承载模型、消息、采样、停止条件、流式选项和调用方标识。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 聊天上下文消息列表。
     */
    private List<Message> messages;
    /**
     * 是否请求流式响应。
     */
    private Boolean stream;

    /**
     * 流式响应的扩展选项。
     */
    @JsonProperty("stream_options")
    private Map<String, Object> streamOptions;

    /**
     * 兼容 Chat Completion 的最大生成 token 数。
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 新版 Chat Completion 的最大生成 token 数。
     */
    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    /**
     * 采样温度；为空时由服务端决定。
     */
    private Double temperature;

    /**
     * 核采样概率阈值；为空时由服务端决定。
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * 频率惩罚系数。
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚系数。
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 可复现采样使用的随机种子。
     */
    private Integer seed;
    /**
     * 停止生成条件，可以是字符串或字符串列表。
     */
    private Object stop;
    /**
     * 调用方提供的终端用户标识。
     */
    private String user;

    /**
     * <p>将当前请求切换为流式模式。</p>
     *
     * @return 当前对象，便于链式调用
     * @since 1.0.0
     */
    public ChatRequest withStream() {
        ChatRequest r = new ChatRequest();
        r.model = this.model; r.messages = this.messages; r.stream = true;
        r.streamOptions = this.streamOptions; r.maxTokens = this.maxTokens;
        r.maxCompletionTokens = this.maxCompletionTokens; r.temperature = this.temperature;
        r.topP = this.topP; r.frequencyPenalty = this.frequencyPenalty;
        r.presencePenalty = this.presencePenalty; r.seed = this.seed;
        r.stop = this.stop; r.user = this.user;
        return r;
    }

    /**
     * <p>Hermes 会话消息模型。</p>
     *
     * <p>表示带会话归属、角色、内容和创建更新时间的持久化消息。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        /**
         * 消息角色。
         */
        private String role;
        /**
         * 消息内容。
         */
        private Object content;
    }

    /**
     * <p>多模态消息内容片段。</p>
     *
     * <p>按类型承载文本或图像地址等结构化内容。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentPart {
        /**
         * 内容片段类型，例如 text 或 image_url。
         */
        private String type;
        /**
         * 文本内容片段的正文。
         */
        private String text;

        /**
         * 图像内容片段的 URL 及扩展属性。
         */
        @JsonProperty("image_url")
        private Map<String, Object> imageUrl;
    }
}
