# Responsabilidade única: insights e CSV

**Antes:** `InsightService.csv()` verificava ownership e limite, buscava os lançamentos e montava cabeçalho, aspas e linhas CSV. Mudanças no formato de exportação exigiam editar o serviço que também calcula dashboard e pagina entradas.

**Depois:** `InsightService` mantém a política de acesso, intervalo e limite de 10.000 linhas; `InsightCsvExporter.render(entries)` escreve o contrato CSV e usa `CsvCell.encode` para texto controlado pelo usuário. O teste `InsightCsvExporterTest` fixa formato e proteção contra fórmulas.

**Princípio:** motivos independentes para mudança devem ter donos claros. **Trade-off:** há mais um componente injetado, mas a decisão financeira e a serialização agora podem ser testadas separadamente.
