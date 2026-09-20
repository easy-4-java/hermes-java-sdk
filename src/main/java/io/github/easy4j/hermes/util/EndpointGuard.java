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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Strict {@code URL host} validator for outbound requests.
 *
 * <p>Rejects anything that is not {@code http} or {@code https}, and rejects
 * hosts that resolve to {@code localhost}, loopback addresses, private
 * networks, link-local addresses or the reserved {@code 0.0.0.0}/test
 * ranges. The intent is to fail closed if a misconfigured or malicious
 * {@code baseUrl} (e.g. {@code http://169.254.169.254/latest/meta-data/},
 * a local service discovery endpoint, or a metadata service) ever sneaks in.</p>
 *
 * <p>Resolution semantics: an IP literal is checked directly; a host name is
 * resolved once and the validator fails if any of the resolved addresses is
 * unsafe. That way {@code localhost.example.com} (which resolves to
 * {@code 127.0.0.1}) is rejected even if a naive string check would miss it.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class EndpointGuard {

    private EndpointGuard() {
    }

    /**
     * Validates the {@code URL} once. Throws if scheme is not {@code http}/
     * {@code https}, host is blank, or any resolved address is unsafe.
     *
     * @param url the URL string to check.
     * @return the original {@code url} for fluent chaining.
     * @throws IllegalArgumentException when the URL is unsafe.
     */
    public static String require(String url) {
        Objects.requireNonNull(url, "url");
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        if (uri.getScheme() == null || !(uri.getScheme().equalsIgnoreCase("http")
                || uri.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Endpoint URL must use http or https: " + url);
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Endpoint URL must have a host: " + url);
        }
        requireSafeHost(host, url);
        return url;
    }

    /**
     * Validates the {@code host} portion of a URL (after parsing). Exposed
     * for the call sites that already hold an {@code HttpUrl} object so they
     * do not have to re-parse.
     *
     * @param host host string (may be an IP literal or a name).
     * @param context caller-supplied diagnostic context (the original URL).
     * @throws IllegalArgumentException when {@code host} resolves to any
     *                                  unsafe address.
     */
    public static void requireSafeHost(String host, String context) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Endpoint host must not be blank: " + context);
        }
        // Strip IPv6 brackets that URI parsing leaves intact on some JDKs.
        String lookup = stripBrackets(host);
        // RFC 2606 reserved TLDs (.invalid, .test, .example, .localhost) are
        // rejected deterministically without DNS — a wildcard resolver may
        // return a public IP for these, bypassing the guard.
        String lower = lookup.toLowerCase();
        if (lower.endsWith(".invalid") || lower.endsWith(".test")
                || lower.endsWith(".example") || lower.endsWith(".localhost")
                || "localhost".equals(lower)) {
            throw new IllegalArgumentException(
                    "Refusing to talk to reserved test host: " + lookup + " (" + context + ")");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(lookup);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Endpoint host could not be resolved: " + host + " (" + context + ")", e);
        }
        if (isUnsafe(addr)) {
            throw new IllegalArgumentException(
                    "Refusing to talk to unsafe endpoint host " + host
                            + " (resolved " + addr.getHostAddress() + "): " + context);
        }
    }

    private static String stripBrackets(String host) {
        if (host.length() >= 2 && host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    /**
     * Decides whether an {@link InetAddress} must not be a request target.
     *
     * <p>Loopback (IPv4/IPv6), site-local / private / link-local IPv4,
     * unique-local IPv6, multicast, the IPv4 {@code 0.0.0.0} any-address
     * sentinel, and the IPv6 {@code ::} any-address sentinel all count as
     * unsafe. Public IPv4 / IPv6 addresses are allowed; IPv6 transition
     * addresses ({@code ::ffff:x.x.x.x}) are mapped and their v4 part is
     * checked recursively.</p>
     */
    static boolean isUnsafe(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] raw = addr.getAddress();
        if (raw.length == 4) {
            // IPv4 site-local (RFC 1918), link-local (169.254/16), and
            // the IETF protocol assignments 192.0.0.0/24 + 192.0.2.0/24.
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            if (b0 == 10) {
                return true;
            }
            if (b0 == 127) {
                return true;
            }
            if (b0 == 172 && (b1 & 0xF0) == 16) {
                return true;
            }
            if (b0 == 192 && b1 == 168) {
                return true;
            }
            if (b0 == 169 && b1 == 254) {
                return true;
            }
            if (b0 == 192 && (b1 == 0)) {
                return true;
            }
            return false;
        }
        if (raw.length == 16) {
            // IPv6 unique-local (fc00::/7).
            if ((raw[0] & 0xFE) == 0xFC) {
                return true;
            }
            // Link-local (fe80::/10) is already covered by isLinkLocalAddress()
            // but the API is on Inet6Address; rely on isLinkLocalAddress.
            if (addr.isLinkLocalAddress()) {
                return true;
            }
            // IPv4-mapped IPv6 (::ffff:a.b.c.d).
            if (addr instanceof java.net.Inet6Address) {
                byte[] v4 = ((java.net.Inet6Address) addr).getAddress();
                boolean mapped = true;
                for (int i = 0; i < 10; i++) {
                    if (v4[i] != 0) {
                        mapped = false;
                        break;
                    }
                }
                if (mapped && (v4[10] & 0xFF) == 0xFF && (v4[11] & 0xFF) == 0xFF) {
                    byte[] inner = new byte[] {v4[12], v4[13], v4[14], v4[15]};
                    try {
                        return isUnsafe(InetAddress.getByAddress(inner));
                    } catch (UnknownHostException e) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }
}
