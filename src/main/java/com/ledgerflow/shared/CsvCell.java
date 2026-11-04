package com.ledgerflow.shared;

/** Quotes CSV fields and neutralizes formulas in user-controlled text. */
public final class CsvCell {
    private CsvCell() { }
    public static String encode(String value) {
        String visible = value.stripLeading();
        if ((!visible.isEmpty() && "=+-@".indexOf(visible.charAt(0)) >= 0)
                || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
