# LedgerFlow — Java Fintech Backend

[![CI](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/workflows/ci.yml)

LedgerFlow é uma simulação fintech educacional para explorar como um pequeno livro de contas trata operações monetárias de forma consistente e segura. O projeto aborda preocupações comuns de backend — identidade, titularidade, alterações concorrentes de saldo, repetição de pedidos e histórico auditável — em uma aplicação executável. Depósitos e saques são simulados; nenhum dinheiro real é movimentado.

> **Apenas simulação.** O LedgerFlow não se conecta a bancos, cartões, provedores de pagamento, sistemas de liquidação nem movimenta dinheiro real. Não é uma plataforma bancária de produção.

[English](README.md) · [Arquitetura](docs/architecture.md) · [Diagramas](docs/diagrams.md) · [Exemplos de API](docs/requests.http) · [Trilha de aprendizado](docs/learning/01-java-fundamentals.md) · [Evidências de verificação](docs/verification.md)

## Funcionalidades

- Autenticação stateless por bearer JWT e senhas protegidas com BCrypt.
- Contas BRL restritas ao titular, com ativação e desativação.
- Depósitos, saques e transferências simulados registrados em lançamentos imutáveis.
- Transações PostgreSQL, locks de linha e chaves de idempotência para pedidos financeiros.
- Histórico paginado, resumos em intervalos UTC e extratos CSV com neutralização de fórmulas.
- Esquema gerenciado pelo Flyway, ambiente Docker Compose e documentação OpenAPI.

## Decisões técnicas

O projeto é um monólito modular: um processo Spring Boot e um banco PostgreSQL, organizados por capacidade (`auth`, `account`, `ledger`, `reporting`, `shared`). Valores usam `BigDecimal` e `NUMERIC` de escala fixa, evitando aritmética de ponto flutuante. Cada operação financeira altera saldos e registra lançamentos em uma única transação. Locks de linha serializam alterações de saldo; locks consultivos PostgreSQL transacionais coordenam chaves de idempotência iguais. Essas escolhas tornam as regras de consistência explícitas mantendo o exemplo compreensível. Consulte [arquitetura](docs/architecture.md) para invariantes e tradeoffs.

## Tecnologias

Java 21, Spring Boot 3.5, Maven Wrapper, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, Testcontainers, Docker Compose e springdoc OpenAPI.

## Requisitos

Para build e testes no host: JDK 21 e acesso à rede para dependências Maven. Para executar o serviço completo ou o perfil de integração PostgreSQL: Docker Engine com Compose (ou Docker Desktop no Windows). O wrapper fornece o Maven. O Compose compila com JDK 21 e executa a aplicação em uma imagem Java 21.

## Início rápido

Clone o repositório público e siga as instruções para o seu terminal:

```sh
git clone https://github.com/GuiNovaisz/ledgerflow-java-fintech.git
cd ledgerflow-java-fintech
```

### Linux / macOS

```sh
chmod +x mvnw
# Cria credenciais aleatórias locais e falha se .env já existir.
(set -C; umask 077; printf 'POSTGRES_PASSWORD=%s\nJWT_SECRET=%s\nAPP_PORT=8080\n' "$(openssl rand -base64 36 | tr -d '\n')" "$(openssl rand -base64 48 | tr -d '\n')" > .env)
docker compose up --build -d --wait
```

### Windows PowerShell

```powershell
# O Compose compila com JDK 21; não exige JDK instalado no host.
.\scripts\New-LocalEnvironment.ps1
docker compose up --build -d --wait
```

O script de ambiente cria `.env` com segredos aleatórios e se recusa a sobrescrever um caminho existente. `.env` é ignorado pelo Git. O Compose não exige JDK no host, pois compila a aplicação com JDK 21. Para builds ou testes Maven no host, instale JDK 21; testes unitários não exigem Docker, mas o perfil de integração PostgreSQL exige um engine compatível com Docker. O Compose publica PostgreSQL e aplicação somente em loopback por padrão; altere `APP_PORT` em `.env` para mudar a porta da aplicação. Na rede Compose, o banco é `postgres:5432`.

Interface OpenAPI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Saúde: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Para parar os containers preservando o volume local do banco:

```sh
docker compose down
```

## Testes e auditoria do release

Testes unitários: `./mvnw test` (Linux/macOS) ou `.\mvnw.cmd test` (PowerShell). O perfil de integração PostgreSQL é `./mvnw -Pintegration verify` ou `.\mvnw.cmd -Pintegration verify`; exige JDK 21 e Docker compatível e não deve ser descrito como verificado se foi ignorado ou ficou indisponível.

A [auditoria do release v0.1.0](docs/release-audit.md) registra **38 testes aprovados (28 unitários + 10 de integração PostgreSQL), nenhum ignorado**, Compose saudável e 10 verificações HTTP locais aprovadas. A [primeira execução no GitHub Actions](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/runs/40873784507) também aprovou a suíte Maven, o build da imagem Docker e a verificação de prontidão. O relatório documenta as atualizações de dependências e a avaliação de aplicabilidade dos dois alertas Spring restantes. O [registro anterior com 37 testes](docs/verification.md) foi preservado como evidência histórica.

Após iniciar o Compose, o script de smoke test da API para PowerShell 7 pode ser executado com `./scripts/Verify-Api.ps1`. Ele cria dados sintéticos e não limpa o banco.

## Visão geral da API

Endpoints públicos: `POST /api/v1/auth/register` e `POST /api/v1/auth/login`. As demais rotas `/api/**` exigem `Authorization: Bearer <accessToken>`.

| Recurso | Rotas |
| --- | --- |
| Contas | `POST /api/v1/accounts`, `GET /api/v1/accounts`, `GET /api/v1/accounts/{id}`, `PATCH /api/v1/accounts/{id}/status` |
| Operações financeiras | `POST /api/v1/transactions/deposits`, `/withdrawals`, `/transfers` |
| Relatórios | `GET /api/v1/accounts/{id}/entries`, `/summary`, `/statement.csv` |

Operações financeiras exigem `Idempotency-Key` no formato `[A-Za-z0-9._:-]{1,100}`. Após um timeout, repita a mesma chave e o mesmo pedido semântico; mudar o pedido para uma chave já usada gera conflito. Veja [exemplos da API](docs/requests.http).

## Limitações conhecidas e segurança

- Somente BRL; valores devem ser positivos, ter até duas casas decimais e caber na precisão suportada. Frações de centavo são rejeitadas, sem arredondamento.
- Transferências podem creditar a conta ativa de outra pessoa, mas dados do titular destinatário não são expostos. É um livro interno de contas, não um razão completo de partidas dobradas com contas externas de compensação.
- Relatórios usam instantes UTC e intervalos semiabertos `[from,to)`. CSV tem limite de 10.000 linhas e neutraliza fórmulas em texto fornecido pelo usuário.
- Tokens de acesso expiram em 15 minutos. Não há refresh token, revogação imediata, limite de requisições, MFA ou fluxos KYC/AML.
- O projeto não oferece conciliação externa, conversão cambial, tarifas, juros, operações de produção, compliance ou implantação pública.

## Próximos passos

Possíveis evoluções incluem ferramentas de conciliação e auditoria, monitoramento operacional e controles de identidade e abuso adequados a um serviço real. Isso exige decisões próprias de produto e arquitetura.

## Licença e notas da versão

[Licença MIT](LICENSE) · [Changelog](CHANGELOG.md) · [Notas da versão v0.1.0](docs/releases/v0.1.0.md) · [GitHub Release](https://github.com/GuiNovaisz/ledgerflow-java-fintech/releases/tag/v0.1.0)
