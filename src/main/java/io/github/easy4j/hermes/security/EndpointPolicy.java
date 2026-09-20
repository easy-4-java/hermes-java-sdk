package io.github.easy4j.hermes.security;

import io.github.easy4j.hermes.util.EndpointGuard;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;

/**
 * Explicit outbound endpoint policy for Hermes HTTP and SSE transports.
 *
 * <p>The default policy preserves the historical strict public-endpoint
 * validation. Trusted local access is opt-in and is constrained to one
 * loopback host and one explicit port.</p>
 *
 * @since 1.0.0
 */
public final class EndpointPolicy {

    private enum Mode {
        STRICT_PUBLIC,
        TRUSTED_LOCAL
    }

    private final Mode mode;
    private final String trustedHost;
    private final int trustedPort;

    private EndpointPolicy(Mode mode, String trustedHost, int trustedPort) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.trustedHost = trustedHost;
        this.trustedPort = trustedPort;
    }

    /**
     * Historical strict policy: http/https are allowed only when the target
     * host passes {@link EndpointGuard}.
     */
    public static EndpointPolicy strictPublic() {
        return new EndpointPolicy(Mode.STRICT_PUBLIC, null, -1);
    }

    /**
     * Allows exactly one loopback host and port.
     *
     * @param host loopback host or literal
     * @param port explicit TCP port
     * @return endpoint policy
     */
    public static EndpointPolicy trustedLocal(String host, int port) {
        String normalized = normalizeHost(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Trusted local port must be between 1 and 65535");
        }
        InetAddress[] addresses = resolveAll(normalized);
        if (addresses.length == 0) {
            throw new IllegalArgumentException("Trusted local host did not resolve: " + host);
        }
        for (InetAddress address : addresses) {
            if (!address.isLoopbackAddress()) {
                throw new IllegalArgumentException(
                        "Trusted local host must resolve only to loopback addresses: " + host);
            }
        }
        return new EndpointPolicy(Mode.TRUSTED_LOCAL, normalized, port);
    }

    /**
     * Validates one endpoint URL against this policy.
     *
     * @param url endpoint URL
     * @return the original URL
     */
    public String require(String url) {
        if (mode == Mode.STRICT_PUBLIC) {
            return EndpointGuard.require(url);
        }

        URI uri = parse(url);
        requireHttpScheme(uri, url);
        String host = normalizeHost(uri.getHost());
        int port = effectivePort(uri);

        if (!trustedHost.equals(host) || trustedPort != port) {
            throw new IllegalArgumentException(
                    "Endpoint is outside the trusted local origin: " + url);
        }

        InetAddress[] addresses = resolveAll(host);
        for (InetAddress address : addresses) {
            if (!address.isLoopbackAddress()) {
                throw new IllegalArgumentException(
                        "Trusted local endpoint resolved outside loopback: " + url);
            }
        }
        return url;
    }

    private static URI parse(String url) {
        Objects.requireNonNull(url, "url");
        try {
            return new URI(url);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Malformed URL: " + url, error);
        }
    }

    private static void requireHttpScheme(URI uri, String url) {
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Endpoint URL must use http or https: " + url);
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new IllegalArgumentException("Endpoint URL must have a host: " + url);
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String normalizeHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint host must not be blank");
        }
        String value = host.trim();
        if (value.length() >= 2 && value.charAt(0) == '['
                && value.charAt(value.length() - 1) == ']') {
            value = value.substring(1, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static InetAddress[] resolveAll(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException error) {
            throw new IllegalArgumentException("Endpoint host could not be resolved: " + host, error);
        }
    }
}
