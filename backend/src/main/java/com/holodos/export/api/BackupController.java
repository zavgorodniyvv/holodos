package com.holodos.export.api;

import com.holodos.export.application.BackupService;
import com.holodos.export.application.CsvExportService;
import com.holodos.export.domain.BackupSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class BackupController {
    private final BackupService backupService;
    private final CsvExportService csvExportService;

    public BackupController(BackupService backupService, CsvExportService csvExportService) {
        this.backupService = backupService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/json")
    public BackupSnapshot exportJson() {
        return backupService.exportSnapshot();
    }

    @GetMapping(value = "/json/download", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BackupSnapshot> downloadJson() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=holodos-backup.json")
            .body(backupService.exportSnapshot());
    }

    @GetMapping(value = "/csv/products", produces = "text/csv")
    public ResponseEntity<String> exportProductsCsv() {
        return csvResponse("holodos-products.csv", csvExportService.exportProducts());
    }

    @GetMapping(value = "/csv/shopping-list", produces = "text/csv")
    public ResponseEntity<String> exportShoppingListCsv() {
        return csvResponse("holodos-shopping-list.csv", csvExportService.exportShoppingList());
    }

    @GetMapping(value = "/csv/operation-log", produces = "text/csv")
    public ResponseEntity<String> exportOperationLogCsv() {
        return csvResponse("holodos-operation-log.csv", csvExportService.exportOperationLog());
    }

    @PostMapping("/restore")
    public RestoreResponse restore(@Valid @RequestBody RestoreRequest request) {
        var result = backupService.restoreSnapshot(request.snapshot(), request.clearExisting());
        return new RestoreResponse("OK", result.storagePlaces(), result.units(), result.categories(), result.stores(), result.products(), result.shoppingItems());
    }

    private ResponseEntity<String> csvResponse(String fileName, String payload) {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
            .body(payload);
    }

    public record RestoreRequest(BackupSnapshot snapshot, boolean clearExisting) {}
    public record RestoreResponse(String status, int storagePlaces, int units, int categories, int stores, int products, int shoppingItems) {}
}
