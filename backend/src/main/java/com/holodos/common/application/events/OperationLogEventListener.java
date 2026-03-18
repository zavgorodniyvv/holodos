package com.holodos.common.application.events;

import com.holodos.common.application.OperationLogService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {
    private final OperationLogService operationLogService;

    public OperationLogEventListener(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOperationLog(OperationLogEvent event) {
        operationLogService.log(event.eventType(), event.entityType(), event.entityId(), event.payload());
    }
}
