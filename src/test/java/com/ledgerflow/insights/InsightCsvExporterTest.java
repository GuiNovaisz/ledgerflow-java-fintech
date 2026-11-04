package com.ledgerflow.insights;

import com.ledgerflow.insights.InsightDtos.Entry;
import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import com.ledgerflow.ledger.domain.Operation.Type;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsightCsvExporterTest {
    @Test
    void keepsTheExportContractAndNeutralizesUserControlledFields() {
        UUID id = UUID.randomUUID(), account = UUID.randomUUID(), operation = UUID.randomUUID();
        Entry entry = new Entry(id, account, "=Savings, \"main\"", operation, Type.DEPOSIT,
                Direction.CREDIT, "12.34", Category.SALARY, "+formula\ntext", Instant.parse("2026-01-02T03:04:05Z"));

        assertThat(new InsightCsvExporter().render(List.of(entry))).isEqualTo(
                "id,accountId,accountName,operationId,type,direction,amount,currency,category,description,createdAt\r\n"
                + id + "," + account + ",\"'=Savings, \"\"main\"\"\"," + operation
                + ",DEPOSIT,CREDIT,12.34,BRL,SALARY,\"'+formula\ntext\",2026-01-02T03:04:05Z\r\n");
    }
}
