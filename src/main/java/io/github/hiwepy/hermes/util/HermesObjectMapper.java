package io.github.hiwepy.hermes.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 共享 ObjectMapper，SDK 全局唯一实例，避免各处重复创建。
 */
public final class HermesObjectMapper {

    public static final ObjectMapper INSTANCE = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HermesObjectMapper() {}
}
