package io.github.easy4j.hermes.api.model;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelsResponse {
    private String object;
    private List<ModelData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelData {
        private String id;
        private String object;
        private Long created;

        @JsonProperty("owned_by")
        private String ownedBy;
    }
}
