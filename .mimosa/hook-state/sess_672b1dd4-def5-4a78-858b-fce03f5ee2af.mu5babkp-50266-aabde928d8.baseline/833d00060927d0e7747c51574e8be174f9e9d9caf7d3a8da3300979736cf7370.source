package io.github.easy4j.hermes.api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * <p>Hermes 服务健康状态模型。</p>
 *
 * <p>承载服务状态、版本、网关状态、活动智能体数量和进程信息。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus {
    /**
     * 服务端状态。
     */
    private String status;
    /**
     * Hermes 平台名称。
     */
    private String platform;
    /**
     * 探测到的 Hermes 版本。
     */
    private String version;

    /**
     * Gateway 当前运行状态。
     */
    @JsonProperty("gateway_state")
    private String gatewayState;

    /**
     * 各平台的健康详情。
     */
    private Map<String, Object> platforms;

    /**
     * 当前活动智能体数量。
     */
    @JsonProperty("active_agents")
    private Integer activeAgents;

    /**
     * 服务进程退出原因。
     */
    @JsonProperty("exit_reason")
    private String exitReason;

    /**
     * 服务端返回的最后更新时间。
     */
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * 服务进程标识。
     */
    private Integer pid;
}
