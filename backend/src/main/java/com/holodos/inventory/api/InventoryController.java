package com.holodos.inventory.api;

import com.holodos.inventory.api.InventoryDtos.AddStockRequest;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.api.InventoryDtos.MoveStockRequest;
import com.holodos.inventory.api.InventoryDtos.StockEntryResponse;
import com.holodos.inventory.application.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-entries")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<StockEntryResponse> list() {
        return inventoryService.list();
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
