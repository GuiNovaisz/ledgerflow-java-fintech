package com.ledgerflow.insights;

import com.ledgerflow.account.application.AccountService;
import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsightServiceTest {
    @Test
    void dashboardCoordinatesOwnerScopedQueriesAndMapsCalculatedAmounts() {
        InsightRepository repository = mock(InsightRepository.class);
        InsightService service = new InsightService(repository, mock(AccountService.class), mock(InsightCsvExporter.class));
        UUID owner = UUID.randomUUID();
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        when(repository.flows(eq(owner), any())).thenReturn(List.of(
                new FlowAmount(Direction.CREDIT, Category.SALARY, new BigDecimal("12.00")),
                new FlowAmount(Direction.DEBIT, Category.FOOD, new BigDecimal("2.00"))));
        when(repository.dailyChanges(eq(owner), any())).thenReturn(List.of());
        when(repository.openingBalance(eq(owner), any())).thenReturn(BigDecimal.ZERO);
        when(repository.totalBalance(owner)).thenReturn(new BigDecimal("10.00"));
        when(repository.operationCount(eq(owner), any())).thenReturn(2L);
        when(repository.entries(eq(owner), any(), any(), eq(0L), eq(6))).thenReturn(List.of());

        var dashboard = service.dashboard(owner, from, to);

        assertThat(dashboard.totalBalance()).isEqualTo("10.00");
        assertThat(dashboard.income()).isEqualTo("12.00");
        assertThat(dashboard.expenses()).isEqualTo("2.00");
        assertThat(dashboard.expensesByCategory()).containsEntry(Category.FOOD, "2.00");
        assertThat(dashboard.transactionCount()).isEqualTo(2);
        verify(repository).flows(eq(owner), any());
    }
}
