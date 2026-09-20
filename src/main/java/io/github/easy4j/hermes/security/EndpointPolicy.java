package io.github.easy4j.hermes.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;

/**
 * Explicit outbound endpoint policy for Hermes HTTP and SSE transports.
 *
 * <p>Public endpoints are HTTPS-only and fail closed when any resolved
 * address is not public. Trusted local/private access is opt-in and bound
 * to one explicit host and port.</p>
 *
 * @since 1.0.0
 */
public final class EndpointPolicy {

    enum Mode {
        STRICT_PUBLIC,
        TRUSTED_LOCAL,
        TRUSTED_PRIVATE
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private static final HostResolver SYSTEM_RESOLVER = InetAddress::getAllByName;

    private final Mode mode;
    private final String trustedHost;
    private final int trustedPort;
    private final HostResolver resolver;

    private EndpointPolicy(Mode mode, String trustedHost, int trustedPort, HostResolver resolver) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.trustedHost = trustedHost;
        this.trustedPort = trustedPort;
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Strict public policy. Public targets must use HTTPS and every resolved
     * address must be public.
     *
     * @return endpoint policy
     */
    public static EndpointPolicy strictPublic() {
        return strictPublic(SYSTEM_RESOLVER);
    }

    static EndpointPolicy strictPublic(HostResolver resolver) {
        return new EndpointPolicy(Mode.STRICT_PUBLIC, null, -1, resolver);
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
        requirePort(port, "Trusted local");
        validateAddresses(normalized, SYSTEM_RESOLVER, EndpointPolicy::isLoopbackOnly,
                "Trusted local host must resolve only to loopback addresses: " + host);
        return new EndpointPolicy(Mode.TRUSTED_LOCAL, normalized, port, SYSTEM_RESOLVER);
    }

    /**
     * Allows exactly one private-network host and port.
     *
     * <p>The host must resolve only to RFC1918/site-local or IPv6 ULA
     * addresses. Loopback, link-local, multicast and public addresses are
     * rejected.</p>
     *
     * @param host private host or literal
     * @param port explicit TCP port
     * @return endpoint policy
     */
    public static EndpointPolicy trustedPrivate(String host, int port) {
        String normalized = normalizeHost(host);
        requirePort(port, "Trusted private");
        validateAddresses(normalized, SYSTEM_RESOLVER, EndpointPolicy::isPrivateOnly,
                "Trusted private host must resolve only to private addresses: " + host);
        return new EndpointPolicy(Mode.TRUSTED_PRIVATE, normalized, port, SYSTEM_RESOLVER);
    }

    /**
     * Validates one endpoint URL against this policy.
     *
     * @param url endpoint URL
     * @return the original URL
     */
    public String require(String url) {
        URI uri = parse(url);
        requireHttpScheme(uri, url);
        rejectUserInfo(uri, url);
        String host = normalizeHost(uri.getHost());

        if (mode == Mode.STRICT_PUBLIC) {
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Public endpoint must use https: " + url);
            }
            validateAddresses(host, resolver, address -> !isUnsafePublic(address),
                    "Public endpoint resolved to a non-public address: " + url);
            return url;
        }

        int port = effectivePort(uri);
        if (!trustedHost.equals(host) || trustedPort != port) {
            throw new IllegalArgumentException("Endpoint is outside the trusted origin: " + url);
        }

        if (mode == Mode.TRUSTED_LOCAL) {
            validateAddresses(host, resolver, EndpointPolicy::isLoopbackOnly,
                    "Trusted local endpoint resolved outside loopback: " + url);
        } else {
            validateAddresses(host, resolver, EndpointPolicy::isPrivateOnly,
                    "Trusted private endpoint resolved outside private networks: " + url);
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

    private static void rejectUserInfo(URI uri, String url) {
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("Endpoint URL must not contain user-info: " + url);
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static void requirePort(int port, String label) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(label + " port must be between 1 and 65535");
        }
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

    private static void validateAddresses(String host, HostResolver resolver,
                                          java.util.function.Predicate<InetAddress> predicate,
                                          String failureMessage) {
        InetAddress[] addresses = resolveAll(host, resolver);
        if (addresses.length == 0) {
            throw new IllegalArgumentException("Endpoint host resolved to no addresses: " + host);
        }
        for (InetAddress address : addresses) {
            if (!predicate.test(address)) {
                throw new IllegalArgumentException(failureMessage + " (resolved "
                        + address.getHostAddress() + ")");
            }
        }
    }

    private static InetAddress[] resolveAll(String host, HostResolver resolver) {
        try {
            InetAddress[] addresses = resolver.resolve(host);
            return addresses == null ? new InetAddress[0] : addresses;
        } catch (UnknownHostException error) {
            throw new IllegalArgumentException("Endpoint host could not be resolved: " + host, error);
        }
    }

    private static boolean isLoopbackOnly(InetAddress address) {
        return address != null && address.isLoopbackAddress();
    }

    private static boolean isPrivateOnly(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        if (address.isSiteLocalAddress()) {
            return true;
        }
        byte[] raw = address.getAddress();
        return raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
    }

    private static boolean isUnsafePublic(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isMulticastAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            return b0 == 10
                    || b0 == 127
                    || (b0 == 172 && (b1 & 0xF0) == 16)
                    || (b0 == 192 && b1 == 168)
                    || (b0 == 169 && b1 == 254)
                    || (b0 == 192 && b1 == 0);
        }
        if (raw.length == 16) {
            return (raw[0] & 0xFE) == 0xFC;
        }
        return true;
    }
}
