# ADR 0002 — Exportação CSV de insights

**Estado:** aceito. **Contexto:** `InsightService` concentrava consulta, política de exportação e formatação de CSV; ainda dependia de `ReportService.csvCell`.

**Decisão:** manter autorização, período e limite no caso de uso; delegar a renderização a `InsightCsvExporter`. Compartilhar somente a codificação segura de célula em `CsvCell`, usada também por relatórios. Preservar `ReportService.csvCell` como delegação para compatibilidade.

**Alternativas:** uma hierarquia genérica de exportadores ou Strategy de formatos acrescentaria abstração sem segundo formato; mover a política de limite para o exporter misturaria regra de acesso com serialização.

**Consequência:** mudanças de layout CSV ficam localizadas e a proteção contra fórmulas permanece centralizada. Há um bean adicional; o formato de relatório por conta permanece na service enquanto for pequeno.
