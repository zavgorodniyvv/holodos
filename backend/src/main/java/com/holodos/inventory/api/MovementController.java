package com.holodos.inventory.api;

import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class MovementController {
    private final InventoryMovementRepository movementRepository;

    public MovementController(InventoryMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @GetMapping
    public List<MovementResponse> list() {
        return movementRepository.findAll().stream().map(m -> new MovementResponse(
            m.getId(),
            m.getProduct().getId(),
            m.getStockEntry().getId(),
            m.getFromStoragePlace().getId(),
            m.getToStoragePlace().getId(),
            m.getQuantity(),
            m.getMovedAt(),
            m.getComment(),
            m.getUsername(),
            m.getCreatedAt()
        )).toList();
    }

    public record MovementResponse(
        Long id,
        Long productId,
        Long stockEntryId,
        Long fromStoragePlaceId,
        Long toStoragePlaceId,
        BigDecimal quantity,
        OffsetDateTime movedAt,
        String comment,
        String username,
        OffsetDateTime createdAt
    ) {}
}
