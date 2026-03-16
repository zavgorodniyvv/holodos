package com.holodos.reports.api;

import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.reports.api.ReportDtos.ReportsResponse;
import com.holodos.reports.application.ReportsService;
import java.time.OffsetDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping
    public ReportsResponse report(
        @RequestParam(required = false) OffsetDateTime from,
        @RequestParam(required = false) OffsetDateTime to,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long storagePlaceId,
        @RequestParam(required = false) Long storeId
    ) {
        return reportsService.build(new ReportFilter(from, to, categoryId, storagePlaceId, storeId));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
        @RequestParam(required = false) OffsetDateTime from,
        @RequestParam(required = false) OffsetDateTime to,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long storagePlaceId,
        @RequestParam(required = false) Long storeId
    ) {
        String csv = reportsService.buildCsv(new ReportFilter(from, to, categoryId, storagePlaceId, storeId));
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=holodos-report.csv")
            .body(csv);
    }
}
