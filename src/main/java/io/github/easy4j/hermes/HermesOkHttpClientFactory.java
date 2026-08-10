package io.github.easy4j.hermes;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hermes 独立运行时使用的高并发 OkHttpClient 工厂。
 *
 * <p>Spring 容器已经提供共享 {@link OkHttpClient} 时应使用注入构造器，本工厂不会参与。</p>
 */
public final class HermesOkHttpClientFactory {

    private HermesOkHttpClientFactory() {
    }

    /**
     * 根据 Hermes HTTP 配置创建客户端。
     *
     * @param config HTTP 配置
     * @return SDK 自主管理的 OkHttpClient
     */
    public static OkHttpClient create(HermesHttpClientConfig config) {
        Objects.requireNonNull(config, "config");
        Dispatcher dispatcher = createDispatcher(config);
        ConnectionPool connectionPool = new ConnectionPool(
                Math.max(1, config.getMaxIdleConnections()),
                Math.max(1L, config.getKeepAliveDurationMillis()),
                TimeUnit.MILLISECONDS);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .connectTimeout(Math.max(1, config.getConnectTimeoutMillis()), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(0, config.getReadTimeoutMillis()), TimeUnit.MILLISECONDS)
                .writeTimeout(Math.max(1, config.getWriteTimeoutMillis()), TimeUnit.MILLISECONDS)
                .callTimeout(Math.max(0, config.getCallTimeoutMillis()), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(config.isRetryOnConnectionFailure());
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
    }

    /** 创建具有固定并发上限的 Dispatcher，供 HTTP 与 SSE 共享语义。 */
    public static Dispatcher createDispatcher(HermesHttpClientConfig config) {
        Objects.requireNonNull(config, "config");
        int maxRequests = Math.max(1, config.getMaxRequests());
        AtomicInteger threadIndex = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maxRequests, maxRequests, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxRequests), runnable -> {
                    Thread thread = new Thread(runnable,
                            "hermes-okhttp-dispatcher-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        executor.allowCoreThreadTimeOut(true);
        Dispatcher dispatcher = new Dispatcher(executor);
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(Math.max(1, config.getMaxRequestsPerHost()));
        return dispatcher;
    }

    /**
     * 释放 SDK 自建客户端资源。
     *
     * @param client SDK 自建客户端
     */
    public static void shutdown(OkHttpClient client) {
        if (Objects.isNull(client)) {
            return;
        }
        client.dispatcher().cancelAll();
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }
}
