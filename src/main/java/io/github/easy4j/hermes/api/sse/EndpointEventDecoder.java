package io.github.easy4j.hermes.api.sse;

/** Decodes one preserved raw SSE frame into an endpoint-specific event view. */
@FunctionalInterface
public interface EndpointEventDecoder<T> {
    T decode(SseFrame frame) throws Exception;
}
