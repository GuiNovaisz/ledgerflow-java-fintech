package com.ledgerflow.insights;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

record InsightPeriod(Instant from, Instant to) {
    static InsightPeriod of(Instant from, Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        if (!start.isBefore(end) || start.isBefore(Instant.EPOCH)
                || end.isAfter(Instant.parse("9999-12-31T23:59:59Z"))
                || Duration.between(start, end).compareTo(Duration.ofDays(366)) > 0) {
            throw new IllegalArgumentException("Choose an increasing UTC period of at most 366 days from 1970 through 9999");
        }
        return new InsightPeriod(ceil(start), ceil(end));
    }
    private static Instant ceil(Instant value) {
        Instant floor = value.truncatedTo(ChronoUnit.MICROS);
        return floor.equals(value) ? floor : floor.plus(1, ChronoUnit.MICROS);
    }
}
