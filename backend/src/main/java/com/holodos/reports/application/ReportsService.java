package com.holodos.reports.application;

import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.reports.api.ReportDtos.ReportsResponse;
import com.holodos.settings.application.UserSettingsService;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportsService {
    private static final OffsetDateTime MIN_TIME = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final StockEntryRepository stockEntryRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final OperationLogRepository operationLogRepository;
    private final UserSettingsService userSettingsService;
    private final ZoneId zone;

    public ReportsService(StockEntryRepository stockEntryRepository, PurchaseEventRepository purchaseEventRepository,
                          ShoppingListItemRepository shoppingListItemRepository, OperationLogRepository operationLogRepository,
                          UserSettingsService userSettingsService, @Value("${holodos.time-zone:Europe/Kyiv}") String zone) {
        this.stockEntryRepository = stockEntryRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.operationLogRepository = operationLogRepository;
        this.userSettingsService = userSettingsService;
        this.zone = ZoneId.of(zone);
    }

    public ReportsResponse build(ReportFilter filter) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = filter.from() == null ? MIN_TIME : filter.from();
        OffsetDateTime to = filter.to() == null ? now.plusYears(100) : filter.to();

        // Current inventory only: consumed/discarded batches are history, not stock
        List<StockEntry> stock = stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE).stream()
            .filter(s -> inRange(s.getAddedAt(), filter.from(), filter.to()))
            .filter(s -> filter.storagePlaceId() == null || s.getStoragePlace().getId().equals(filter.storagePlaceId()))
            .filter(s -> filter.categoryId() == null || s.getProduct().getCategory().getId().equals(filter.categoryId()))
            .toList();
        Map<String, Long> inventoryByStorage = countBy(stock, s -> s.getStoragePlace().getName());
        Map<String, Long> inventoryByCategory = countBy(stock, s -> s.getProduct().getCategory().getName());
        Map<String, Long> shoppingByStore = shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream()
            .filter(i -> filter.storeId() == null || (i.getStore() != null && i.getStore().getId().equals(filter.storeId())))
            .collect(Collectors.groupingBy(i -> i.getStore() == null ? "UNASSIGNED" : i.getStore().getName(), Collectors.counting()));
        Map<String, Long> operationByEvent = new HashMap<>();
        for (Object[] row : operationLogRepository.countByEventTypeBetween(from, to)) {
            operationByEvent.put((String) row[0], ((Number) row[1]).longValue());
        }

        // Expiry is a calendar date stored as midnight UTC; "today" is the household's date, as in the UI and notifications
        LocalDate today = now.atZoneSameInstant(zone).toLocalDate();
        LocalDate expiringBoundary = today.plusDays(userSettingsService.getOrDefault("default").getExpiryDaysBeforeNotify());
        long expiringSoon = stock.stream().map(s -> utcDate(s.getExpiresAt()))
            .filter(d -> d != null && !d.isBefore(today) && !d.isAfter(expiringBoundary)).count();
        long expired = stock.stream().map(s -> utcDate(s.getExpiresAt()))
            .filter(d -> d != null && d.isBefore(today)).count();
        long storedTooLong = stock.stream().filter(s -> s.getAddedAt() != null && s.getAddedAt().isBefore(now.minusDays(365))).count();
        long purchases = filter.storeId() == null
            ? purchaseEventRepository.countByPurchasedAtBetween(from, to)
            : purchaseEventRepository.countByPurchasedAtBetweenAndStoreId(from, to, filter.storeId());
        long discards = stockEntryRepository.count(discardedMatching(filter, from, to));
        return new ReportsResponse(
            sortMap(inventoryByStorage),
            sortMap(inventoryByCategory),
            sortMap(shoppingByStore),
            sortMap(operationByEvent),
            expiringSoon,
            expired,
            storedTooLong,
            purchases,
            discards
        );
    }

    private static Specification<StockEntry> discardedMatching(ReportFilter filter, OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            var predicate = cb.and(
                cb.equal(root.get("status"), StockStatus.DISCARDED),
                cb.between(root.get("addedAt"), from, to));
            if (filter.storagePlaceId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("storagePlace").get("id"), filter.storagePlaceId()));
            }
            if (filter.categoryId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("product").get("category").get("id"), filter.categoryId()));
            }
            return predicate;
        };
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

    private static LocalDate utcDate(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
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
