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
 * <p>Hermes SDK 的高并发 OkHttpClient 工厂。</p>
 *
 * <p>创建具有有界并发、连接复用和明确所有权的独立传输；外部注入客户端不由本工厂创建。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class HermesOkHttpClientFactory {

    private HermesOkHttpClientFactory() {
    }

    /**
     * <p>根据配置创建 OkHttpClient。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @return 由 SDK 管理生命周期的高并发 OkHttpClient
     * @since 1.0.0
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

    /**
     * <p>根据配置创建有界并发 Dispatcher。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @return 具有全局和单主机并发上限的 Dispatcher
     * @since 1.0.0
     */
    public static Dispatcher createDispatcher(HermesHttpClientConfig config) {
        Objects.requireNonNull(config, "config");
        int maxRequests = Math.max(1, config.getMaxRequests());
        AtomicInteger threadIndex = new AtomicInteger();
        // 工作线程数与等待队列均受 maxRequests 约束，防止并发突发造成无界线程或任务积压。
        // Dispatcher 继续执行全局和单主机准入，本执行器提供确定的资源上限。
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
     * <p>释放 SDK 自建 OkHttpClient 的资源。</p>
     *
     * @param client 待释放的 OkHttpClient；可以为 {@code null}
     * @since 1.0.0
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
