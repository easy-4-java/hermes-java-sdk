package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * <p>Hermes Responses API 请求模型。</p>
 *
 * <p>承载输入、指令、上下文续接、采样参数和存储策略。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseRequest {
    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 请求输入。
     */
    private Object input;
    /**
     * 本次请求使用的系统指令。
     */
    private String instructions;
    /**
     * 是否请求流式响应。
     */
    private Boolean stream;
    /**
     * 是否要求服务端保存响应。
     */
    private Boolean store;

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
     * Responses API 允许生成的最大 token 数。
     */
    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

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
     * 调用方提供的终端用户标识。
     */
    private String user;

    /**
     * <p>将当前请求切换为流式模式。</p>
     *
     * @return 当前对象，便于链式调用
     * @since 1.0.0
     */
    public ResponseRequest withStream() {
        ResponseRequest r = new ResponseRequest();
        r.model = this.model; r.input = this.input; r.instructions = this.instructions;
        r.stream = true; r.store = this.store; r.previousResponseId = this.previousResponseId;
        r.conversation = this.conversation; r.maxOutputTokens = this.maxOutputTokens;
        r.temperature = this.temperature; r.topP = this.topP; r.user = this.user;
        return r;
    }
}
