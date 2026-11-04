# Refatorar sem mudar o contrato

**Baseline:** 29 testes unitários passaram; a primeira execução de integração encontrou o Docker desligado. Após iniciar o ambiente, uma verificação intermediária passou com 31 testes unitários e 22 de integração; ao final, 32 e 23 passaram com PostgreSQL. Isso separou falha de ambiente de regressão do código.

**Mudança observável preservada:** `/api/v1/entries/export` ainda usa o mesmo cabeçalho, CRLF, limite, ordenação e neutralização de texto. O teste de integração existente cobre ownership e CSV; `InsightCsvExporterTest` fixa uma linha com aspas, vírgula e fórmula. A extração do JWT foi coberta por teste unitário e integração bearer/sessão.

**Trade-off:** testes de comportamento exigem manutenção, mas permitem modificar a estrutura interna sem confiar apenas em inspeção visual. Confira a execução final no relatório arquitetural.
