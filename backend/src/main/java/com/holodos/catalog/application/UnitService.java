package com.holodos.catalog.application;

import com.holodos.common.domain.NotFoundException;
import com.holodos.catalog.api.CatalogDtos.UnitResponse;
import com.holodos.catalog.api.CatalogDtos.UnitUpsertRequest;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.UnitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> list() {
        return unitRepository.findAll().stream().map(this::map).toList();
    }

    public UnitResponse create(UnitUpsertRequest request) {
        UnitOfMeasure entity = new UnitOfMeasure();
        apply(entity, request);
        return map(unitRepository.save(entity));
    }

    public UnitResponse update(Long id, UnitUpsertRequest request) {
        UnitOfMeasure entity = unitRepository.findById(id).orElseThrow(() -> new NotFoundException("Unit not found"));
        apply(entity, request);
        return map(unitRepository.save(entity));
    }

    public void delete(Long id) {
        if (!unitRepository.existsById(id)) {
            throw new NotFoundException("Unit not found");
        }
        unitRepository.deleteById(id);
    }

    private void apply(UnitOfMeasure entity, UnitUpsertRequest request) {
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setShortName(request.shortName());
        entity.setUnitType(request.unitType());
        entity.setActive(request.active());
    }

    private UnitResponse map(UnitOfMeasure entity) {
        return new UnitResponse(entity.getId(), entity.getCode(), entity.getName(), entity.getShortName(), entity.getUnitType(),
            entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
