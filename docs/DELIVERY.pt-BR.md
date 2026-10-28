# LedgerFlow — relatório de entrega

Projeto desenvolvido no workspace autorizado em 28/10/2026. **É uma simulação financeira educacional, sem integração bancária ou dinheiro real.**

## 1. Funcionalidades implementadas

- Cadastro, login, hash BCrypt e JWT com expiração, emissor, audiência e identidade UUID validados.
- Contas BRL com identificadores UUID, saldo, listagem, histórico e ativação/inativação.
- Depósitos e saques simulados; transferências internas entre contas do mesmo usuário ou de usuários diferentes.
- Saldo protegido contra valores inválidos, saldo negativo, estouro de precisão e gastos concorrentes.
- Idempotência por usuário e chave, com rejeição de payload diferente e repetição da operação original.
- Lançamentos auditáveis protegidos contra atualização/exclusão, operação atômica e rollback.
- Histórico paginado, filtro de período UTC, entradas/saídas, despesas por categoria e CSV.
- OpenAPI/Swagger, Actuator, migrations Flyway, Docker Compose e pipeline GitHub Actions.

## 2. Testes executados e resultados

O comando `.\mvnw.cmd -B -ntp -Pintegration verify` passou com **37 testes: 28 unitários e 9 de integração, sem falhas, erros ou testes ignorados**. Os testes de integração usaram PostgreSQL 16 real via Testcontainers, incluindo concorrência, idempotência e uma falha de banco induzida para provar rollback.

A imagem multi-stage foi construída, o Compose inicializou aplicação e banco saudáveis, e o script HTTP executou o fluxo completo com usuários sintéticos. A reconciliação do banco não encontrou contas com divergência de saldo nem transferências desbalanceadas nos dados de demonstração.

Detalhes e evidências reproduzíveis: [verification.md](verification.md), [resumo dos testes](evidence/test-summary.json), [respostas HTTP reais](evidence/http-smoke.json) e [conferência do banco](evidence/database-check.json). O workflow ainda não foi executado no GitHub, pois não houve publicação do repositório.

## 3. Arquitetura

Monólito modular Java 21/Spring Boot: `auth`, `account`, `ledger`, `reporting`, `shared`, segurança e configuração. Controllers usam DTOs; serviços coordenam os casos de uso; entidades mantêm regras; repositories persistem os dados.

Cada operação financeira adquire um bloqueio transacional da chave de idempotência, verifica repetição, bloqueia as contas em ordem consistente, valida as regras e grava saldos/lançamentos numa única transação. PostgreSQL também impõe constraints e triggers de imutabilidade e consistência dos lançamentos. Valores usam `BigDecimal` e `NUMERIC(17,2)`; frações de centavo são rejeitadas, sem arredondamento silencioso.

Consulte [decisões e trade-offs](architecture.md) e [diagramas de arquitetura e entidades](diagrams.md).

## 4. Comandos para execução

Neste workspace a configuração `.env` já foi gerada com valores aleatórios e não está versionada. Para iniciar ou retomar:

```powershell
docker compose up -d --build --wait
```

Em um checkout novo, execute primeiro `.\scripts\New-LocalEnvironment.ps1`. O script não sobrescreve configurações existentes.

- Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
- Saúde: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).
- Exemplos manuais: [requests.http](requests.http).
- Verificação funcional: `.\scripts\Verify-Api.ps1` (PowerShell 7; cria dados sintéticos).
- Parar preservando os dados: `docker compose down`.

Para testar no terminal, selecione Java 21 no processo e execute:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools\jdk21\jdk-21.0.12.1+1').Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -Pintegration verify
```

Esse JDK portátil existe neste workspace e foi validado por checksum; em outro computador use seu próprio JDK 21. Não é preciso JDK instalado no host para executar somente o Compose.

## 5. Limitações

Somente BRL; sem tarifas, juros, câmbio ou integrações externas. Depósitos e saques são simulados. O ledger não substitui uma contabilidade bancária completa com contas de compensação externas. Os relatórios representam fluxo da conta e incluem transferências.

CSV limitado a 10 mil lançamentos por exportação. Paginação por offset pode mudar entre requisições quando novas operações chegam. JWT sem refresh/revogação imediata; sem rate limiting, MFA, KYC/AML ou controles operacionais de um banco. O ambiente local compartilha a função de migrations e acesso da aplicação no banco. Não foram realizados testes de carga ou auditoria de segurança independente.

## 6. Próximas melhorias

Prioridades sugeridas: limitar tentativas de login, separar credenciais de migration/runtime, implementar gestão de chaves e revogação, automatizar reconciliação, adotar paginação por cursor, ampliar testes das constraints diretamente no banco e definir política de backup/restauração. Integrações financeiras reais exigiriam um projeto e controles próprios.

## 7. Perguntas de entrevista e respostas esperadas

| Pergunta | Resposta esperada |
| --- | --- |
| Por que `BigDecimal`? | Representa decimais e permite definir escala/arredondamento explicitamente; não usar ponto flutuante para dinheiro. |
| Como uma transferência é atômica? | Saldo de origem, saldo de destino, operação e lançamentos pertencem à mesma transação; qualquer falha desfaz tudo. |
| Como evitar gasto concorrente? | Bloquear a conta antes de verificar o saldo e realizar a alteração; a segunda transação lê o saldo atualizado. |
| O que garante idempotência? | Chave por usuário, serialização transacional, fingerprint do pedido e constraint única; retries retornam a operação já salva. |
| Como impedir acesso à conta de outra pessoa? | Identidade vem do JWT validado e a consulta/ação exige proprietário; o cliente não escolhe `ownerId`. |
| Por que Testcontainers? | Exercita PostgreSQL, locks, triggers e migrations reais, que mocks ou outro banco não reproduzem fielmente. |

Mais perguntas e roteiro de estudo: [07-interview-questions.md](learning/07-interview-questions.md).

## 8. Modificações práticas para o proprietário

Há **21 exercícios**, três por capítulo, em [docs/learning](learning/01-java-fundamentals.md), começando por variáveis, condicionais, métodos e objetos. Para começar:

1. Criar um teste para o nome de conta com 100 e 101 caracteres, explorando Bean Validation.
2. Criar um pequeno método que formate um `BigDecimal` para exibição, preservando o cálculo e a API numérica existentes.
3. Testar que usuários diferentes podem usar a mesma chave de idempotência sem interferir um no outro.

Faça uma mudança por vez, execute os testes correspondentes e explique o comportamento antes/depois com suas palavras.

## Execução e delegação

Foram usados subagentes reais: GPT-6.1 Sol/High para arquitetura e revisão; GPT-6 Luna/Medium para autenticação, testes e infraestrutura; GPT-6 Luna/Low para documentação. O agente principal integrou o código, corrigiu problemas e executou as verificações. Não houve alegação de alternância automática do modelo principal nem de medição de custo.

O histórico Git local foi dividido por fundação, autenticação, operações financeiras, relatórios, testes, infraestrutura e documentação. Nenhum repositório foi publicado, nenhum deploy público foi realizado e nenhum serviço pago foi contratado.
