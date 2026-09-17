package io.github.easy4j.hermes.util;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * <p>Hermes JSON ObjectMapper 工厂。</p>
 *
 * <p>创建忽略未知字段、支持 Java 时间类型且不输出时间戳的共享映射器。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class HermesObjectMapper {

    /**
     * 供 SDK 内部共享的预配置 ObjectMapper。
     */
    public static final ObjectMapper INSTANCE = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private HermesObjectMapper() {}
}
