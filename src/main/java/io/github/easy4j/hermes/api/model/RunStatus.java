package io.github.easy4j.hermes.api.model;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunStatus {
    private String object;

    @JsonProperty("run_id")
    private String runId;

    private String status;

    @JsonProperty("session_id")
    private String sessionId;

    private String model;
    private String output;
    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("input_tokens")
        private Long inputTokens;

        @JsonProperty("output_tokens")
        private Long outputTokens;

        @JsonProperty("total_tokens")
        private Long totalTokens;
    }
}
