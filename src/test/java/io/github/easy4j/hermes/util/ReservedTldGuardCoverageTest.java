package io.github.easy4j.hermes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RFC 2606 保留 TLD（.invalid/.test/.example/.localhost）确定性拦截的守卫层回归测试。
 *
 * <p>通配 DNS 环境可能把保留 TLD 解析到公网 IP，绕过基于解析结果的防护；
 * EndpointGuard 必须在 DNS 之前按后缀确定性拒绝。</p>
 */
class ReservedTldGuardCoverageTest {

    @Test
    void shouldRejectReservedTldHostsBeforeDns() {
        for (String host : new String[] {
                "hermes.invalid", "hermes.test", "hermes.example",
                "hermes.localhost", "localhost"}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EndpointGuard.requireSafeHost(host, "https://" + host + "/v1"));
            assertTrue(ex.getMessage().contains("reserved test host"),
                    "expected reserved-TLD rejection for " + host + " but got: " + ex.getMessage());
        }
    }

    @Test
    void shouldRejectReservedTldUrls() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.require("https://gateway.hermes.invalid/v1/chat"));
        assertTrue(ex.getMessage().contains("reserved test host"));
    }

    @Test
    void shouldStillAcceptPublicIpLiterals() {
        // 公网 IP 字面量不经过 DNS，也不应触发保留 TLD 分支。
        assertDoesNotThrow(() -> EndpointGuard.requireSafeHost("93.184.216.34", "https://93.184.216.34/v1"));
    }
}
