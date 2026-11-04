# SOLID como ferramenta de diagnóstico

**Problema real.** `InsightService` combinava consultas e agregação com a montagem linha a linha de CSV; seis controllers repetiam o parsing do JWT. **Solução.** A serialização foi para `InsightCsvExporter`; `AuthenticatedUserResolver` concentra a conversão do sujeito autenticado. É uma melhora de responsabilidade única (S) e redução de acoplamento.

O, L, I e D não são metas de contagem de interfaces. O LedgerFlow não tem uma família de implementações intercambiáveis que justifique Strategy para exportação, nem uma hierarquia de serviços cuja substituição precise ser provada. As interfaces `JpaRepository` existentes já oferecem contratos coesos. **Trade-off:** as duas extrações adicionam classes; cada uma elimina um motivo concreto para alterar vários lugares.
