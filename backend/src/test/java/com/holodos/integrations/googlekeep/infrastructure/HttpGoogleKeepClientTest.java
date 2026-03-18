package com.holodos.integrations.googlekeep.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holodos.integrations.googlekeep.application.GoogleKeepClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

class HttpGoogleKeepClientTest {
    RestTemplate restTemplate;
    MockRestServiceServer server;
    GoogleKeepProperties properties;
    HttpGoogleKeepClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        server = MockRestServiceServer.createServer(restTemplate);
        properties = new GoogleKeepProperties(true, "http://example.com", "token", Duration.ofSeconds(5));
        client = new HttpGoogleKeepClient(restTemplate, properties);
    }

    @Test
    void pushChecklistCallsRemoteApi() {
        server.expect(MockRestRequestMatchers.requestTo("http://example.com/notes/note-1/checklist:sync"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
            .andRespond(MockRestResponseCreators.withSuccess("{\"success\":true,\"etag\":\"etag-123\",\"details\":\"ok\"}", MediaType.APPLICATION_JSON));

        GoogleKeepClient.KeepSyncResult result = client.pushChecklist("note-1",
            List.of(new GoogleKeepClient.KeepChecklistItem("Milk", false, "shopping-1")), "etag-old");

        assertTrue(result.success());
        assertEquals("etag-123", result.newEtag());
        server.verify();
    }

    @Test
    void fetchChecklistReturnsItems() {
        server.expect(MockRestRequestMatchers.requestTo("http://example.com/notes/note-1/checklist"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess("{\"etag\":\"remote\",\"items\":[{\"text\":\"Milk\",\"checked\":false,\"externalId\":\"shopping-1\"}]}", MediaType.APPLICATION_JSON));

        GoogleKeepClient.KeepRemoteState state = client.fetchChecklist("note-1");

        assertEquals("remote", state.etag());
        assertEquals(1, state.items().size());
        assertEquals("Milk", state.items().get(0).text());
        server.verify();
    }
}
