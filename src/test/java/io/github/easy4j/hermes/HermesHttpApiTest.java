package io.github.easy4j.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.ResponseRequest;
import io.github.easy4j.hermes.api.model.RunCreateRequest;
import io.github.easy4j.hermes.exception.HermesHttpException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.github.easy4j.hermes.Java8Collections.list;
import static io.github.easy4j.hermes.Java8Collections.map;

class HermesHttpApiTest {

    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicBoolean failTransport = new AtomicBoolean();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private OkHttpClient okHttpClient;
    private HermesHttpClientConfig httpConfig;
    private HermesHttpClient http;

    @BeforeEach
    void setUp() {
        okHttpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            requests.add(chain.request());
            if (failTransport.get()) {
                throw new IOException("transport down");
            }
            int responseCode = status.get();
            String body = isListRequest(chain.request()) ? "[]" : "{}";
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message(responseCode < 300 ? "OK" : "Failure")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .build();
        }).build();
        httpConfig = new HermesHttpClientConfig();
        httpConfig.setBaseUrl("http://localhost:8642");
        httpConfig.setApiKey("secret");
        http = new HermesHttpClient(httpConfig, new ObjectMapper(), okHttpClient);
    }

    @AfterEach
    void tearDown() {
        http.close();
        HermesOkHttpClientFactory.shutdown(okHttpClient);
    }

    @Test
    void shouldCoverAllRestApiOperationsAndRequestShapes() {
        ChatRequest chat = new ChatRequest();
        chat.setModel("model");
        chat.setMessages(list(new ChatRequest.Message("user", "hello")));
        chat.setStream(false);
        ResponseRequest response = new ResponseRequest();
        RunCreateRequest run = new RunCreateRequest();

        assertNotNull(http.health());
        assertNotNull(http.healthDetailed());
        assertNotNull(http.healthV1());
        assertNotNull(http.chatCompletion(chat));
        assertNotNull(http.chatCompletion(chat, map("X-Trace", "trace")));
        AtomicBoolean cancellationRegistered = new AtomicBoolean();
        assertThrows(HermesHttpException.class, () -> http.chatCompletion(chat, null, callback -> {
            cancellationRegistered.set(true);
            callback.run();
            return () -> { };
        }));
        assertTrue(cancellationRegistered.get());
        assertNotNull(http.createResponse(response));
        assertNotNull(http.createResponse(response, map("X-Trace", "trace")));
        assertNotNull(http.getResponse("response-id"));
        assertTrue(http.deleteResponse("response-id"));
        assertNotNull(http.listModels());
        assertNotNull(http.getModel("model/name"));
        assertNotNull(http.getCapabilities());
        assertTrue(http.listSkills().isEmpty());
        assertTrue(http.listToolsets().isEmpty());
        assertNotNull(http.createRun(run));
        assertNotNull(http.getRun("run-id"));
        http.stopRun("run-id");
        assertTrue(http.approveRun("run-id", map("approved", true)).isEmpty());
        assertNotNull(http.createSession("title"));
        assertNotNull(http.createSession(null));
        assertTrue(http.listSessions().isEmpty());
        assertTrue(http.listSessions(10, 2, "api", true).isEmpty());
        assertTrue(http.listSessions(null, null, null, null).isEmpty());
        assertNotNull(http.getSession("session-id"));
        assertTrue(http.getSessionMessages("session-id").isEmpty());
        assertNotNull(http.forkSession("session-id", "fork"));
        assertNotNull(http.forkSession("session-id", null));
        assertTrue(http.deleteSession("session-id"));
        assertNotNull(http.updateSession("session-id", map("title", "new")));
        assertNotNull(http.sessionChat("session-id", "hello"));
        assertTrue(http.listJobs().isEmpty());
        assertTrue(http.createJob(map("name", "job")).isEmpty());
        assertTrue(http.getJob("job-id").isEmpty());
        assertTrue(http.updateJob("job-id", map("enabled", true)).isEmpty());
        assertTrue(http.deleteJob("job-id"));
        assertTrue(http.pauseJob("job-id").isEmpty());
        assertTrue(http.resumeJob("job-id").isEmpty());
        assertTrue(http.runJobNow("job-id").isEmpty());

        assertSame(okHttpClient, http.getOkHttpClient());
        assertNotNull(http.getObjectMapper());
        assertTrue(requests.stream().allMatch(request -> "Bearer secret".equals(request.header("Authorization"))));
        assertTrue(requests.stream().anyMatch(request -> "trace".equals(request.header("X-Trace"))));
        assertTrue(requests.stream().anyMatch(request -> request.url().encodedPath().contains("model%2Fname")));

        Map<String, String> headers = HermesHttpClient.hermesHeaders("key", "id", "channel");
        assertEquals(3, headers.size());
        assertTrue(HermesHttpClient.hermesHeaders(null, null, null).isEmpty());
    }

    @Test
    void shouldReportHttpStatusAndTransportFailures() {
        status.set(503);
        HermesHttpException healthError = assertThrows(HermesHttpException.class, http::health);
        assertEquals(503, healthError.getStatusCode());
        assertEquals("{}", healthError.getResponseBody());
        assertThrows(HermesHttpException.class, http::listSkills);
        assertThrows(HermesHttpException.class, () -> http.approveRun("run", map()));
        assertThrows(HermesHttpException.class, () -> http.stopRun("run"));
        assertThrows(HermesHttpException.class, () -> http.getJob("job"));
        assertThrows(HermesHttpException.class, () -> http.updateJob("job", map()));
        assertFalse(http.deleteJob("job"));
        assertFalse(http.deleteSession("session"));

        failTransport.set(true);
        HermesHttpException transport = assertThrows(HermesHttpException.class, http::health);
        assertEquals(-1, transport.getStatusCode());
        assertNull(transport.getResponseBody());
        assertThrows(HermesHttpException.class, () -> http.stopRun("run"));
        assertThrows(HermesHttpException.class, () -> http.approveRun("run", map()));
        assertThrows(HermesHttpException.class, () -> http.deleteResponse("response"));
        assertThrows(HermesHttpException.class, () -> http.getJob("job"));
        assertThrows(HermesHttpException.class, () -> http.updateJob("job", map()));
        assertThrows(HermesHttpException.class, () -> http.deleteJob("job"));
    }

    @Test
    void shouldDelegateAllFacadeOperationsToSharedHttpClient() {
        HermesCliConfig cliConfig = new HermesCliConfig();
        cliConfig.setEnabled(false);
        ChatRequest chat = new ChatRequest();
        ResponseRequest response = new ResponseRequest();
        RunCreateRequest run = new RunCreateRequest();

        try (HermesClient client = new HermesClient(httpConfig, cliConfig, new ObjectMapper(), okHttpClient)) {
            assertTrue(client.isHttpEnabled());
            assertFalse(client.isCliEnabled());
            assertNotNull(client.health());
            assertNotNull(client.healthDetailed());
            assertNotNull(client.healthV1());
            assertNotNull(client.chatCompletion(chat));
            assertNotNull(client.chatCompletion(chat, map("X-Test", "yes")));
            assertNotNull(client.chatCompletionWithSession(chat, "key"));
            assertNotNull(client.chatCompletionWithSession(chat, "key", "session"));
            assertNotNull(client.createResponse(response));
            assertNotNull(client.createResponse(response, map("X-Test", "yes")));
            assertNotNull(client.getResponse("response"));
            assertTrue(client.deleteResponse("response"));
            assertNotNull(client.listModels());
            assertNotNull(client.getCapabilities());
            assertTrue(client.listSkills().isEmpty());
            assertTrue(client.listToolsets().isEmpty());
            assertNotNull(client.createRun(run));
            assertNotNull(client.getRun("run"));
            client.stopRun("run");
            assertTrue(client.approveRun("run", map()).isEmpty());
            assertNotNull(client.createSession("title"));
            assertTrue(client.listSessions().isEmpty());
            assertTrue(client.listSessions(1, 0, "api", false).isEmpty());
            assertNotNull(client.getSession("session"));
            assertTrue(client.getSessionMessages("session").isEmpty());
            assertNotNull(client.forkSession("session", "fork"));
            assertTrue(client.deleteSession("session"));
            assertNotNull(client.sessionChat("session", "hello"));
            assertTrue(client.listJobs().isEmpty());
            assertTrue(client.createJob(map()).isEmpty());
            assertTrue(client.getJob("job").isEmpty());
            assertTrue(client.updateJob("job", map()).isEmpty());
            assertTrue(client.deleteJob("job"));
            assertTrue(client.pauseJob("job").isEmpty());
            assertTrue(client.resumeJob("job").isEmpty());
            assertTrue(client.runJobNow("job").isEmpty());
            assertNotNull(client.chat());
            assertNull(client.cli());
            assertNotNull(client.getConfig());
            assertSame(okHttpClient, client.getOkHttpClient());
        }
    }

    @Test
    void shouldCoverFacadeConstructorVariantsAndStartupChecks() {
        HermesHttpClientConfig disabledHttp = new HermesHttpClientConfig();
        disabledHttp.setEnabled(false);
        HermesCliConfig disabledCli = new HermesCliConfig();
        disabledCli.setEnabled(false);
        HermesClientConfig disabledConfig = new HermesClientConfig();
        disabledConfig.getHttp().setEnabled(false);
        disabledConfig.getCli().setEnabled(false);

        try (HermesClient value = new HermesClient(disabledConfig, okHttpClient)) {
            assertFalse(value.isHttpEnabled());
            assertFalse(value.isCliEnabled());
            assertNull(value.getOkHttpClient());
        }
        try (HermesClient value = new HermesClient(disabledConfig, new ObjectMapper(), okHttpClient)) {
            assertFalse(value.isHttpEnabled());
        }
        try (HermesClient value = new HermesClient(disabledHttp, disabledCli)) {
            assertFalse(value.isHttpEnabled());
        }
        try (HermesClient value = new HermesClient(disabledHttp, disabledCli, okHttpClient)) {
            assertFalse(value.isHttpEnabled());
        }
        try (HermesClient value = new HermesClient(disabledHttp, new ObjectMapper(), okHttpClient)) {
            assertFalse(value.isHttpEnabled());
        }
        try (HermesClient value = new HermesClient(disabledCli, new ObjectMapper(), okHttpClient)) {
            assertTrue(value.isHttpEnabled());
        }
        try (HermesClient value = new HermesClient(disabledConfig)) {
            assertFalse(value.isHttpEnabled());
        }

        HermesHttpClientConfig checkedHttp = new HermesHttpClientConfig();
        checkedHttp.setStartupCheckEnabled(true);
        checkedHttp.setBaseUrl("http://localhost:8642");
        HermesCliConfig checkedCli = new HermesCliConfig();
        checkedCli.setEnabled(true);
        checkedCli.setStartupCheckEnabled(true);
        checkedCli.setExecutable("/bin/echo");
        try (HermesClient value = new HermesClient(checkedHttp, checkedCli, new ObjectMapper(), okHttpClient)) {
            assertTrue(value.isHttpEnabled());
            assertTrue(value.isCliEnabled());
            assertNotNull(value.cli());
        }

        status.set(200);
        OkHttpClient unhealthyClient = new OkHttpClient.Builder().addInterceptor(chain ->
                new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(200).message("OK")
                        .body(ResponseBody.create("{\"status\":\"bad\"}", MediaType.get("application/json")))
                        .build()).build();
        checkedHttp.setFailFastOnUnavailable(false);
        try (HermesClient value = new HermesClient(checkedHttp, disabledCli, new ObjectMapper(), unhealthyClient)) {
            assertTrue(value.isHttpEnabled());
        }
        checkedHttp.setFailFastOnUnavailable(true);
        assertThrows(IllegalStateException.class,
                () -> new HermesClient(checkedHttp, disabledCli, new ObjectMapper(), unhealthyClient));
        HermesOkHttpClientFactory.shutdown(unhealthyClient);

        HermesCliConfig missingCli = new HermesCliConfig();
        missingCli.setStartupCheckEnabled(true);
        missingCli.setExecutable("/definitely/missing/hermes");
        missingCli.setFailFastOnUnavailable(false);
        try (HermesClient value = new HermesClient(disabledHttp, missingCli, new ObjectMapper(), okHttpClient)) {
            assertTrue(value.isCliEnabled());
        }
        missingCli.setFailFastOnUnavailable(true);
        assertThrows(IllegalStateException.class,
                () -> new HermesClient(disabledHttp, missingCli, new ObjectMapper(), okHttpClient));
    }

    private boolean isListRequest(Request request) {
        String path = request.url().encodedPath();
        return request.method().equals("GET") && (path.equals("/v1/skills") || path.equals("/v1/toolsets")
                || path.equals("/api/sessions") || path.endsWith("/messages")
                || path.equals("/api/jobs"));
    }
}
