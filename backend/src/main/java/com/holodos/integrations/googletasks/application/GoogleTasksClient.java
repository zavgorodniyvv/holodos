package com.holodos.integrations.googletasks.application;

import java.util.List;

public interface GoogleTasksClient {

    String createTaskList(String title, String accessToken);

    List<RemoteTask> listTasks(String taskListId, String accessToken);

    RemoteTask createTask(String taskListId, String title, String externalId, String accessToken);

    void completeTask(String taskListId, String taskId, String accessToken);

    void deleteTask(String taskListId, String taskId, String accessToken);

    record RemoteTask(String id, String title, String notes, boolean completed) {}
}
