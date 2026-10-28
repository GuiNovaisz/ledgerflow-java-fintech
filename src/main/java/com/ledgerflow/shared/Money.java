package com.ledgerflow.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal MAX = new BigDecimal("999999999999999.99");
    private Money() { }

    /** Reject fractional cents; never silently round a customer's input. */
    public static BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX) > 0 || value.scale() > 2) {
            throw new IllegalArgumentException("Amount must be positive, at most 15 integer digits and two decimal places");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
