package com.holodos.inventory.api;

import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class MovementController {
    private final InventoryMovementRepository movementRepository;

    public MovementController(InventoryMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @GetMapping
    public Page<MovementResponse> list(
        @RequestParam(required = false) Long fromStoragePlaceId,
        @RequestParam(required = false) Long toStoragePlaceId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Specification<com.holodos.inventory.domain.InventoryMovement> spec = Specification.where((Specification<com.holodos.inventory.domain.InventoryMovement>) null);
        if (fromStoragePlaceId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("fromStoragePlace").get("id"), fromStoragePlaceId));
        }
        if (toStoragePlaceId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("toStoragePlace").get("id"), toStoragePlaceId));
        }
        return movementRepository.findAll(spec, pageable).map(m -> new MovementResponse(
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
        ));
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
