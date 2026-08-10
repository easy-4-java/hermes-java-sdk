package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * <p>Hermes 服务能力响应模型。</p>
 *
 * <p>描述平台、模型、鉴权和可用功能等服务端能力信息。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityInfo {
    /**
     * OpenAI 兼容协议中的对象类型。
     */
    private String object;
    /**
     * Hermes 平台名称。
     */
    private String platform;
    /**
     * 请求或响应关联的模型名称。
     */
    private String model;
    /**
     * 服务端鉴权能力描述。
     */
    private Map<String, Object> auth;
    /**
     * 服务端功能开关与能力映射。
     */
    private Map<String, Object> features;
}
