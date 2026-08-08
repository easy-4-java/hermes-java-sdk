package io.github.easy4j.hermes;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(Math.max(1, config.getMaxRequests()));
        dispatcher.setMaxRequestsPerHost(Math.max(1, config.getMaxRequestsPerHost()));
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
