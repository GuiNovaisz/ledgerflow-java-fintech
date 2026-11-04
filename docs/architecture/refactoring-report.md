# Auditoria e refatoração Java — DEV

## Baseline e escopo

Ponto inicial: `DEV` em `14c873f`, árvore limpa. Os 29 testes unitários existentes passaram. A primeira tentativa de `-Pintegration verify` falhou antes de executar os testes de integração porque o Docker estava desligado. Após iniciá-lo, a primeira verificação com PostgreSQL/Testcontainers passou: 31 testes unitários (incluindo os dois novos testes do resolver) e 22 testes de integração. A verificação final passou com **32 testes unitários e 23 testes de integração**, sem falhas nem testes ignorados. Não houve alteração de migration, endpoint, payload financeiro ou histórico publicado.

## Services auditadas

| Service | Responsabilidade esperada | Responsabilidades observadas | Decisão |
| --- | --- | --- | --- |
| `AuthService` | cadastro, autenticação e perfil | normalização, hash, credenciais e emissão de token | Manter: política de autenticação coesa; `JwtTokenService` já isola emissão. |
| `AccountService` | ciclo de vida e visibilidade de contas | criação, listagem, lock, status e `requireOwned` | Manter: a consulta por owner e a checagem após lock protegem caminhos diferentes. |
| `TransactionService` | postagem financeira atômica | forma da operação, idempotência, lock PostgreSQL, saldos e lançamentos | Manter neste escopo: extração de lock/fingerprint pode alterar semântica; integração financeira já cobre o contrato. Revisão futura merece cautela. |
| `ReportService` | extrato e resumo por conta | consultas, período, agregação e CSV | Manter o fluxo de relatório; só mover codificação de célula para `CsvCell`. A serialização de extrato ainda pode ser extraída se crescer. |
| `InsightService` | consultas e agregação de visão financeira | dashboard, paginação, filtro, limite, ownership e montagem CSV | Extrair apenas a serialização para `InsightCsvExporter`. Manter políticas e consultas no serviço. |
| `GoalService` | metas de um proprietário | CRUD, lock por owner e versão otimista | Manter: regras e persistência compõem o caso de uso; sem duplicação relevante. |

## Mudanças efetivas

| Antes | Problema | Princípio | Depois | Benefício | Trade-off |
| --- | --- | --- | --- | --- | --- |
| 19 métodos em seis controllers convertiam `jwt.getSubject()` em UUID. | Conhecimento do JWT repetido em endpoints HTTP. | Coesão e um ponto de alteração. | `@AuthenticatedUser UUID` resolvido por `AuthenticatedUserResolver`. | Controllers tipados e um local para conversão; testes do resolver. | Uma anotação e um resolver do projeto. |
| `InsightService` montava o CSV e importava `ReportService.csvCell`. | Serviço de insights conhecia serialização e outra feature. | SRP e menor acoplamento. | `InsightCsvExporter` renderiza linhas; `CsvCell` compartilha a codificação segura. | Teste focal do formato e independência entre features. | Mais um componente injetado; `ReportService.csvCell` permanece como delegação compatível. |
| OpenAPI global exigia autenticação também no login de sessão. | Contrato publicado não refletia rota pública. | Contrato HTTP fiel. | `OpenApiConfiguration` distingue rotas públicas, sessão cookie e erros comuns. | Cliente vê segurança e respostas coerentes; contrato testado via `/v3/api-docs`. | Regras de rota no customizer exigem revisão quando rotas mudarem. |

## Outras verificações

- Repositories usam Spring Data; `AccountRepository.findByIdAndOwnerId` e `GoalRepository.lockOwned` incorporam escopo de proprietário. `TransactionService` verifica owner após lock de linha. Mantive essas checagens explícitas: segurança prevalece sobre DRY.
- Entidades `Account`, `Goal`, `Operation` e `LedgerEntry` concentram invariantes e/ou estado. DTOs são `record` e mappings curtos (`View.from`). Nenhum volume de conversão justificou MapStruct.
- `ApiExceptionHandler` já centralizava erros em `ProblemDetail`. JWT tem propriedades tipadas (`JwtProperties`) e validações de assinatura, emissor, audiência, prazo e UUID canônico. Não foi preciso novo advice, interface de segurança ou campo injetado.
- Não há pacote `util`, Feign, Twilio, streaming ou outras integrações externas da referência. Banco, JWT e HTTP permanecem nas fronteiras atuais. O lock PostgreSQL fica deliberadamente no caso de uso financeiro, que depende da sua atomicidade.
- Não há subtipos de services cujo contrato LSP esteja em risco, nem interfaces próprias grandes que exijam ISP. OCP/Strategy não justificam uma família de exportadores de uma implementação. Não acrescentei ArchUnit para regras que a estrutura pequena já torna verificáveis por revisão e integração.
- Pacotes por feature foram mantidos; `shared/CsvCell` contém apenas conhecimento técnico efetivamente compartilhado. Nenhuma reorganização em massa.
- Lombok não foi introduzido: records e construtores explícitos já removem a maior parte do boilerplate sem geração oculta; `@Data` em entidades JPA seria especialmente arriscado.
- Interfaces HTTP `XControllerApi` foram avaliadas e rejeitadas por ora: as anotações atuais são modestas e a documentação transversal está concentrada. Separar contrato HTTP não seria, por si, DIP.

## Verificação e revisão humana

Com JDK 21 local e Docker ativo, `./mvnw.cmd -B -ntp -Pintegration verify` terminou com `BUILD SUCCESS` (32 unitários, 23 integração). Os testes cobrem `/v3/api-docs`, autenticação bearer e sessão, CSV e ownership, operações financeiras com PostgreSQL. Pontos discutíveis para um Java Senior: limite entre `TransactionService` e o lock PostgreSQL; futura extração da serialização de `ReportService`; manutenção do customizer OpenAPI conforme novas rotas; eventual interface de controller se as anotações crescerem. Esta etapa é local na `DEV`; nenhum push ou release está autorizado pela missão.
