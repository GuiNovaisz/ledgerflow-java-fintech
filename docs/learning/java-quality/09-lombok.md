# Lombok: decisão proporcional

**Situação real:** o projeto não usa Lombok. Os DTOs como `InsightDtos.Entry` são `record`; services já possuem construtores explícitos e campos `final`. Nesta etapa, adicionar Lombok pouparia poucas linhas e acrescentaria processamento de anotações e dependência de ferramenta. Por isso não foi introduzido.

`@RequiredArgsConstructor` poderia reduzir construtores repetitivos em um projeto maior. `@Getter` pode servir a entidades JPA quando necessário. **Cuidado:** `@Data` gera `equals`, `hashCode` e `toString`; em entidades Hibernate isso pode atravessar relações lazy ou expor campos sensíveis. `@SneakyThrows` esconderia tratamento de falhas. **Trade-off:** mantemos um pouco de boilerplate visível para preservar clareza de dependências e comportamento gerado.
