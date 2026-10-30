package com.ledgerflow.goals;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class GoalDtos {
    private GoalDtos() { }
    public record CreateGoal(@NotBlank @Size(max = 100) String name,
                             @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
                             @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal currentAmount,
                             LocalDate dueDate) { }
    public record UpdateGoal(@NotBlank @Size(max = 100) String name,
                             @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
                             @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal currentAmount,
                             LocalDate dueDate, @NotNull @PositiveOrZero Long version) { }
    public record GoalView(UUID id, String name, String targetAmount, String currentAmount, LocalDate dueDate, String status, long version) {
        static GoalView from(Goal goal) {
            return new GoalView(goal.getId(), goal.getName(), goal.getTargetAmount().toPlainString(), goal.getCurrentAmount().toPlainString(), goal.getDueDate(), goal.getStatus(), goal.getVersion());
        }
    }
}
