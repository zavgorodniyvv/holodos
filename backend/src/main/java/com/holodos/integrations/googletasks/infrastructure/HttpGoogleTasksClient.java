package com.holodos.integrations.googletasks.infrastructure;

import com.holodos.integrations.googletasks.application.GoogleTasksClient;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class HttpGoogleTasksClient implements GoogleTasksClient {

    private static final Logger log = LoggerFactory.getLogger(HttpGoogleTasksClient.class);
    private static final String BASE_URL = "https://tasks.googleapis.com/tasks/v1";
    /** Safety net against a misbehaving API returning tokens forever. */
    private static final int MAX_PAGES = 50;

    private final RestTemplate restTemplate;

    public HttpGoogleTasksClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String createTaskList(String title, String accessToken) {
        String url = BASE_URL + "/users/@me/lists";
        try {
            TaskListResponse response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(new TaskListPayload(title), authHeaders(accessToken)),
                    TaskListResponse.class
            ).getBody();
            if (response == null || response.id() == null) {
                throw new IllegalStateException("Empty or invalid response when creating Google Tasks list");
            }
            return response.id();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to create Google Tasks task list: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<RemoteTask> listTasks(String taskListId, String accessToken) {
        // showHidden: tasks completed in the Google Tasks app become "hidden"; we still need to see them.
        // The API pages results (max 100 per page); follow nextPageToken so later pages aren't treated as missing.
        List<RemoteTask> result = new ArrayList<>();
        String pageToken = null;
        int pages = 0;
        do {
            UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE_URL + "/lists/{listId}/tasks")
                    .queryParam("showCompleted", true)
                    .queryParam("showHidden", true)
                    .queryParam("maxResults", 100);
            if (pageToken != null) {
                uri.queryParam("pageToken", pageToken);
            }
            TaskListItemsResponse response;
            try {
                response = restTemplate.exchange(
                        uri.buildAndExpand(taskListId).encode().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(accessToken)),
                        TaskListItemsResponse.class
                ).getBody();
            } catch (RestClientException ex) {
                throw new IllegalStateException("Failed to list Google Tasks tasks: " + ex.getMessage(), ex);
            }
            if (response == null) {
                break;
            }
            if (response.items() != null) {
                response.items().forEach(t -> result.add(new RemoteTask(t.id(), t.title(), t.notes(), "completed".equals(t.status()))));
            }
            pageToken = response.nextPageToken();
            if (pageToken != null && ++pages >= MAX_PAGES) {
                // A partial list would make the sync think tasks are missing and create duplicates
                throw new IllegalStateException("Google Tasks list is too large (more than " + MAX_PAGES + " pages)");
            }
        } while (pageToken != null);
        return result;
    }

    @Override
    public RemoteTask createTask(String taskListId, String title, String externalId, String accessToken) {
        String url = BASE_URL + "/lists/" + taskListId + "/tasks";
        try {
            TaskResponse response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(new TaskPayload(title, externalId), authHeaders(accessToken)),
                    TaskResponse.class
            ).getBody();
            if (response == null) {
                throw new IllegalStateException("Empty response when creating Google Tasks task");
            }
            return new RemoteTask(response.id(), response.title(), response.notes(),
                    "completed".equals(response.status()));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to create Google Tasks task: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void completeTask(String taskListId, String taskId, String accessToken) {
        String url = BASE_URL + "/lists/" + taskListId + "/tasks/" + taskId;
        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    new HttpEntity<>(new StatusPayload("completed"), authHeaders(accessToken)),
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to complete Google Tasks task " + taskId + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteTask(String taskListId, String taskId, String accessToken) {
        String url = BASE_URL + "/lists/" + taskListId + "/tasks/" + taskId;
        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(authHeaders(accessToken)),
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to delete Google Tasks task " + taskId + ": " + ex.getMessage(), ex);
        }
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private record TaskListPayload(String title) {}
    private record TaskListResponse(String id, String title) {}
    private record TaskPayload(String title, String notes) {}
    private record StatusPayload(String status) {}
    private record TaskResponse(String id, String title, String notes, String status) {}
    private record TaskListItemsResponse(List<TaskResponse> items, String nextPageToken) {}
}
