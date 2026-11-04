# DIP não é sinônimo de interface

**Problema:** uma regra central que depende de um detalhe volátil pode obrigar o núcleo a mudar junto com ele. No LedgerFlow, `InsightService` importava `ReportService.csvCell`: uma feature conhecia a implementação de outra para formatar texto.

**Depois:** ambos os formatos usam `CsvCell`, conceito técnico compartilhado, e a exportação de insights tem seu próprio componente. Isso reduz uma dependência entre features, mas não é, por si só, um exemplo clássico de Dependency Inversion Principle: nenhuma interface foi criada para inverter a direção da dependência. A injeção do componente concreto pelo Spring também não prova DIP.

**Trade-off:** uma porta de exportação faria sentido quando surgisse uma implementação alternativa real ou um limite externo instável. Hoje ela adicionaria navegação e mocks sem ganho proporcional.
