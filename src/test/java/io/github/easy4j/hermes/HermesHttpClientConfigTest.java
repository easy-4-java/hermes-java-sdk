package io.github.easy4j.hermes;

import org.junit.jupiter.api.Test;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesHttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HermesHttpClientConfigTest {

    @Test
    void shouldExposeUnifiedStreamPropertiesAndLegacyAliases() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        assertEquals(HttpResponseMode.BLOCKING, config.getMode());
        config.setServerUrl("http://legacy-hermes");
        assertEquals("http://legacy-hermes", config.getBaseUrl());

        config.setSseCorePoolSize(7);
        config.setSseMaxPoolSize(9);
        config.setSseQueueCapacity(11);
        config.setSseKeepAliveMillis(13L);
        config.setSseEventQueueCapacity(15);

        assertEquals(7, config.getStreamCorePoolSize());
        assertEquals(9, config.getStreamMaxPoolSize());
        assertEquals(11, config.getStreamQueueCapacity());
        assertEquals(13L, config.getStreamKeepAliveMillis());
        assertEquals(15, config.getStreamEventQueueCapacity());
    }

    @Test
    void shouldExposeChatClientAsHttpClientScenario() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        try (HermesChatClient client = new HermesChatClient(config)) {
            assertEquals(HermesHttpClient.class, client.getClass().getSuperclass());
        }
    }
}
