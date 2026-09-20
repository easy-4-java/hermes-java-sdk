package io.github.easy4j.hermes.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EndpointPolicy 层的 RFC 2606 保留 TLD 确定性拦截回归测试。
 *
 * <p>对应 Java: EndpointPolicy#rejectReservedTld —— strictPublic 的
 * require 必须在 DNS 解析之前拒绝保留 TLD，避免通配 DNS 环境绕过。</p>
 */
class ReservedTldPolicyCoverageTest {

    @Test
    void strictPublicShouldRejectEveryReservedTldBeforeDns() {
        EndpointPolicy policy = EndpointPolicy.strictPublic();
        for (String host : new String[] {
                "gateway.invalid", "gateway.test", "gateway.example",
                "gateway.localhost", "localhost"}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> policy.require("https://" + host + "/v1"));
            assertTrue(ex.getMessage().contains("reserved test host"),
                    "expected reserved-TLD rejection for " + host + " but got: " + ex.getMessage());
        }
    }

    @Test
    void trustedLocalShouldAlsoRejectReservedTld() {
        EndpointPolicy policy = EndpointPolicy.trustedLocal("127.0.0.1", 8642);
        assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://sub.localhost/v1"));
    }
}
