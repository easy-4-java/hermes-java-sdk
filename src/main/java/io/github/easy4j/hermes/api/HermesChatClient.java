package io.github.easy4j.hermes.api;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Hermes 聊天场景客户端，统一提供完整响应与流式响应。
 *
 * <p>普通 Chat Completion 继承自 {@link HermesHttpClient}；SSE 只是该场景客户端的
 * 内部传输机制，业务调用方无需直接依赖 {@link HermesSseClient}。</p>
 */
public class HermesChatClient extends HermesHttpClient {

    private final HermesHttpClientConfig config;
    private final HermesSseClient eventClient;

    public HermesChatClient(HermesHttpClientConfig config) {
        super(config);
        this.config = Objects.requireNonNull(config, "config");
        this.eventClient = new HermesSseClient(config, getObjectMapper(), getOkHttpClient());
    }

    public HermesChatClient(HermesHttpClientConfig config, ObjectMapper objectMapper,
                            OkHttpClient httpClient) {
        super(config, objectMapper, httpClient);
        this.config = Objects.requireNonNull(config, "config");
        this.eventClient = new HermesSseClient(config, getObjectMapper(), getOkHttpClient());
    }

    public StreamingChatResponse chatCompletionStream(ChatRequest request) {
        return chatCompletionStream(request, null, null);
    }

    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                       Map<String, String> headers) {
        return chatCompletionStream(request, headers, null);
    }

    /** 在订阅启动前绑定增量回调，避免丢失首批分片。 */
    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                       Map<String, String> headers,
                                                       Consumer<String> deltaConsumer) {
        Objects.requireNonNull(request, "request");
        StreamingChatResponse stream = new StreamingChatResponse(
                config.getStreamEventQueueCapacity()).onDelta(deltaConsumer);
        eventClient.subscribeChat(request.withStream(), headers, stream::accept, stream::finish, stream::fail);
        return stream;
    }

    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionStreamWithSession(request, sessionKey, null, null);
    }

    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId) {
        return chatCompletionStreamWithSession(request, sessionKey, sessionId, null);
    }

    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId,
                                                                  Consumer<String> deltaConsumer) {
        return chatCompletionStream(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null), deltaConsumer);
    }

    /**
     * 原始事件客户端，仅供 run/session 事件等高级场景使用。
     */
    public HermesSseClient events() {
        return eventClient;
    }

    @Override
    public void close() {
        eventClient.close();
        super.close();
    }
}
