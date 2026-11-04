package com.ledgerflow.insights;

import com.ledgerflow.insights.InsightDtos.Entry;
import com.ledgerflow.shared.CsvCell;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
final class InsightCsvExporter {
    String render(List<Entry> entries) {
        StringBuilder csv = new StringBuilder("id,accountId,accountName,operationId,type,direction,amount,currency,category,description,createdAt\r\n");
        for (Entry entry : entries) {
            csv.append(entry.id()).append(',').append(entry.accountId()).append(',').append(CsvCell.encode(entry.accountName())).append(',')
                    .append(entry.operationId()).append(',').append(entry.type()).append(',').append(entry.direction()).append(',').append(entry.amount())
                    .append(",BRL,").append(entry.category()).append(',').append(CsvCell.encode(entry.description())).append(',').append(entry.createdAt()).append("\r\n");
        }
        return csv.toString();
    }
}
