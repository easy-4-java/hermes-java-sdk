package io.github.hiwepy.hermes.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {
    private String id;
    private String title;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("end_reason")
    private String endReason;

    private Map<String, Object> metadata;
}
