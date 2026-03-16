package com.holodos.export.application;

import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CsvExportService {
    private final ProductRepository productRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final OperationLogRepository operationLogRepository;

    public CsvExportService(ProductRepository productRepository,
                            ShoppingListItemRepository shoppingListItemRepository,
                            OperationLogRepository operationLogRepository) {
        this.productRepository = productRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.operationLogRepository = operationLogRepository;
    }

    public String exportProducts() {
        StringBuilder sb = new StringBuilder("id,name,category,defaultUnit,defaultStoragePlace,defaultStore,active\n");
        productRepository.findAll().forEach(p -> sb
            .append(p.getId()).append(',')
            .append(csv(p.getName())).append(',')
            .append(csv(p.getCategory().getName())).append(',')
            .append(csv(p.getDefaultUnit().getCode())).append(',')
            .append(csv(p.getDefaultStoragePlace().getName())).append(',')
            .append(csv(p.getDefaultStore() == null ? "" : p.getDefaultStore().getName())).append(',')
            .append(p.isActive())
            .append('\n'));
        return sb.toString();
    }

    public String exportShoppingList() {
        StringBuilder sb = new StringBuilder("id,title,quantity,unit,store,status,source,comment\n");
        shoppingListItemRepository.findAll().forEach(i -> sb
            .append(i.getId()).append(',')
            .append(csv(i.getTitle())).append(',')
            .append(i.getQuantity()).append(',')
            .append(csv(i.getUnit() == null ? "" : i.getUnit().getCode())).append(',')
            .append(csv(i.getStore() == null ? "" : i.getStore().getName())).append(',')
            .append(i.getStatus()).append(',')
            .append(i.getSource()).append(',')
            .append(csv(i.getComment()))
            .append('\n'));
        return sb.toString();
    }

    public String exportOperationLog() {
        StringBuilder sb = new StringBuilder("id,eventType,entityType,entityId,correlationId,createdAt\n");
        operationLogRepository.findAll().forEach(o -> sb
            .append(o.getId()).append(',')
            .append(csv(o.getEventType())).append(',')
            .append(csv(o.getEntityType())).append(',')
            .append(csv(o.getEntityId())).append(',')
            .append(csv(o.getCorrelationId())).append(',')
            .append(o.getCreatedAt())
            .append('\n'));
        return sb.toString();
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
