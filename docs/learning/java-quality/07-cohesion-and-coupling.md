# Coesão e acoplamento

**Antes:** `InsightService` chamava `ReportService.csvCell`, embora insight e relatório de conta tenham formatos de exportação diferentes. Isso acoplava uma feature à outra por um detalhe de texto. **Depois:** `InsightCsvExporter` cuida do formato de insights; `CsvCell` contém somente as regras compartilhadas de aspas e neutralização de fórmula.

Coesão significa que as operações de uma unidade servem ao mesmo propósito; acoplamento é quanto uma unidade precisa saber de outra. `AccountService.requireOwned` continua sendo usado por relatórios e insights porque a visibilidade de uma conta é conhecimento real compartilhado. **Trade-off:** nem toda semelhança merece extração; centralizar todas as verificações em uma política genérica esconderia a autorização onde ela é crítica.
