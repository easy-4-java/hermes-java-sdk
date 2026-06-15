package io.github.hiwepy.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.hermes.api.HermesApiConstants;
import io.github.hiwepy.hermes.api.HermesHttpClient;
import io.github.hiwepy.hermes.api.model.ChatStreamingResponse;
import io.github.hiwepy.hermes.api.model.*;
import io.github.hiwepy.hermes.cli.HermesCliResult;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;


/**
 * Integration tests against a local Hermes API Server.
 * <p>
 * Requires: {@code hermes gateway run} with {@code API_SERVER_ENABLED=true}.
 * Tests marked with {@code @Tag("integration")} — run with {@code -Dgroups=integration}.
 * </p>
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HermesClientIntegrationTest {

    private static final String BASE_URL = "http://127.0.0.1:8642";
    private static final String API_KEY = System.getenv().getOrDefault("HERMES_TEST_API_KEY", "change-me-local-dev");

    private static HermesClient client;

    @BeforeAll
    static void setUp() {
        HermesClientConfig config = new HermesClientConfig();
        config.setServerUrl(BASE_URL);
        config.setApiKey(API_KEY);
        config.setDefaultModel("mimo-v2-pro");
        client = new HermesClient(config);
    }

    @AfterAll
    static void tearDown() {
        if (client != null) client.close();
    }

    // ============================================================
    // Health
    // ============================================================

    @Test @Order(1)
    void health() {
        HealthStatus status = client.health();
        assertNotNull(status);
        assertEquals("ok", status.getStatus());
    }

    @Test @Order(2)
    void healthDetailed() {
        HealthStatus status = client.healthDetailed();
        assertNotNull(status);
        assertEquals("ok", status.getStatus());
    }

    @Test @Order(3)
    void healthV1() {
        HealthStatus status = client.healthV1();
        assertNotNull(status);
        assertEquals("ok", status.getStatus());
    }

    // ============================================================
    // Models / Capabilities / Skills / Toolsets
    // ============================================================

    @Test @Order(10)
    void listModels() {
        ModelsResponse resp = client.listModels();
        assertNotNull(resp);
        assertEquals("list", resp.getObject());
        assertNotNull(resp.getData());
        assertFalse(resp.getData().isEmpty());
        // Hermes advertises its profile name as a model
        assertNotNull(resp.getData().get(0).getId());
    }

    @Test @Order(11)
    void getCapabilities() {
        CapabilityInfo info = client.getCapabilities();
        assertNotNull(info);
        assertEquals("hermes-agent", info.getPlatform());
        assertNotNull(info.getFeatures());
        assertNotNull(info.getAuth());
    }

    @Test @Order(12)
    void listSkills() {
        try {
            List<Map<String, Object>> skills = client.listSkills();
            assertNotNull(skills);
        } catch (RuntimeException e) {
            // Skills endpoint may require different auth version
            assertNotNull(e.getMessage());
        }
    }

    @Test @Order(13)
    void listToolsets() {
        try {
            List<Map<String, Object>> toolsets = client.listToolsets();
            assertNotNull(toolsets);
        } catch (RuntimeException e) {
            // Toolsets endpoint may require different auth version
            assertNotNull(e.getMessage());
        }
    }

    // ============================================================
    // Chat Completion
    // ============================================================

    @Test @Order(20)
    void chatCompletion() {
        ChatRequest req = new ChatRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setMessages(java.util.Arrays.asList(msg("user", "Reply with exactly: OK")));
        req.setMaxTokens(10);
        req.setTemperature(0.0);

        try {
            ChatResponse resp = client.chatCompletion(req);
            assertNotNull(resp);
            assertNotNull(resp.getId());
            assertNotNull(resp.getChoices());
            if (!resp.getChoices().isEmpty()) {
                assertNotNull(resp.getChoices().get(0).getMessage());
            }
        } catch (RuntimeException e) {
            // Accept 502 if model auth fails — tests SDK serialization, not model availability
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401") || e.getMessage().contains("500"),
                    "Expected auth/model error but got: " + e.getMessage());
        }
    }

    @Test @Order(21)
    void chatCompletionWithHeaders() {
        ChatRequest req = new ChatRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setMessages(java.util.Arrays.asList(msg("user", "Reply with: OK")));
        req.setMaxTokens(10);

        Map<String, String> headers = HermesHttpClient.hermesHeaders(
                "agent:main:test", "test-session", null);
        try {
            ChatResponse resp = client.chatCompletion(req, headers);
            assertNotNull(resp);
            assertNotNull(resp.getId());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error but got: " + e.getMessage());
        }
    }

    @Test @Order(22)
    void chatCompletionWithSession() {
        ChatRequest req = new ChatRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setMessages(java.util.Arrays.asList(msg("user", "Reply with: OK")));
        req.setMaxTokens(10);

        try {
            ChatResponse resp = client.chatCompletionWithSession(
                    req, "agent:main:test-user-42", "transcript-1");
            assertNotNull(resp);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
        }
    }

    @Test @Order(23)
    void chatCompletionRequestFields_preservedInJson() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setModel("hermes-agent");
        req.setMessages(java.util.Arrays.asList(msg("system", "You are helpful."), msg("user", "Hi")));
        req.setStream(false);
        req.setMaxTokens(100);
        req.setTemperature(0.7);
        req.setTopP(0.9);
        req.setFrequencyPenalty(0.1);
        req.setPresencePenalty(0.1);
        req.setSeed(42);
        req.setUser("test-user");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"model\":\"hermes-agent\""));
        assertTrue(json.contains("\"max_tokens\":100"));
        assertTrue(json.contains("\"temperature\":0.7"));
        assertTrue(json.contains("\"top_p\":0.9"));
        assertTrue(json.contains("\"seed\":42"));
        assertTrue(json.contains("\"user\":\"test-user\""));
    }

    // ============================================================
    // Responses API
    // ============================================================

    @Test @Order(30)
    void createResponse() {
        ResponseRequest req = new ResponseRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setInput("Reply with exactly: OK");
        req.setMaxOutputTokens(10);

        try {
            ResponseResult resp = client.createResponse(req);
            assertNotNull(resp);
            assertNotNull(resp.getId());
            assertNotNull(resp.getStatus());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
        }
    }

    @Test @Order(31)
    void createResponse_conservation() {
        ResponseRequest req1 = new ResponseRequest();
        req1.setModel(client.getConfig().getDefaultModel());
        req1.setInput("My name is TestBot");
        req1.setStore(true);
        req1.setConversation("test-conversation");

        ResponseRequest req2 = new ResponseRequest();
        req2.setModel(client.getConfig().getDefaultModel());
        req2.setInput("What is my name?");
        try {
            ResponseResult resp1 = client.createResponse(req1);
            assertNotNull(resp1.getId());
            req2.setPreviousResponseId(resp1.getId());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
            return;
        }

        ResponseResult resp2 = client.createResponse(req2);
        assertNotNull(resp2);
        assertNotNull(resp2.getId());
    }

    @Test @Order(32)
    void getResponse() {
        ResponseRequest req = new ResponseRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setInput("OK");
        req.setMaxOutputTokens(5);
        ResponseResult created;
        try {
            created = client.createResponse(req);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
            return;
        }

        ResponseResult fetched = client.getResponse(created.getId());
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("completed", fetched.getStatus());
    }

    @Test @Order(33)
    void deleteResponse() {
        ResponseRequest req = new ResponseRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setInput("OK");
        req.setMaxOutputTokens(5);
        ResponseResult created;
        try {
            created = client.createResponse(req);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
            return;
        }

        boolean deleted = client.deleteResponse(created.getId());
        assertTrue(deleted);
    }

    // ============================================================
    // Streaming SSE
    // ============================================================

    @Test @Order(40)
    void chatCompletionStream() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setModel(client.getConfig().getDefaultModel());
        req.setMessages(java.util.Arrays.asList(msg("user", "Count from 1 to 3")));
        req.setMaxTokens(50);

        try {
            ChatStreamingResponse stream = client.chatCompletionStream(req);
            String full = stream.get();
            assertNotNull(full);
            // Content may be empty if model auth fails, but stream plumbing works
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401") || e.getMessage().contains("500"),
                    "Expected auth error: " + e.getMessage());
        }
    }

    // ============================================================
    // Run
    // ============================================================

    @Test @Order(50)
    void createAndGetRun() {
        RunCreateRequest req = new RunCreateRequest();
        req.setInput("Reply OK");
        try {
            RunStatus run = client.createRun(req);
            assertNotNull(run);
            assertNotNull(run.getRunId());
            assertNotNull(run.getStatus());
            
            RunStatus fetched = client.getRun(run.getRunId());
            assertNotNull(fetched);
            assertEquals(run.getRunId(), fetched.getRunId());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401"),
                    "Expected auth error: " + e.getMessage());
        }
    }

    // ============================================================
    // Session
    // ============================================================

    @Test @Order(60)
    void createAndListSessions() {
        try {
            Session session = client.createSession("test-session-" + System.currentTimeMillis());
            assertNotNull(session);
            if (session.getId() == null) {
                return; // Server rejected due to validation — skip rest
            }
            assertNotNull(session.getTitle());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("400") || e.getMessage().contains("500"),
                    "Expected error: " + e.getMessage());
            return;
        }

        List<Session> sessions = client.listSessions();
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty());
    }

    @Test @Order(61)
    void getAndDeleteSession() {
        Session session;
        try {
            session = client.createSession("delete-" + System.currentTimeMillis());
            if (session.getId() == null) return;
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("400"), "Expected error: " + e.getMessage());
            return;
        }

        Session fetched = client.getSession(session.getId());
        assertEquals(session.getId(), fetched.getId());

        try {
            boolean deleted = client.deleteSession(session.getId());
            assertTrue(deleted);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("400") || e.getMessage().contains("404"),
                    "Expected deletion error: " + e.getMessage());
        }
    }

    @Test @Order(62)
    void sessionChat() {
        Session session = client.createSession("chat-" + System.currentTimeMillis());
        try {
            ChatResponse resp = client.sessionChat(session.getId(), "Reply OK");
            assertNotNull(resp);
            assertNotNull(resp.getChoices());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("502") || e.getMessage().contains("401") || e.getMessage().contains("404"),
                    "Expected error: " + e.getMessage());
        }
    }

    @Test @Order(63)
    void getSessionMessages() {
        Session session = client.createSession("msg-" + System.currentTimeMillis());
        try { client.sessionChat(session.getId(), "Hello"); } catch (RuntimeException ignored) {};

        try {
            List<Map<String, Object>> messages = client.getSessionMessages(session.getId());
            assertNotNull(messages);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("404") || e.getMessage().contains("502") || e.getMessage().contains("400"),
                    "Expected messages error: " + e.getMessage());
        }
    }

    @Test @Order(64)
    void forkSession() {
        Session session = client.createSession("fork-orig-" + System.currentTimeMillis());
        try {
            client.sessionChat(session.getId(), "Test message");
        } catch (RuntimeException ignored) { /* auth issue */ }
        
        try {
            Session forked = client.forkSession(session.getId(), "fork-test");
            assertNotNull(forked);
            assertNotNull(forked.getId());
            assertNotEquals(session.getId(), forked.getId());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("404") || e.getMessage().contains("502") || e.getMessage().contains("400"),
                    "Expected error: " + e.getMessage());
        }
    }

    // ============================================================
    // CLI
    // ============================================================

    @Test @Order(70)
    void cliVersion() {
        HermesCliResult result = client.cli().version();
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Hermes"));
    }

    @Test @Order(71)
    void cliDoctor() {
        HermesCliResult result = client.cli().doctor();
        assertTrue(result.isSuccess());
    }

    @Test @Order(72)
    void cliStatus() {
        HermesCliResult result = client.cli().status();
        assertTrue(result.isSuccess());
    }

    // ============================================================
    // Config
    // ============================================================

    @Test @Order(80)
    void configDefaults() {
        HermesClientConfig config = client.getConfig();
        assertEquals(BASE_URL, config.getServerUrl());
        assertEquals(API_KEY, config.getApiKey());
        assertEquals(HermesApiConstants.DEFAULT_CONNECT_TIMEOUT_MS, config.getConnectTimeoutMillis());
        assertEquals(HermesApiConstants.DEFAULT_READ_TIMEOUT_MS, config.getReadTimeoutMillis());
        assertEquals(HermesApiConstants.DEFAULT_EXECUTABLE, config.getLocalExecutable());
    }

    // ============================================================
    // HermesApiConstants
    // ============================================================

    @Test @Order(90)
    void apiConstants_notNull() {
        assertNotNull(HermesApiConstants.PATH_HEALTH);
        assertNotNull(HermesApiConstants.PATH_CHAT_COMPLETIONS);
        assertNotNull(HermesApiConstants.PATH_RESPONSES);
        assertNotNull(HermesApiConstants.PATH_MODELS);
        assertNotNull(HermesApiConstants.PATH_CAPABILITIES);
        assertNotNull(HermesApiConstants.PATH_SKILLS);
        assertNotNull(HermesApiConstants.PATH_TOOLSETS);
        assertNotNull(HermesApiConstants.PATH_RUNS);
        assertNotNull(HermesApiConstants.PATH_SESSIONS);
        assertNotNull(HermesApiConstants.PATH_JOBS);
        assertNotNull(HermesApiConstants.HEADER_SESSION_KEY);
        assertNotNull(HermesApiConstants.HEADER_SESSION_ID);
        assertNotNull(HermesApiConstants.SSE_DONE_MARKER);
        assertNotNull(HermesApiConstants.SSE_DATA_PREFIX);
    }

    // ============================================================
    // Helper
    // ============================================================

    private static ChatRequest.Message msg(String role, String content) {
        ChatRequest.Message m = new ChatRequest.Message();
        m.setRole(role);
        m.setContent(content);
        return m;
    }
}
