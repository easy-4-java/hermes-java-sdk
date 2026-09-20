/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.easy4j.hermes.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.HermesApiConstants;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.HermesSseClient;

/**
 * 单元测试：服务端请求 URL 的 host 白名单校验。
 *
 * <p>{@link EndpointGuard} 与三个 HTTP/SSE 客户端构造点（{@link HermesHttpClientConfig}
 * setter、{@link HermesHttpClient} 构造器）必须共同形成 fail-closed 防线：
 * 仅允许 {@code http}/{@code https}，拒绝 localhost、环回、私有与保留地址。</p>
 *
 * @since 1.0.0
 */
class EndpointGuardTest {

    // ----- scheme 校验 -----

    @Test
    void shouldRejectNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("ftp://example.com"));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("javascript:alert(1)"));
    }

    @Test
    void shouldRejectBlankOrMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require(""));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("not a url"));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://"));
    }

    // ----- host 校验：IP 字面量与域名解析 -----

    @Test
    void shouldRejectLoopbackLiterals() {
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://127.0.0.1"));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://127.0.0.1:8642/api"));
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://[::1]/api"));
    }

    @Test
    void shouldRejectPrivateIpv4() {
        // 10/8
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://10.0.0.5"));
        // 172.16/12
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://172.16.5.5"));
        // 192.168/16
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://192.168.1.1"));
    }

    @Test
    void shouldRejectLinkLocalAndAnyAddress() {
        // 169.254/16 (link-local) including the IMDS / cloud metadata address
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.require("http://169.254.169.254/latest/meta-data/"));
        // 0.0.0.0 (any-address sentinel)
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://0.0.0.0"));
    }

    @Test
    void shouldRejectMulticastAndTestRanges() {
        // 224.0.0.0/4 (multicast) — apt/yum repo mirrors live here
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://224.0.0.1"));
        // 192.0.2.0/24 (TEST-NET-1) — IETF protocol assignment
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://192.0.2.1"));
    }

    @Test
    void shouldRejectHostnameThatResolvesToLoopback() {
        // localhost resolves to 127.0.0.1; hostname check must resolve first.
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://localhost/api"));
    }

    @Test
    void shouldRejectUniqueLocalIpv6() {
        // fc00::/7
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.require("http://[fd12:3456:789a::1]/api"));
    }

    @Test
    void shouldRejectMappedLoopback() {
        // ::ffff:127.0.0.1
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.require("http://[::ffff:127.0.0.1]/api"));
    }

    @Test
    void shouldAcceptPublicIpv4AndHttps() {
        // 8.8.8.8 (public, well-known Google DNS) over https
        assertDoesNotThrow(() -> EndpointGuard.require("https://8.8.8.8/dns-query"));
        // 1.1.1.1 over http
        assertDoesNotThrow(() -> EndpointGuard.require("http://1.1.1.1/dns-query"));
    }

    @Test
    void shouldAcceptPublicHost() {
        assertDoesNotThrow(() -> EndpointGuard.require("https://example.com/api"));
        assertDoesNotThrow(() -> EndpointGuard.require("https://api.openai.com/v1"));
    }

    @Test
    void shouldAllowPublicIpv6Literal() {
        // Cloudflare 公共 DNS IPv6（2606:4700:4700::1111）——IP 字面量不经 DNS。
        assertDoesNotThrow(() -> EndpointGuard.require("http://[2606:4700:4700::1111]/dns-query"));
    }

    @Test
    void shouldRejectWhenDnsCannotResolve() {
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.require("https://hermes-endpoint-does-not-exist.invalid/v1"));
    }

    @Test
    void shouldCoverRequireSafeHostEdgeBranches() {
        // 空 host 直接拒绝；带方括号的 IPv6 环回经 stripBrackets 后命中守卫。
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.requireSafeHost(null, "ctx"));
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.requireSafeHost("", "ctx"));
        assertThrows(IllegalArgumentException.class,
                () -> EndpointGuard.requireSafeHost("[::1]", "ctx"));
        // 带方括号的公网 IPv6 放行（覆盖 stripBrackets 正常剥离路径）。
        assertDoesNotThrow(() -> EndpointGuard.requireSafeHost("[2606:4700:4700::1111]", "ctx"));
    }

    @Test
    void shouldReturnSameUrlOnSuccess() {
        assertEquals("https://api.example.com/v1", EndpointGuard.require("https://api.example.com/v1"));
    }

    // ----- 集成点：所有出站客户端都套同一守卫 -----

    @Test
    void shouldRejectUnsafeBaseUrlInHermesHttpClientConfigSetter() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();

        assertThrows(IllegalArgumentException.class, () -> config.setBaseUrl("http://127.0.0.1/api"));
        assertThrows(IllegalArgumentException.class, () -> config.setBaseUrl("http://10.0.0.5/api"));
        assertThrows(IllegalArgumentException.class, () -> config.setBaseUrl("ftp://example.com/api"));
    }

    @Test
    void shouldAcceptSafeBaseUrlInHermesHttpClientConfigSetter() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        assertDoesNotThrow(() -> config.setBaseUrl("https://example.com/api"));
        assertEquals("https://example.com/api", config.getBaseUrl());
    }

    @Test
    void shouldRejectUnsafeBaseUrlAtHermesHttpClientConstruction() {
        // 即便 baseUrl 走反射绕过了 setter（绕过白名单），构造时也必须拦截
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        forceField(config, "baseUrl", "http://192.168.0.1/api");
        assertThrows(IllegalArgumentException.class, () -> new HermesHttpClient(config));
    }

    @Test
    void shouldRejectUnsafeBaseUrlAtHermesChatClientAndSseClient() {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        forceField(config, "baseUrl", "http://10.0.0.5/api");

        // HermesChatClient 单参构造器存在；HermesSseClient 三参，反射构造让守卫
        // 在 super(...) 里先抛 IAE（如不安全），反之抛 IllegalArgumentException 不安全。
        assertThrows(IllegalArgumentException.class, () -> new HermesChatClient(config));
        // 按形参类型匹配三参构造器（三参：config, objectMapper, httpClient）——
        // 避免依赖 Jackson 2/3 的具体 ObjectMapper 类名（两分支传递依赖不同）。
        java.lang.reflect.Constructor<?> sseCtor = null;
        for (java.lang.reflect.Constructor<?> c : HermesSseClient.class.getConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == 3
                    && params[0] == HermesHttpClientConfig.class
                    && params[2] == okhttp3.OkHttpClient.class) {
                sseCtor = c;
                break;
            }
        }
        assertNotNull(sseCtor, "HermesSseClient (config, objectMapper, httpClient) constructor must exist");
        try {
            sseCtor.newInstance(config, null, null);
            throw new AssertionError("HermesSseClient construction should have rejected the unsafe baseUrl");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (!(cause instanceof IllegalArgumentException)
                    || !cause.getMessage().toLowerCase().contains("unsafe")) {
                throw new AssertionError(
                        "Expected EndpointGuard IllegalArgumentException, got " + cause);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Reflective HermesSseClient construction failed", e);
        }
    }

    @Test
    void shouldCoverBranchAndPortVariants() {
        // 端口不应影响校验结论
        assertThrows(IllegalArgumentException.class, () -> EndpointGuard.require("http://127.0.0.1:8080"));
        assertDoesNotThrow(() -> EndpointGuard.require("http://example.com:8080"));
    }

    private static void forceField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Could not force " + name + " on " + target.getClass(), e);
        }
    }

    @Test
    void sanityCheckHelperLogsWhenReachable() {
        // 调试时排查容易忘记 forceField 的行为——失败时打印
        assertTrue(EndpointGuard.require("https://example.com").contains("example.com"));
    }
}
