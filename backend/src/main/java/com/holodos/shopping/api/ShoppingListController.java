package com.holodos.shopping.api;

import com.holodos.shopping.api.ShoppingDtos.ShoppingItemResponse;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.application.ShoppingListService;
import com.holodos.shopping.domain.ShoppingItemStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {
    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping
    public Page<ShoppingItemResponse> list(
        @RequestParam(required = false) ShoppingItemStatus status,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String search,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return shoppingListService.list(status, storeId, search, pageable);
    }

    @PostMapping
    public ShoppingItemResponse create(@Valid @RequestBody ShoppingItemUpsertRequest request) {
        return shoppingListService.create(request);
    }

    @PutMapping("/{id}")
    public ShoppingItemResponse update(@PathVariable Long id, @Valid @RequestBody ShoppingItemUpsertRequest request) {
        return shoppingListService.update(id, request);
    }

    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id) {
        shoppingListService.markCompleted(id);
    }
}
