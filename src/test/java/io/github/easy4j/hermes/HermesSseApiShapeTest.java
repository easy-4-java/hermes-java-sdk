package io.github.easy4j.hermes;

import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.sse.SseSubscription;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 Hermes 统一 SSE 对象拓扑和破坏性命名结果。 */
class HermesSseApiShapeTest {

    @Test
    void shouldExposeOnlyTheUnifiedSseTopology() {
        assertEquals(HermesSseClient.class,
                method(HermesClient.class, "sse").getReturnType());
        assertTrue(hasMethod(HermesSseClient.class, "subscribeChat"));
        assertTrue(hasMethod(HermesSseClient.class, "subscribeRunEvents"));
        assertTrue(hasMethod(HermesSseClient.class, "subscribeRunEventsQueue"));
        assertTrue(hasMethod(HermesSseClient.class, "subscribeSessionEvents"));
        assertTrue(hasMethod(HermesSseClient.class, "activeSubscriptionCount"));
        assertFalse(hasMethod(HermesSseClient.class, "subscribe"));
        assertFalse(hasMethod(HermesSseClient.class, "subscribeQueue"));
        assertFalse(hasMethod(HermesSseClient.class, "subscribeQueueSubscription"));
        assertFalse(hasMethod(HermesSseClient.class, "subscribeSessionStream"));
        assertFalse(hasMethod(HermesSseClient.class, "stop"));
        assertFalse(hasMethod(HermesChatClient.class, "events"));
        assertFalse(hasMethod(io.github.easy4j.hermes.api.sse.StreamingChatResponse.class,
                "getEventQueue"));
        for (Field field : HermesChatClient.class.getDeclaredFields()) {
            assertFalse("eventClient".equals(field.getName()));
        }
    }

    @Test
    void shouldCancelSubscriptionIdempotently() {
        AtomicInteger cancellations = new AtomicInteger();
        SseSubscription subscription = new SseSubscription(cancellations::incrementAndGet);
        assertTrue(subscription.isActive());
        assertTrue(subscription.cancel());
        subscription.close();
        assertFalse(subscription.isActive());
        assertEquals(1, cancellations.get());
    }

    @Test
    void shouldExposeChatAndSseClientsFromRootFacade() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        config.setStartupCheckEnabled(false);
        try (HermesClient client = new HermesClient(config)) {
            assertNotNull(client.chat());
            assertNotNull(client.sse());
        }
    }

    private Method method(Class<?> type, String name) {
        return Arrays.stream(type.getMethods())
                .filter(value -> name.equals(value.getName()))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods()).map(Method::getName)
                .anyMatch(name::equals);
    }
}
