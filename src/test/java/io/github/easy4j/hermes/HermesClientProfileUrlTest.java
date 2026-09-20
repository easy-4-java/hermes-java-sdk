package io.github.easy4j.hermes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HermesClient profile URL 拼接辅助的回归测试（包级静态方法直接覆盖）。
 */
class HermesClientProfileUrlTest {

    @Test
    void shouldAppendProfileSegment() {
        assertEquals("http://127.0.0.1:8642/p/team-a",
                HermesClient.profileServerUrl("http://127.0.0.1:8642/", "team-a"));
        assertEquals("http://127.0.0.1:8642/p/team-a",
                HermesClient.profileServerUrl("http://127.0.0.1:8642", " team-a "));
    }

    @Test
    void shouldRejectBlankServerUrl() {
        assertThrows(IllegalStateException.class, () -> HermesClient.profileServerUrl("  ", "team-a"));
        assertThrows(IllegalStateException.class, () -> HermesClient.profileServerUrl(null, "team-a"));
    }

    @Test
    void shouldRejectIllegalProfileIdInsideUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> HermesClient.profileServerUrl("http://127.0.0.1:8642", "../evil"));
    }
}
