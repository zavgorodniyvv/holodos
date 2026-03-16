package com.holodos.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        String correlationId,
        int status,
        String error,
        String message,
        List<String> details,
        OffsetDateTime timestamp
) {
}
