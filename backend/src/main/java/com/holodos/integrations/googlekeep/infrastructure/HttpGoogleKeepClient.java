package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.application.GoogleKeepClient;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class HttpGoogleKeepClient implements GoogleKeepClient {
    private static final Logger log = LoggerFactory.getLogger(HttpGoogleKeepClient.class);
    private final RestTemplate restTemplate;
    private final GoogleKeepProperties properties;

    public HttpGoogleKeepClient(RestTemplate restTemplate, GoogleKeepProperties properties) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalArgumentException("holodos.integrations.google-keep.base-url must be set when integration is enabled");
        }
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public KeepSyncResult pushChecklist(String remoteNoteId, List<KeepChecklistItem> items, String lastKnownEtag) {
        String url = properties.baseUrl() + "/notes/" + remoteNoteId + "/checklist:sync";
        HttpHeaders headers = authHeaders();
        if (lastKnownEtag != null && !lastKnownEtag.isBlank()) {
            headers.setIfMatch(List.of(lastKnownEtag));
        }
        PushChecklistRequest payload = new PushChecklistRequest(mapItems(items), lastKnownEtag);
        try {
            ResponseEntity<PushChecklistResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                PushChecklistResponse.class
            );
            PushChecklistResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from Google Keep push endpoint");
            }
            return new KeepSyncResult(body.success(), body.etag(), body.details());
        } catch (RestClientException e) {
            log.warn("Google Keep push failed", e);
            return new KeepSyncResult(false, lastKnownEtag, e.getMessage());
        }
    }

    @Override
    public KeepRemoteState fetchChecklist(String remoteNoteId) {
        String url = properties.baseUrl() + "/notes/" + remoteNoteId + "/checklist";
        HttpHeaders headers = authHeaders();
        try {
            ResponseEntity<FetchChecklistResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FetchChecklistResponse.class
            );
            FetchChecklistResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from Google Keep fetch endpoint");
            }
            List<KeepChecklistItem> items = CollectionUtils.isEmpty(body.items())
                ? List.of()
                : body.items().stream()
                    .map(i -> new KeepChecklistItem(i.text(), i.checked(), i.externalId()))
                    .collect(Collectors.toList());
            return new KeepRemoteState(body.etag(), items);
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to fetch Google Keep checklist", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            headers.setBearerAuth(properties.apiKey());
        }
        return headers;
    }

    private List<ChecklistItemPayload> mapItems(List<KeepChecklistItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .map(i -> new ChecklistItemPayload(i.text(), i.checked(), i.externalId()))
            .toList();
    }

    private record PushChecklistRequest(List<ChecklistItemPayload> items, String lastKnownEtag) {}
    private record ChecklistItemPayload(String text, boolean checked, String externalId) {}
    private record PushChecklistResponse(boolean success, String etag, String details) {}
    private record FetchChecklistResponse(String etag, List<ChecklistItemPayload> items) {}
}
