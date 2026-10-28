package com.ledgerflow.reporting.api;

import com.ledgerflow.ledger.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ReportDtos {
    private ReportDtos() { }
    public record EntryView(UUID id, UUID operationId, Operation.Type type, LedgerEntry.Direction direction,
                            BigDecimal amount, Category category, String description, Instant createdAt) {
        public static EntryView from(LedgerEntry e) {
            Operation op = e.getOperation();
            return new EntryView(e.getId(), op.getId(), op.getType(), e.getDirection(), e.getAmount(), op.getCategory(), op.getDescription(), e.getCreatedAt());
        }
    }
    public record EntryPage(List<EntryView> content, int page, int size, long totalElements, int totalPages) { }
    public record Summary(BigDecimal income, BigDecimal expenses, BigDecimal net, Map<Category, BigDecimal> expensesByCategory) { }
}
