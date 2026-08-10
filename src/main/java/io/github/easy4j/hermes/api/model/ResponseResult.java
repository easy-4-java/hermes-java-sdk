package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * <p>Hermes Responses API 响应模型。</p>
 *
 * <p>承载响应状态、模型、结构化输出及 token 使用量。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult {
    /**
     * 对象唯一标识。
     */
    private String id;
    /**
     * OpenAI 兼容协议中的对象类型。
     */
    private String object;
    /**
     * 服务端状态。
     */
    private String status;
    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 服务端生成的结构化输出。
     */
    private List<Map<String, Object>> output;
    /**
     * 本次调用的 token 使用量。
     */
    private RunStatus.Usage usage;
}
