package com.ledgerflow.reporting.application;

import com.ledgerflow.account.application.AccountService;
import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.persistence.LedgerEntryRepository;
import com.ledgerflow.reporting.api.ReportDtos.*;
import com.ledgerflow.shared.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReportService {
    private static final Sort ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final AccountService accounts;
    private final LedgerEntryRepository entries;
    private final JdbcTemplate jdbc;
    public ReportService(AccountService accounts, LedgerEntryRepository entries, JdbcTemplate jdbc) {
        this.accounts = accounts; this.entries = entries; this.jdbc = jdbc;
    }
    public EntryPage history(UUID owner, UUID accountId, Instant from, Instant to, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        accounts.requireOwned(owner, accountId);
        Period period = period(from, to);
        var result = entries.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(accountId,
                period.from(), period.to(), PageRequest.of(page, size, ORDER));
        return new EntryPage(result.getContent().stream().map(EntryView::from).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    public Summary summary(UUID owner, UUID accountId, Instant from, Instant to) {
        accounts.requireOwned(owner, accountId);
        Period period = period(from, to);
        var rows = jdbc.queryForList("""
                select e.direction, o.category, sum(e.amount) as total
                from ledger_entries e join operations o on o.id = e.operation_id
                where e.account_id = ? and e.created_at >= ? and e.created_at < ?
                group by e.direction, o.category
                """, accountId, Timestamp.from(period.from()), Timestamp.from(period.to()));
        BigDecimal income = new BigDecimal("0.00"), expenses = new BigDecimal("0.00");
        Map<Category, BigDecimal> categories = new EnumMap<>(Category.class);
        for (var row : rows) {
            BigDecimal total = (BigDecimal) row.get("total");
            if ("CREDIT".equals(row.get("direction"))) income = income.add(total);
            else {
                expenses = expenses.add(total);
                categories.put(Category.valueOf((String) row.get("category")), total);
            }
        }
        return new Summary(income, expenses, income.subtract(expenses), categories);
    }
    public String csv(UUID owner, UUID accountId, Instant from, Instant to) {
        accounts.requireOwned(owner, accountId);
        Period period = period(from, to);
        var result = entries.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(accountId,
                period.from(), period.to(), PageRequest.of(0, 10001, ORDER));
        if (result.getTotalElements() > 10000) throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPORT_LIMIT", "CSV limit is 10000 entries. Select a shorter period.");
        StringBuilder csv = new StringBuilder("id,operationId,type,direction,amount,currency,category,description,createdAt\r\n");
        for (var entry : result.getContent()) {
            EntryView e = EntryView.from(entry);
            csv.append(e.id()).append(',').append(e.operationId()).append(',').append(e.type()).append(',')
                    .append(e.direction()).append(',').append(e.amount().toPlainString()).append(",BRL,")
                    .append(e.category()).append(',').append(csvCell(e.description())).append(',').append(e.createdAt()).append("\r\n");
        }
        return csv.toString();
    }
    public static String csvCell(String value) {
        String visible = value.stripLeading();
        if (!visible.isEmpty() && "=+-@".indexOf(visible.charAt(0)) >= 0 || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    private Period period(Instant from, Instant to) {
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plusSeconds(1) : to;
        if (!start.isBefore(end) || start.isBefore(Instant.EPOCH) || end.isAfter(Instant.parse("9999-12-31T23:59:59Z")))
            throw new IllegalArgumentException("Period must be an increasing UTC interval from year 1970 through 9999");
        return new Period(start, end);
    }
    private record Period(Instant from, Instant to) { }
}
