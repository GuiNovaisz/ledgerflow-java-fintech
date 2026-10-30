package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import com.ledgerflow.ledger.domain.Operation.Type;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import static com.ledgerflow.insights.InsightDtos.*;

@Repository
class InsightRepository {
    private static final String FROM = " from ledger_entries e join accounts a on a.id=e.account_id join operations o on o.id=e.operation_id ";
    private static final String OWNED_PERIOD = " a.owner_id=:owner and e.created_at>=:from and e.created_at<:to ";
    private static final String EXTERNAL_FLOW = " and not (o.type='TRANSFER' and exists(select 1 from accounts s where s.id=o.source_account_id and s.owner_id=:owner) and exists(select 1 from accounts t where t.id=o.target_account_id and t.owner_id=:owner)) ";
    private final NamedParameterJdbcTemplate jdbc;
    InsightRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    record Filter(UUID accountId, String q, Category category, Type type, Direction direction) { }
    record Query(String where, MapSqlParameterSource params) { }
    record DailyChange(LocalDate date, BigDecimal amount) { }
    record Flow(Direction direction, Category category, BigDecimal total) { }

    private Query query(UUID owner, InsightPeriod period, Filter filter) {
        MapSqlParameterSource params = params(owner, period);
        StringBuilder where = new StringBuilder(OWNED_PERIOD);
        if (filter.accountId() != null) { where.append(" and a.id=:account "); params.addValue("account", filter.accountId()); }
        if (filter.category() != null) { where.append(" and o.category=:category "); params.addValue("category", filter.category().name()); }
        if (filter.type() != null) { where.append(" and o.type=:type "); params.addValue("type", filter.type().name()); }
        if (filter.direction() != null) { where.append(" and e.direction=:direction "); params.addValue("direction", filter.direction().name()); }
        if (filter.q() != null && !filter.q().isBlank()) {
            // position() is literal substring search: %, _ and backslashes never become SQL wildcards.
            where.append(" and (position(lower(:q) in lower(o.description))>0 or position(lower(:q) in lower(a.name))>0) ");
            params.addValue("q", filter.q().trim());
        }
        return new Query(where.toString(), params);
    }
    List<Entry> entries(UUID owner, InsightPeriod period, Filter filter, long offset, int limit) {
        Query query = query(owner, period, filter);
        query.params().addValue("offset", offset).addValue("limit", limit);
        return jdbc.query("select e.id,e.account_id,a.name account_name,e.operation_id,o.type,e.direction,e.amount,o.category,o.description,e.created_at" + FROM
                + " where " + query.where() + " order by e.created_at desc,e.id desc limit :limit offset :offset", query.params(), (rs, n) -> entry(rs));
    }
    long count(UUID owner, InsightPeriod period, Filter filter) {
        Query query = query(owner, period, filter);
        return jdbc.queryForObject("select count(*)" + FROM + " where " + query.where(), query.params(), Long.class);
    }
    long operationCount(UUID owner, InsightPeriod period) {
        return jdbc.queryForObject("select count(distinct o.id)" + FROM + " where " + OWNED_PERIOD, params(owner, period), Long.class);
    }
    BigDecimal totalBalance(UUID owner) {
        return jdbc.queryForObject("select coalesce(sum(balance),0) from accounts where owner_id=:owner", Map.of("owner", owner), BigDecimal.class);
    }
    BigDecimal openingBalance(UUID owner, InsightPeriod period) {
        return jdbc.queryForObject("select coalesce(sum(case when e.direction='CREDIT' then e.amount else -e.amount end),0) from ledger_entries e join accounts a on a.id=e.account_id where a.owner_id=:owner and e.created_at<:from", params(owner, period), BigDecimal.class);
    }
    List<DailyChange> dailyChanges(UUID owner, InsightPeriod period) {
        return jdbc.query("select (e.created_at at time zone 'UTC')::date as posting_date, sum(case when e.direction='CREDIT' then e.amount else -e.amount end) amount"
                + FROM + " where " + OWNED_PERIOD + " group by posting_date order by posting_date", params(owner, period),
                (rs, n) -> new DailyChange(rs.getObject("posting_date", LocalDate.class), rs.getBigDecimal("amount")));
    }
    List<Flow> flows(UUID owner, InsightPeriod period) {
        return jdbc.query("select e.direction,o.category,sum(e.amount) total" + FROM + " where " + OWNED_PERIOD + EXTERNAL_FLOW
                + " group by e.direction,o.category", params(owner, period),
                (rs, n) -> new Flow(Direction.valueOf(rs.getString("direction")), Category.valueOf(rs.getString("category")), rs.getBigDecimal("total")));
    }
    List<OperationStatus> operation(UUID owner, String key) {
        return jdbc.query("select * from operations where owner_id=:owner and idempotency_key=:key", Map.of("owner", owner, "key", key),
                (rs, n) -> new OperationStatus(rs.getObject("id", UUID.class), Type.valueOf(rs.getString("type")),
                        rs.getObject("source_account_id", UUID.class), rs.getObject("target_account_id", UUID.class),
                        money(rs.getBigDecimal("amount")), Category.valueOf(rs.getString("category")), rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(), "COMPLETED"));
    }
    private static Entry entry(ResultSet rs) throws SQLException {
        return new Entry(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getString("account_name"),
                rs.getObject("operation_id", UUID.class), Type.valueOf(rs.getString("type")), Direction.valueOf(rs.getString("direction")),
                money(rs.getBigDecimal("amount")), Category.valueOf(rs.getString("category")), rs.getString("description"), rs.getTimestamp("created_at").toInstant());
    }
    static String money(BigDecimal amount) { return amount.setScale(2).toPlainString(); }
    private static MapSqlParameterSource params(UUID owner, InsightPeriod period) {
        return new MapSqlParameterSource("owner", owner).addValue("from", Timestamp.from(period.from())).addValue("to", Timestamp.from(period.to()));
    }
}
