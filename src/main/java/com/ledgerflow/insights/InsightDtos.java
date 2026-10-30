package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import com.ledgerflow.ledger.domain.Operation.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Decimal strings preserve the database's full cent precision in JavaScript clients. */
public final class InsightDtos {
    private InsightDtos() { }
    public record Entry(UUID id, UUID accountId, String accountName, UUID operationId, Type type,
                        Direction direction, String amount, Category category, String description, Instant createdAt) { }
    public record EntryPage(List<Entry> content, int page, int size, long totalElements, long totalPages) { }
    public record BalancePoint(LocalDate date, String balance) { }
    public record Dashboard(String totalBalance, String income, String expenses, long transactionCount,
                            List<BalancePoint> balanceSeries, Map<Category, String> expensesByCategory,
                            List<Entry> recentEntries) { }
    public record OperationStatus(UUID id, Type type, UUID sourceAccountId, UUID targetAccountId,
                                  String amount, Category category, String description, Instant createdAt, String status) { }
}
