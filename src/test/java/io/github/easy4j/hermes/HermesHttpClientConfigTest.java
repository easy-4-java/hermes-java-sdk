package io.github.easy4j.hermes;

import org.junit.jupiter.api.Test;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.sse.SseEvent;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HermesHttpClientConfigTest {

    @Test
    void shouldExposeUnifiedHttpProperties() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        assertEquals(HttpResponseMode.BLOCKING, config.getMode());
        config.setBaseUrl("http://hermes");
        assertEquals("http://hermes", config.getBaseUrl());

        config.setStreamCorePoolSize(7);
        config.setStreamMaxPoolSize(9);
        config.setStreamQueueCapacity(11);
        config.setStreamKeepAliveMillis(13L);
        config.setStreamEventQueueCapacity(15);

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

    @Test
    void shouldExposeStreamingObjectsUnderSsePackage() {
        SseEvent event = new SseEvent();
        event.setData("{\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");
        StreamingChatResponse response = new StreamingChatResponse();
        response.accept(event);
        response.finish();
        assertEquals("hello", response.join());
    }
}
