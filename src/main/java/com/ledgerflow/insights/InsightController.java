package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import com.ledgerflow.ledger.domain.Operation.Type;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import static com.ledgerflow.insights.InsightDtos.*;

@RestController
@RequestMapping("/api/v1")
public class InsightController {
    private final InsightService insights;
    InsightController(InsightService insights) { this.insights = insights; }
    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return insights.dashboard(UUID.fromString(jwt.getSubject()), from, to);
    }
    @GetMapping("/entries")
    public EntryPage entries(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID accountId,
                            @RequestParam(required = false) String q, @RequestParam(required = false) Category category,
                            @RequestParam(required = false) Type type, @RequestParam(required = false) Direction direction,
                            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return insights.entries(UUID.fromString(jwt.getSubject()), from, to, new InsightRepository.Filter(accountId, q, category, type, direction), page, size);
    }
    @GetMapping("/entries/export.csv")
    public ResponseEntity<String> csv(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID accountId,
                                     @RequestParam(required = false) String q, @RequestParam(required = false) Category category,
                                     @RequestParam(required = false) Type type, @RequestParam(required = false) Direction direction,
                                     @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ledgerflow-statement.csv")
                .body(insights.csv(UUID.fromString(jwt.getSubject()), from, to, new InsightRepository.Filter(accountId, q, category, type, direction)));
    }
    @GetMapping("/operations/by-key/{key}")
    public OperationStatus operation(@AuthenticationPrincipal Jwt jwt, @PathVariable String key) { return insights.operation(UUID.fromString(jwt.getSubject()), key); }
}
