package io.github.easy4j.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SseEvent {
    private String event;
    private String data;
}
