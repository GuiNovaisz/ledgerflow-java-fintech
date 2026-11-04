package com.ledgerflow.reporting.api;

import com.ledgerflow.reporting.application.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.ledgerflow.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;
import static com.ledgerflow.reporting.api.ReportDtos.*;

@RestController @RequestMapping("/api/v1/accounts/{id}")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }
    @GetMapping("/entries")
    public EntryPage history(@AuthenticatedUser UUID userId, @PathVariable UUID id,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reports.history(userId, id, from, to, page, size);
    }
    @GetMapping("/summary")
    public Summary summary(@AuthenticatedUser UUID userId, @PathVariable UUID id,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return reports.summary(userId, id, from, to);
    }
    @GetMapping(value = "/statement.csv", produces = "text/csv")
    public ResponseEntity<String> csv(@AuthenticatedUser UUID userId, @PathVariable UUID id,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement.csv")
                .body(reports.csv(userId, id, from, to));
    }
}
