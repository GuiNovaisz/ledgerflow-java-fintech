package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowSummaryCalculatorTest {
    private final FlowSummaryCalculator calculator = new FlowSummaryCalculator();

    @Test void noFlowsHaveZeroTotalsAndNoCategories() {
        var result = calculator.calculate(List.of());
        assertThat(result.income()).isEqualByComparingTo("0");
        assertThat(result.expenses()).isEqualByComparingTo("0");
        assertThat(result.categories()).isEmpty();
    }

    @Test void creditsOnlyIncreaseIncome() {
        var result = calculator.calculate(List.of(flow(Direction.CREDIT, Category.SALARY, "12.34")));
        assertThat(result.income()).isEqualByComparingTo("12.34");
        assertThat(result.expenses()).isEqualByComparingTo("0");
        assertThat(result.categories()).isEmpty();
    }

    @Test void debitsOnlyIncreaseExpenses() {
        var result = calculator.calculate(List.of(flow(Direction.DEBIT, Category.FOOD, "5.25")));
        assertThat(result.income()).isEqualByComparingTo("0");
        assertThat(result.expenses()).isEqualByComparingTo("5.25");
        assertThat(result.categories().get(Category.FOOD)).isEqualByComparingTo("5.25");
    }

    @Test void mixedRepeatedCategoriesKeepDecimalPrecision() {
        var result = calculator.calculate(List.of(
                flow(Direction.CREDIT, Category.SALARY, "10.01"),
                flow(Direction.CREDIT, Category.OTHER, "2.02"),
                flow(Direction.DEBIT, Category.FOOD, "1.10"),
                flow(Direction.DEBIT, Category.FOOD, "2.20"),
                flow(Direction.DEBIT, Category.TRANSPORT, "0.03")));
        assertThat(result.income()).isEqualByComparingTo("12.03");
        assertThat(result.expenses()).isEqualByComparingTo("3.33");
        assertThat(result.categories().get(Category.FOOD)).isEqualByComparingTo("3.30");
        assertThat(result.categories().get(Category.TRANSPORT)).isEqualByComparingTo("0.03");
    }

    @Test void resultDoesNotExposeMutableCategoryMap() {
        var result = calculator.calculate(List.of(flow(Direction.DEBIT, Category.FOOD, "1.00")));
        assertThatThrownBy(() -> result.categories().put(Category.OTHER, BigDecimal.ONE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static FlowAmount flow(Direction direction, Category category, String total) {
        return new FlowAmount(direction, category, new BigDecimal(total));
    }
}
