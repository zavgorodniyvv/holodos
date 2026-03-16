package com.holodos.catalog.api;

import com.holodos.catalog.api.CatalogDtos.DictionaryResponse;
import com.holodos.catalog.api.CatalogDtos.DictionaryUpsertRequest;
import com.holodos.catalog.api.CatalogDtos.ProductResponse;
import com.holodos.catalog.api.CatalogDtos.ProductUpsertRequest;
import com.holodos.catalog.api.CatalogDtos.UnitResponse;
import com.holodos.catalog.api.CatalogDtos.UnitUpsertRequest;
import com.holodos.catalog.application.CatalogDictionaryService;
import com.holodos.catalog.application.ProductService;
import com.holodos.catalog.application.UnitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class CatalogController {

    private final CatalogDictionaryService dictionaryService;
    private final UnitService unitService;
    private final ProductService productService;

    public CatalogController(
        CatalogDictionaryService dictionaryService,
        UnitService unitService,
        ProductService productService
    ) {
        this.dictionaryService = dictionaryService;
        this.unitService = unitService;
        this.productService = productService;
    }

    @GetMapping("/storage-places")
    public List<DictionaryResponse> listStoragePlaces() { return dictionaryService.listStoragePlaces(); }

    @PostMapping("/storage-places")
    public DictionaryResponse createStoragePlace(@Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.createStoragePlace(request); }

    @PutMapping("/storage-places/{id}")
    public DictionaryResponse updateStoragePlace(@PathVariable Long id, @Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.updateStoragePlace(id, request); }

    @DeleteMapping("/storage-places/{id}")
    public void deleteStoragePlace(@PathVariable Long id) { dictionaryService.deleteStoragePlace(id); }

    @GetMapping("/categories")
    public List<DictionaryResponse> listCategories() { return dictionaryService.listCategories(); }

    @PostMapping("/categories")
    public DictionaryResponse createCategory(@Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.createCategory(request); }

    @PutMapping("/categories/{id}")
    public DictionaryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.updateCategory(id, request); }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) { dictionaryService.deleteCategory(id); }

    @GetMapping("/stores")
    public List<DictionaryResponse> listStores() { return dictionaryService.listStores(); }

    @PostMapping("/stores")
    public DictionaryResponse createStore(@Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.createStore(request); }

    @PutMapping("/stores/{id}")
    public DictionaryResponse updateStore(@PathVariable Long id, @Valid @RequestBody DictionaryUpsertRequest request) { return dictionaryService.updateStore(id, request); }

    @DeleteMapping("/stores/{id}")
    public void deleteStore(@PathVariable Long id) { dictionaryService.deleteStore(id); }

    @GetMapping("/units")
    public List<UnitResponse> listUnits() { return unitService.list(); }

    @PostMapping("/units")
    public UnitResponse createUnit(@Valid @RequestBody UnitUpsertRequest request) { return unitService.create(request); }

    @PutMapping("/units/{id}")
    public UnitResponse updateUnit(@PathVariable Long id, @Valid @RequestBody UnitUpsertRequest request) { return unitService.update(id, request); }

    @DeleteMapping("/units/{id}")
    public void deleteUnit(@PathVariable Long id) { unitService.delete(id); }

    @GetMapping("/products")
    public Page<ProductResponse> listProducts(
        @RequestParam(required = false) String search,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return productService.list(search, pageable);
    }

    @PostMapping("/products")
    public ProductResponse createProduct(@Valid @RequestBody ProductUpsertRequest request) { return productService.create(request); }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequest request) { return productService.update(id, request); }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) { productService.delete(id); }
}
