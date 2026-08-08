package io.github.easy4j.hermes.api.model;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunCreateRequest {
    private String input;

    @JsonProperty("session_id")
    private String sessionId;

    private String instructions;

    @JsonProperty("conversation_history")
    private Object conversationHistory;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    private String conversation;

    private String model;
}
