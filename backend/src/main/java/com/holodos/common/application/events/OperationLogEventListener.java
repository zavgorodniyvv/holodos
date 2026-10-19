package com.holodos.common.application.events;

import com.holodos.common.application.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {
    private static final Logger log = LoggerFactory.getLogger(OperationLogEventListener.class);
    private final OperationLogService operationLogService;

    public OperationLogEventListener(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOperationLog(OperationLogEvent event) {
        // The business change is already committed: an audit failure must not turn the response into an error.
        try {
            operationLogService.log(event.eventType(), event.entityType(), event.entityId(), event.payload());
        } catch (RuntimeException ex) {
            log.warn("Failed to write operation log {} for {} {}", event.eventType(), event.entityType(), event.entityId(), ex);
        }
    }
}
