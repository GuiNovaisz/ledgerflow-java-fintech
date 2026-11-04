# Injeção de dependências

**Antes e depois:** `InsightService` já recebia `InsightRepository` e `AccountService` pelo construtor, em campos `final`. A refatoração acrescentou `InsightCsvExporter` ao mesmo construtor, sem `@Autowired` no campo. O Spring monta o objeto; um teste pode fornecer dependências controladas.

**Problema evitado:** field injection esconde dependências obrigatórias e permite uma instância incompleta. **Princípio:** dependências explícitas e imutáveis tornam o estado válido mais fácil de enxergar. **Trade-off:** o construtor fica um pouco maior. Quando isso vira incômodo, primeiro verifique se a classe acumulou responsabilidades; não introduza Lombok apenas para ocultá-las. DI descreve a forma de fornecer objetos; DIP descreve a direção das dependências entre políticas e detalhes.
