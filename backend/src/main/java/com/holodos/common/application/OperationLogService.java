package com.holodos.common.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holodos.common.domain.OperationLog;
import com.holodos.common.infrastructure.CorrelationIdFilter;
import com.holodos.common.infrastructure.OperationLogRepository;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    private final OperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(String eventType, String entityType, String entityId, Map<String, Object> payload) {
        OperationLog log = new OperationLog();
        log.setEventType(eventType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setPayload(toJson(payload));
        log.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        operationLogRepository.save(log);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"payload_serialization_failed\"}";
        }
    }
}
