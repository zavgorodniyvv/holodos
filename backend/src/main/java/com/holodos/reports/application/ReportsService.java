package com.holodos.reports.application;

import com.holodos.common.domain.OperationLog;
import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.domain.PurchaseEvent;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.reports.api.ReportDtos.ReportsResponse;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportsService {
    private final StockEntryRepository stockEntryRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final OperationLogRepository operationLogRepository;

    public ReportsService(StockEntryRepository stockEntryRepository, PurchaseEventRepository purchaseEventRepository,
                          ShoppingListItemRepository shoppingListItemRepository, OperationLogRepository operationLogRepository) {
        this.stockEntryRepository = stockEntryRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.operationLogRepository = operationLogRepository;
    }

    public ReportsResponse build(ReportFilter filter) {
        OffsetDateTime now = OffsetDateTime.now();
        List<StockEntry> stock = stockEntryRepository.findAll().stream()
            .filter(s -> inRange(s.getAddedAt(), filter.from(), filter.to()))
            .filter(s -> filter.storagePlaceId() == null || s.getStoragePlace().getId().equals(filter.storagePlaceId()))
            .filter(s -> filter.categoryId() == null || s.getProduct().getCategory().getId().equals(filter.categoryId()))
            .toList();

        Map<String, Long> inventoryByStorage = countBy(stock, s -> s.getStoragePlace().getName());
        Map<String, Long> inventoryByCategory = countBy(stock, s -> s.getProduct().getCategory().getName());

        Map<String, Long> shoppingByStore = shoppingListItemRepository.findAll().stream()
            .filter(i -> i.getStatus() == ShoppingItemStatus.ACTIVE)
            .filter(i -> filter.storeId() == null || (i.getStore() != null && i.getStore().getId().equals(filter.storeId())))
            .collect(Collectors.groupingBy(i -> i.getStore() == null ? "UNASSIGNED" : i.getStore().getName(), Collectors.counting()));

        Map<String, Long> operationByEvent = countBy(operationLogRepository.findAll().stream()
            .filter(o -> inRange(o.getCreatedAt(), filter.from(), filter.to()))
            .toList(), OperationLog::getEventType);

        long expiringSoon = stock.stream().filter(s -> s.getStatus() == StockStatus.AVAILABLE && s.getExpiresAt() != null && s.getExpiresAt().isBefore(now.plusDays(3)) && s.getExpiresAt().isAfter(now)).count();
        long expired = stock.stream().filter(s -> s.getStatus() == StockStatus.EXPIRED || (s.getExpiresAt() != null && s.getExpiresAt().isBefore(now))).count();
        long storedTooLong = stock.stream().filter(s -> s.getAddedAt() != null && s.getAddedAt().isBefore(now.minusDays(365))).count();

        List<PurchaseEvent> purchases = purchaseEventRepository.findAll().stream()
            .filter(p -> inRange(p.getCreatedAt(), filter.from(), filter.to()))
            .filter(p -> filter.storeId() == null || (p.getStore() != null && p.getStore().getId().equals(filter.storeId())))
            .toList();

        long discards = stock.stream().filter(s -> s.getStatus() == StockStatus.DISCARDED).count();

        return new ReportsResponse(
            sortMap(inventoryByStorage),
            sortMap(inventoryByCategory),
            sortMap(shoppingByStore),
            sortMap(operationByEvent),
            expiringSoon,
            expired,
            storedTooLong,
            (long) purchases.size(),
            discards
        );
    }

    public String buildCsv(ReportFilter filter) {
        ReportsResponse r = build(filter);
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("expiringSoonCount,").append(r.expiringSoonCount()).append("\n");
        sb.append("expiredCount,").append(r.expiredCount()).append("\n");
        sb.append("storedTooLongCount,").append(r.storedTooLongCount()).append("\n");
        sb.append("purchasesCount,").append(r.purchasesCount()).append("\n");
        sb.append("discardsCount,").append(r.discardsCount()).append("\n");
        r.inventoryByStoragePlace().forEach((k,v) -> sb.append("inventoryByStoragePlace.").append(escape(k)).append(',').append(v).append("\n"));
        r.inventoryByCategory().forEach((k,v) -> sb.append("inventoryByCategory.").append(escape(k)).append(',').append(v).append("\n"));
        r.shoppingByStore().forEach((k,v) -> sb.append("shoppingByStore.").append(escape(k)).append(',').append(v).append("\n"));
        r.operationLogByEvent().forEach((k,v) -> sb.append("operationLogByEvent.").append(escape(k)).append(',').append(v).append("\n"));
        return sb.toString();
    }

    private <T> Map<String, Long> countBy(List<T> data, Function<T, String> keyFn) {
        return data.stream().collect(Collectors.groupingBy(keyFn, Collectors.counting()));
    }

    private Map<String, Long> sortMap(Map<String, Long> in) {
        return in.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }

    private boolean inRange(OffsetDateTime ts, OffsetDateTime from, OffsetDateTime to) {
        if (ts == null) return false;
        if (from != null && ts.isBefore(from)) return false;
        if (to != null && ts.isAfter(to)) return false;
        return true;
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
