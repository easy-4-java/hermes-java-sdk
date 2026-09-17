package io.github.easy4j.hermes.api;
import tools.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.SseSubscription;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>Hermes 聊天场景客户端。</p>
 *
 * <p>在 REST Chat Completion 能力之上组合 SSE 传输，为完整响应和增量响应提供一致入口。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class HermesChatClient extends HermesHttpClient {

    /**
     * SSE 事件流客户端。
     */
    private final HermesSseClient sseClient;
    /**
     * 当前聊天客户端是否负责关闭组合的 SSE 客户端。
     */
    private final boolean ownsSseClient;

    /**
     * <p>创建 HermesChatClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesChatClient(HermesHttpClientConfig config) {
        super(config);
        Objects.requireNonNull(config, "config");
        this.sseClient = new HermesSseClient(config, getObjectMapper(), getOkHttpClient());
        this.ownsSseClient = true;
    }

    /**
     * <p>创建 HermesChatClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesChatClient(HermesHttpClientConfig config, ObjectMapper objectMapper,
                            OkHttpClient httpClient) {
        super(config, objectMapper, httpClient);
        Objects.requireNonNull(config, "config");
        this.sseClient = new HermesSseClient(config, getObjectMapper(), getOkHttpClient());
        this.ownsSseClient = true;
    }

    /**
     * <p>创建 HermesChatClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @param sseClient SSE 客户端
     * @since 1.0.0
     */
    public HermesChatClient(HermesHttpClientConfig config, ObjectMapper objectMapper,
                            OkHttpClient httpClient, HermesSseClient sseClient) {
        super(config, objectMapper, httpClient);
        Objects.requireNonNull(config, "config");
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.ownsSseClient = false;
    }

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request) {
        return chatCompletionStream(request, null, null);
    }

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                       Map<String, String> headers) {
        return chatCompletionStream(request, headers, null);
    }

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @param deltaConsumer 文本增量消费者
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                       Map<String, String> headers,
                                                       Consumer<String> deltaConsumer) {
        Objects.requireNonNull(request, "request");
        StreamingChatResponse stream = new StreamingChatResponse().onDelta(deltaConsumer);
        SseSubscription subscription = sseClient.subscribeChat(
                request.withStream(), headers, stream::accept, stream::finish, stream::fail);
        stream.onCancel(subscription::close);
        stream.whenComplete((value, error) -> subscription.close());
        return stream;
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionStreamWithSession(request, sessionKey, null, null);
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId) {
        return chatCompletionStreamWithSession(request, sessionKey, sessionId, null);
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @param deltaConsumer 文本增量消费者
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId,
                                                                  Consumer<String> deltaConsumer) {
        return chatCompletionStream(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null), deltaConsumer);
    }

    /**
     * <p>关闭当前对象并释放其拥有的资源。</p>
     *
     * <p>重复关闭不会重复释放资源；调用方注入且不归当前对象所有的共享资源不会被关闭。</p>
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        if (ownsSseClient) {
            sseClient.close();
        }
        super.close();
    }
}
