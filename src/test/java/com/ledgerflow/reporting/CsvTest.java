package com.ledgerflow.reporting;

import com.ledgerflow.reporting.application.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {
    @Test void quotesCommasNewlinesAndQuotes() {
        assertThat(ReportService.csvCell("Food, \"today\"\nOK")).isEqualTo("\"Food, \"\"today\"\"\nOK\"");
    }
    @ParameterizedTest @ValueSource(strings = {"=SUM(A1)", "+1", "-1", "@cmd", "  =cmd", "\tformula", "\rformula", "\nformula"})
    void neutralizesSpreadsheetFormulas(String value) {
        assertThat(ReportService.csvCell(value)).startsWith("\"'");
    }
}
