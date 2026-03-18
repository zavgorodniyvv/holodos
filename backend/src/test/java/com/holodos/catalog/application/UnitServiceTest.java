package com.holodos.catalog.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    private UnitService unitService;

    @BeforeEach
    void setUp() {
        unitService = new UnitService(unitRepository);
    }

    @Test
    void deleteFailsWhenUnitMissing() {
        when(unitRepository.existsById(55L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> unitService.delete(55L));
    }
}
