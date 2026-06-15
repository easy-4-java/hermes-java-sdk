package io.github.hiwepy.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus {
    private String status;
    private String platform;
    private String version;

    @JsonProperty("gateway_state")
    private String gatewayState;

    private Map<String, Object> platforms;

    @JsonProperty("active_agents")
    private Integer activeAgents;

    @JsonProperty("exit_reason")
    private String exitReason;

    @JsonProperty("updated_at")
    private String updatedAt;

    private Integer pid;
}
