package io.github.hiwepy.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Boolean stream;

    @JsonProperty("stream_options")
    private Map<String, Object> streamOptions;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private Integer seed;
    private Object stop;
    private String user;

    /** Convenience: returns a copy with stream=true. */
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private Object content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentPart {
        private String type;
        private String text;

        @JsonProperty("image_url")
        private Map<String, Object> imageUrl;
    }
}
