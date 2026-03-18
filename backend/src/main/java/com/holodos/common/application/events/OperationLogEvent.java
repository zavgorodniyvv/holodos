package com.holodos.common.application.events;

import java.util.Map;

public record OperationLogEvent(String eventType, String entityType, String entityId, Map<String, Object> payload) {
}
