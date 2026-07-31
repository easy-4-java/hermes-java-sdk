package io.github.easy4j.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseRequest {
    private String model;
    private Object input;
    private String instructions;
    private Boolean stream;
    private Boolean store;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    private String conversation;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private String user;

    /** Returns a copy with stream=true. */
    public ResponseRequest withStream() {
        ResponseRequest r = new ResponseRequest();
        r.model = this.model; r.input = this.input; r.instructions = this.instructions;
        r.stream = true; r.store = this.store; r.previousResponseId = this.previousResponseId;
        r.conversation = this.conversation; r.maxOutputTokens = this.maxOutputTokens;
        r.temperature = this.temperature; r.topP = this.topP; r.user = this.user;
        return r;
    }
}
