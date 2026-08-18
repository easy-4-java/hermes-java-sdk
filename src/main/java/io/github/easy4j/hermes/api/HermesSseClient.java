package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.ChatCompletionRequest;
import io.github.easy4j.hermes.api.model.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Hermes Server SSE 客户端，消费 run 事件流和 session 流式聊天。
 */
public class HermesSseClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HermesSseClient.class);

    private final HermesClientConfig config;
    private final ObjectMapper mapper;
    private volatile boolean running;
    private HttpURLConnection connection;
    private ExecutorService executor;

    public HermesSseClient(HermesClientConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 异步订阅 run 事件流，事件通过 consumer 回调。
     *
     * @param runId    run ID
     * @param consumer 事件消费者
     */
    public void subscribe(String runId, Consumer<SseEvent> consumer) {
        this.running = true;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hermes-sse");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(() -> doSubscribeRun(runId, consumer));
    }

    /**
     * 阻塞式订阅 run 事件流，返回一个 BlockingQueue，事件入队供外部消费。
     *
     * @param runId run ID
     * @return 事件队列
     */
    public BlockingQueue<SseEvent> subscribeQueue(String runId) {
        BlockingQueue<SseEvent> queue = new LinkedBlockingQueue<>();
        subscribe(runId, queue::offer);
        return queue;
    }

    /**
     * 异步订阅 session 流式聊天，通过 POST 发送 input 并读取 SSE 响应。
     *
     * @param sessionId session ID
     * @param input     用户输入
     * @param consumer  事件消费者
     */
        /**
     * Async subscribe to Chat Completion SSE stream, invoking consumer per event,
     * onComplete at end, onError on failure.
     */
    public void subscribeChat(ChatCompletionRequest request,
                              java.util.function.Consumer<SseEvent> consumer,
                              Runnable onComplete,
                              java.util.function.Consumer<Throwable> onError) {
        this.running = true;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hermes-sse-chat");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(() -> {
            try {
                String url = config.getServerUrl() + "/v1/chat/completions";
                connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(config.getConnectTimeoutMillis());
                connection.setReadTimeout(0);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");
                String apiKey = config.resolveApiKey();
                if (!apiKey.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                String body = mapper.writeValueAsString(request);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int status = connection.getResponseCode();
                if (status != 200) {
                    onError.accept(new RuntimeException("SSE chat failed: " + status));
                    return;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if ("[DONE]".equals(json)) { onComplete.run(); return; }
                            if (!json.isEmpty()) {
                                try {
                                    SseEvent event = mapper.readValue(json, SseEvent.class);
                                    consumer.accept(event);
                                } catch (Exception e) { log.debug("SSE parse: {}", json, e); }
                            }
                        }
                    }
                }
                onComplete.run();
            } catch (Exception e) { log.warn("SSE chat error", e); onError.accept(e); }
            finally { if (connection != null) connection.disconnect(); }
        });
    }

public void subscribeSessionStream(String sessionId, String input, Consumer<SseEvent> consumer) {
        this.running = true;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hermes-sse");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(() -> doSubscribeSessionStream(sessionId, input, consumer));
    }

    private void doSubscribeRun(String runId, Consumer<SseEvent> consumer) {
        while (running) {
            try {
                String url = config.getServerUrl() + "/v1/runs/" + runId + "/events";
                connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(config.getConnectTimeoutMillis());
                connection.setReadTimeout(0); // SSE 无读超时
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");

                String apiKey = config.resolveApiKey();
                if (!apiKey.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                }

                int status = connection.getResponseCode();
                if (status != 200) {
                    log.warn("SSE connection failed with status: {}, retrying in 5s", status);
                    Thread.sleep(5000);
                    continue;
                }

                log.info("SSE connected to {}", url);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if (!json.isEmpty()) {
                                try {
                                    SseEvent event = mapper.readValue(json, SseEvent.class);
                                    consumer.accept(event);
                                } catch (Exception e) {
                                    log.debug("Failed to parse SSE event: {}", json, e);
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running) {
                    log.warn("SSE connection lost, retrying in 5s", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private void doSubscribeSessionStream(String sessionId, String input, Consumer<SseEvent> consumer) {
        while (running) {
            try {
                String url = config.getServerUrl() + "/api/sessions/" + sessionId + "/chat/stream";
                connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(config.getConnectTimeoutMillis());
                connection.setReadTimeout(0); // SSE 无读超时
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Content-Type", "application/json");

                String apiKey = config.resolveApiKey();
                if (!apiKey.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                }

                String body = mapper.writeValueAsString(java.util.Map.of("input", input));
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                if (status != 200) {
                    log.warn("SSE session stream failed with status: {}, retrying in 5s", status);
                    Thread.sleep(5000);
                    continue;
                }

                log.info("SSE session stream connected to {}", url);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if (!json.isEmpty()) {
                                try {
                                    SseEvent event = mapper.readValue(json, SseEvent.class);
                                    consumer.accept(event);
                                } catch (Exception e) {
                                    log.debug("Failed to parse SSE event: {}", json, e);
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running) {
                    log.warn("SSE session stream connection lost, retrying in 5s", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /**
     * 停止事件流订阅。
     */
    public void stop() {
        this.running = false;
        if (connection != null) {
            connection.disconnect();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
