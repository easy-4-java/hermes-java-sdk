package io.github.easy4j.hermes.security;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EndpointPolicy 边界分支的确定性覆盖测试。
 *
 * <p>全部用例不依赖真实 DNS：要么在解析前即被拦截（畸形 URL、空 host、端口失配），
 * 要么通过包级 HostResolver 注入固定解析结果（仅 strictPublic 提供注入点）。</p>
 */
class EndpointPolicyEdgeCoverageTest {

    private static InetAddress addr(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldRejectMalformedUrlBeforeSchemeCheck() {
        EndpointPolicy policy = EndpointPolicy.strictPublic();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://[::1"));
        assertTrue(ex.getMessage().contains("Malformed URL"));
    }

    @Test
    void shouldRejectUrlWithoutHost() {
        EndpointPolicy policy = EndpointPolicy.strictPublic();
        assertThrows(IllegalArgumentException.class, () -> policy.require("https:///path"));
    }

    @Test
    void shouldTreatPortlessHttpsAsPort443AndRejectOriginMismatch() {
        EndpointPolicy policy = EndpointPolicy.trustedLocal("127.0.0.1", 8642);
        assertThrows(IllegalArgumentException.class, () -> policy.require("https://127.0.0.1"));
    }

    @Test
    void shouldRejectInvalidTrustedPort() {
        assertThrows(IllegalArgumentException.class, () -> EndpointPolicy.trustedLocal("127.0.0.1", 0));
        assertThrows(IllegalArgumentException.class,
                () -> EndpointPolicy.trustedPrivate("10.0.0.5", 70000));
    }

    @Test
    void shouldRejectLoopbackTrustedPrivateHostAtFactoryTime() {
        // 127.0.0.1 是字面量，SYSTEM_RESOLVER 不经过网络；工厂阶段即拒绝环回地址。
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EndpointPolicy.trustedPrivate("127.0.0.1", 8643));
        assertTrue(ex.getMessage().contains("private addresses"));
    }

    @Test
    void shouldAcceptSiteLocalInsideTrustedPrivate() {
        EndpointPolicy policy = EndpointPolicy.trustedPrivate("192.168.1.10", 8644);
        assertDoesNotThrow(() -> policy.require("http://192.168.1.10:8644"));
    }

    @Test
    void strictPublicShouldRejectEmptyResolution() {
        EndpointPolicy policy = EndpointPolicy.strictPublic(host -> new InetAddress[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://multi-nic.gateway.example.com"));
        assertTrue(ex.getMessage().contains("no addresses"));
    }

    @Test
    void strictPublicShouldRejectWhenAnyResolvedAddressIsPrivate() {
        EndpointPolicy policy = EndpointPolicy.strictPublic(
                host -> new InetAddress[] {addr("93.184.216.34"), addr("10.0.0.7")});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://multi-nic.gateway.example.com"));
        assertTrue(ex.getMessage().contains("non-public address"));
        assertTrue(ex.getMessage().contains("10.0.0.7"));
    }

    @Test
    void strictPublicShouldRejectUnresolvableHost() {
        EndpointPolicy policy = EndpointPolicy.strictPublic(host -> {
            throw new UnknownHostException("injected");
        });
        assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://injected-failure.gateway.example.com"));
    }

    @Test
    void strictPublicShouldRejectLoopbackResolution() {
        EndpointPolicy policy = EndpointPolicy.strictPublic(
                host -> new InetAddress[] {addr("127.0.0.1")});
        assertThrows(IllegalArgumentException.class,
                () -> policy.require("https://rebind.gateway.example.com"));
    }

    @Test
    void strictPublicShouldAcceptPublicIpv6AndStripBrackets() {
        EndpointPolicy policy = EndpointPolicy.strictPublic(
                host -> new InetAddress[] {addr("2001:4860:4860::8888")});
        assertDoesNotThrow(() -> policy.require("https://[2001:4860:4860::8888]/v1"));
    }
}
