package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Hermes Agent Run 创建请求。</p>
 *
 * <p>承载输入、会话、指令、历史上下文、续接响应和模型选择。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunCreateRequest {
    /**
     * 请求输入。
     */
    private String input;

    /**
     * 关联会话标识。
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 本次请求使用的系统指令。
     */
    private String instructions;

    /**
     * 兼容服务端的历史会话上下文。
     */
    @JsonProperty("conversation_history")
    private Object conversationHistory;

    /**
     * 需要续接的上一个 Responses API 响应标识。
     */
    @JsonProperty("previous_response_id")
    private String previousResponseId;

    /**
     * 需要续接的会话标识。
     */
    private String conversation;

    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
}
