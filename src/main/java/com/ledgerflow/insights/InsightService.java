package com.ledgerflow.insights;

import com.ledgerflow.account.application.AccountService;
import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import com.ledgerflow.reporting.application.ReportService;
import com.ledgerflow.shared.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.ledgerflow.insights.InsightDtos.*;
import static com.ledgerflow.insights.InsightRepository.money;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class InsightService {
    private final InsightRepository repository;
    private final AccountService accounts;
    InsightService(InsightRepository repository, AccountService accounts) { this.repository = repository; this.accounts = accounts; }
    public Dashboard dashboard(UUID owner, Instant from, Instant to) {
        InsightPeriod period = InsightPeriod.of(from, to);
        BigDecimal income = BigDecimal.ZERO, expenses = BigDecimal.ZERO;
        Map<Category, String> categories = new EnumMap<>(Category.class);
        for (var flow : repository.flows(owner, period)) {
            if (flow.direction() == Direction.CREDIT) income = income.add(flow.total());
            else { expenses = expenses.add(flow.total()); categories.put(flow.category(), money(flow.total())); }
        }
        Map<LocalDate, BigDecimal> changes = new HashMap<>();
        repository.dailyChanges(owner, period).forEach(change -> changes.put(change.date(), change.amount()));
        List<BalancePoint> series = new ArrayList<>();
        BigDecimal balance = repository.openingBalance(owner, period);
        LocalDate first = period.from().atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate last = period.to().minusNanos(1).atOffset(ZoneOffset.UTC).toLocalDate();
        // Equal normalized bounds represent a valid empty sub-microsecond interval.
        if (!period.from().isBefore(period.to())) last = first;
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            balance = balance.add(changes.getOrDefault(date, BigDecimal.ZERO));
            series.add(new BalancePoint(date, money(balance)));
        }
        return new Dashboard(money(repository.totalBalance(owner)), money(income), money(expenses), repository.operationCount(owner, period),
                series, categories, repository.entries(owner, period, new InsightRepository.Filter(null, null, null, null, null), 0, 6));
    }
    public EntryPage entries(UUID owner, Instant from, Instant to, InsightRepository.Filter filter, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        validateFilter(owner, filter);
        InsightPeriod period = InsightPeriod.of(from, to);
        long count = repository.count(owner, period, filter);
        return new EntryPage(repository.entries(owner, period, filter, (long) page * size, size), page, size, count, (count + size - 1) / size);
    }
    public String csv(UUID owner, Instant from, Instant to, InsightRepository.Filter filter) {
        validateFilter(owner, filter);
        InsightPeriod period = InsightPeriod.of(from, to);
        if (repository.count(owner, period, filter) > 10000) throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPORT_LIMIT", "CSV limit is 10000 entries. Select a shorter period.");
        StringBuilder csv = new StringBuilder("id,accountId,accountName,operationId,type,direction,amount,currency,category,description,createdAt\r\n");
        for (Entry entry : repository.entries(owner, period, filter, 0, 10000)) {
            csv.append(entry.id()).append(',').append(entry.accountId()).append(',').append(ReportService.csvCell(entry.accountName())).append(',')
                    .append(entry.operationId()).append(',').append(entry.type()).append(',').append(entry.direction()).append(',').append(entry.amount())
                    .append(",BRL,").append(entry.category()).append(',').append(ReportService.csvCell(entry.description())).append(',').append(entry.createdAt()).append("\r\n");
        }
        return csv.toString();
    }
    public OperationStatus operation(UUID owner, String key) {
        if (!key.matches("[A-Za-z0-9._:-]{1,100}")) throw new IllegalArgumentException("Invalid idempotency key");
        return repository.operation(owner, key).stream().findFirst().orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", "No committed operation was found for this key. An in-flight request may still complete."));
    }
    private void validateFilter(UUID owner, InsightRepository.Filter filter) {
        if (filter.accountId() != null) accounts.requireOwned(owner, filter.accountId());
        if (filter.q() != null && filter.q().length() > 100) throw new IllegalArgumentException("Search is limited to 100 characters");
    }
}
