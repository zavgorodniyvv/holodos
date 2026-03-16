package com.holodos.reports.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {}

    public record ReportFilter(
        OffsetDateTime from,
        OffsetDateTime to,
        Long categoryId,
        Long storagePlaceId,
        Long storeId
    ) {}

    public record NamedQuantity(String key, BigDecimal quantity) {}

    public record ReportsResponse(
        Map<String, Long> inventoryByStoragePlace,
        Map<String, Long> inventoryByCategory,
        Map<String, Long> shoppingByStore,
        Map<String, Long> operationLogByEvent,
        Long expiringSoonCount,
        Long expiredCount,
        Long storedTooLongCount,
        Long purchasesCount,
        Long discardsCount
    ) {}
}
