# LedgerFlow — atualização de qualidade Java / Java quality update

## Português brasileiro

### O que foi feito

- Auditei os controllers, os seis services, repositories, domínio, segurança, configuração e organização de pacotes. O diagnóstico e os limites da refatoração estão em [refactoring-report.md](refactoring-report.md).
- Centralizei a resolução do UUID autenticado em `@AuthenticatedUser` e `AuthenticatedUserResolver`. Antes, 19 métodos de seis controllers convertiam o sujeito do JWT; agora a conversão ocorre em um ponto. A validação do token e as verificações de propriedade dos recursos continuam ativas.
- Separei a montagem de CSV de `InsightService` em `InsightCsvExporter`. `CsvCell` concentra as regras compartilhadas de aspas e proteção contra fórmulas, removendo a dependência de insights em `ReportService`.
- Corrigi a especificação OpenAPI: login e cadastro aparecem como públicos, rotas de sessão protegidas exigem cookie, e respostas de erro comuns usam um esquema `ApiProblem`. Um teste de integração verifica a especificação gerada.
- Criei 12 guias em `docs/learning/java-quality/` e dois registros de decisão em `docs/architecture/decisions/`. As decisões explicam também por que não introduzimos Lombok, ArchUnit, interfaces para todos os controllers ou uma reorganização ampla de pacotes.

### Verificação e ambiente

O build com JDK 21 e PostgreSQL/Testcontainers passou: **32 testes unitários e 23 de integração**, sem falhas. Os contêineres locais foram reconstruídos e permaneceram ligados; a API respondeu `UP`, o frontend respondeu HTTP 200 e `/v3/api-docs` publicou OpenAPI 3.1.0. Os dados persistentes foram preservados.

Foram cadastradas duas identidades locais para conferência: `admin.local@ledgerflow.test` e `usuario.teste@ledgerflow.test`. As senhas geradas ficam somente em `.local/quality-test-accounts.json`, ignorado pelo Git. **O sistema ainda não implementa papéis de autorização; a conta identificada como ADMIN não possui privilégios adicionais.** Nenhuma senha ou segredo foi incluído nesta atualização.

### Histórico e revisão

Os commits `d8d9e8f`, `518b890`, `cc2a649` e `dae5d7b` já estavam na `DEV` antes desta branch documental. Esta branch registra o resumo bilíngue e retorna por merge à `DEV`; não há push nem release. Merecem revisão sênior o acoplamento do lock PostgreSQL em `TransactionService`, a possível extração futura do CSV de relatórios e a manutenção das regras de rota do customizer OpenAPI.

## English

### What changed

- Audited controllers, all six services, repositories, domain code, security, configuration, and package layout. The findings and scope are recorded in [refactoring-report.md](refactoring-report.md).
- Centralized authenticated UUID resolution in `@AuthenticatedUser` and `AuthenticatedUserResolver`. Previously, 19 methods across six controllers parsed the JWT subject; now conversion has one home. Token validation and resource ownership checks remain in place.
- Moved CSV rendering out of `InsightService` into `InsightCsvExporter`. `CsvCell` now holds shared quoting and spreadsheet formula protection, removing the dependency from insights to `ReportService`.
- Corrected OpenAPI security for public login and registration and protected browser session routes. Common error responses refer to an `ApiProblem` schema, and an integration test checks the generated specification.
- Added 12 learning guides under `docs/learning/java-quality/` and two decision records under `docs/architecture/decisions/`. They also explain why Lombok, ArchUnit, interfaces for every controller, and a broad package reshuffle were not warranted.

### Verification and local environment

The JDK 21 build with PostgreSQL/Testcontainers passed **32 unit tests and 23 integration tests**, with no failures. Local containers were rebuilt and left running; the API reported `UP`, the frontend returned HTTP 200, and `/v3/api-docs` served OpenAPI 3.1.0. Persistent data was retained.

Two local review identities were registered: `admin.local@ledgerflow.test` and `usuario.teste@ledgerflow.test`. Generated passwords are stored only in `.local/quality-test-accounts.json`, which Git ignores. **Authorization roles are not implemented yet, so the account labeled ADMIN has no additional privileges.** No password or secret is part of this update.

### History and review

Commits `d8d9e8f`, `518b890`, `cc2a649`, and `dae5d7b` were already on `DEV` before this documentation branch. This branch records the bilingual summary and merges back into `DEV`; it does not push or publish a release. Senior review should focus on the PostgreSQL lock boundary in `TransactionService`, a possible future extraction of report CSV rendering, and maintaining the OpenAPI route customizer as routes evolve.
