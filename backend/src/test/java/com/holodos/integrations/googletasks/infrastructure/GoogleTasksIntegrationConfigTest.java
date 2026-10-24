package com.holodos.integrations.googletasks.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

class GoogleTasksIntegrationConfigTest {
    HttpServer server;
    final AtomicReference<String> receivedMethod = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /** Completing a task is a PATCH; HttpURLConnection-based factories reject that method outright. */
    @Test
    void restTemplateSupportsPatch() {
        RestTemplate restTemplate = new GoogleTasksIntegrationConfig().googleTasksRestTemplate();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/tasks/1";

        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>("{\"status\":\"completed\"}"), String.class);

        assertEquals("PATCH", receivedMethod.get());
    }
}
