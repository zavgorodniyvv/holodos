package com.holodos.catalog.api;

import com.holodos.catalog.application.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/storage-places")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDtos.StoragePlaceResponse createStoragePlace(@Valid @RequestBody CatalogDtos.StoragePlaceRequest request) {
        return catalogService.createStoragePlace(request);
    }

    @GetMapping("/storage-places")
    public Page<CatalogDtos.StoragePlaceResponse> listStoragePlaces(Pageable pageable) {
        return catalogService.listStoragePlaces(pageable);
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDtos.UnitResponse createUnit(@Valid @RequestBody CatalogDtos.UnitRequest request) {
        return catalogService.createUnit(request);
    }

    @GetMapping("/units")
    public Page<CatalogDtos.UnitResponse> listUnits(Pageable pageable) {
        return catalogService.listUnits(pageable);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDtos.CategoryResponse createCategory(@Valid @RequestBody CatalogDtos.CategoryRequest request) {
        return catalogService.createCategory(request);
    }

    @GetMapping("/categories")
    public Page<CatalogDtos.CategoryResponse> listCategories(Pageable pageable) {
        return catalogService.listCategories(pageable);
    }

    @PostMapping("/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDtos.StoreResponse createStore(@Valid @RequestBody CatalogDtos.StoreRequest request) {
        return catalogService.createStore(request);
    }

    @GetMapping("/stores")
    public Page<CatalogDtos.StoreResponse> listStores(Pageable pageable) {
        return catalogService.listStores(pageable);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDtos.ProductResponse createProduct(@Valid @RequestBody CatalogDtos.ProductRequest request) {
        return catalogService.createProduct(request);
    }

    @GetMapping("/products")
    public Page<CatalogDtos.ProductResponse> listProducts(@RequestParam(required = false) String query,
                                                          Pageable pageable) {
        return catalogService.listProducts(query, pageable);
    }
}
