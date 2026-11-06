package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Aggregates already retrieved external flows without presentation formatting. */
final class FlowSummaryCalculator {
    FlowSummary calculate(List<FlowAmount> flows) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        Map<Category, BigDecimal> categories = new EnumMap<>(Category.class);
        for (FlowAmount flow : flows) {
            if (flow.direction() == Direction.CREDIT) income = income.add(flow.total());
            else {
                expenses = expenses.add(flow.total());
                categories.merge(flow.category(), flow.total(), BigDecimal::add);
            }
        }
        return new FlowSummary(income, expenses, categories);
    }

    record FlowSummary(BigDecimal income, BigDecimal expenses, Map<Category, BigDecimal> categories) {
        FlowSummary {
            Map<Category, BigDecimal> copy = new EnumMap<>(Category.class);
            copy.putAll(categories);
            categories = Collections.unmodifiableMap(copy);
        }
    }
}
