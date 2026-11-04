# Quando usar interfaces

As interfaces `AccountRepository` e `OperationRepository` já estendem Spring Data e expressam consultas como `findByIdAndOwnerId` e `findByOwnerIdAndIdempotencyKey`. Elas possuem uma função concreta: separar a consulta da implementação de persistência fornecida pelo framework.

**Ideia avaliada:** `XControllerApi` com mappings e OpenAPI, implementada por `XController`. **Decisão nesta etapa:** não adicionar. Os controllers atuais têm poucos endpoints e a documentação transversal foi concentrada em `OpenApiConfiguration`; a interface duplicaria assinatura e navegação sem diminuir a complexidade presente. Pode ser reavaliada se um contrato ficar dominado por anotações. **Trade-off:** os mappings seguem nos controllers. Separar contrato HTTP pode melhorar leitura, mas não demonstra automaticamente DIP.
