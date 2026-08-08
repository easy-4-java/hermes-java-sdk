package io.github.easy4j.hermes.api.model;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityInfo {
    private String object;
    private String platform;
    private String model;
    private Map<String, Object> auth;
    private Map<String, Object> features;
}
