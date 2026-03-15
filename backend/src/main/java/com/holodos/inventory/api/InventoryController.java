package com.holodos.inventory.api;

import com.holodos.inventory.api.InventoryDtos.AddStockRequest;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.api.InventoryDtos.MoveStockRequest;
import com.holodos.inventory.api.InventoryDtos.StockEntryResponse;
import com.holodos.inventory.application.InventoryService;
import com.holodos.inventory.domain.StockStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-entries")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Page<StockEntryResponse> list(
        @RequestParam(required = false) StockStatus status,
        @RequestParam(required = false) Long storagePlaceId,
        @RequestParam(required = false) String search,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return inventoryService.list(status, storagePlaceId, search, pageable);
    }

    @PostMapping
    public StockEntryResponse add(@Valid @RequestBody AddStockRequest request) {
        return inventoryService.addStock(request);
    }

    @PostMapping("/{id}/consume")
    public StockEntryResponse consume(@PathVariable Long id, @Valid @RequestBody ConsumeStockRequest request) {
        return inventoryService.consume(id, request);
    }

    @PostMapping("/{id}/discard")
    public StockEntryResponse discard(@PathVariable Long id) {
        return inventoryService.discard(id);
    }

    @PostMapping("/{id}/move")
    public StockEntryResponse move(@PathVariable Long id, @Valid @RequestBody MoveStockRequest request) {
        return inventoryService.move(id, request);
    }
}
