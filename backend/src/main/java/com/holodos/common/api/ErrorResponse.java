package com.holodos.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    String code,
    String message,
    List<String> details,
    String correlationId,
    OffsetDateTime timestamp
) {
}
