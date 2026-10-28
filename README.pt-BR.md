# LedgerFlow

LedgerFlow é uma simulação fintech educacional feita com Java 21, Spring Boot 3.5, Maven e PostgreSQL 16. Oferece autenticação, contas em BRL, depósitos e saques simulados, transferências, histórico, resumos e extratos CSV.

> **Apenas simulação.** Depósitos e saques não se conectam a bancos, cartões, provedores de pagamento, sistemas de liquidação nem movimentam dinheiro real. Este repositório não é uma plataforma bancária de produção.

[English](README.md) · [Arquitetura](docs/architecture.md) · [Diagramas](docs/diagrams.md) · [Exemplos de API](docs/requests.http) · [Trilha de aprendizado](docs/learning/01-java-fundamentals.md) · [Evidências de verificação](docs/verification.md)

Verificado localmente: **37 testes aprovados**, migrations aplicadas, Compose saudável e fluxo HTTP real aprovado. O [relatório de entrega](docs/DELIVERY.pt-BR.md) reúne resultados, comandos, limitações e exercícios.

## Incluído

- Autenticação stateless por bearer JWT e senhas protegidas com BCrypt.
- Contas BRL restritas ao titular, com ativação e desativação.
- Depósitos, saques e transferências registrados em lançamentos imutáveis.
- Transações PostgreSQL, locks de linha e idempotência em operações financeiras.
- Histórico paginado, resumos em intervalos UTC e CSV com neutralização de fórmulas.
- Esquema gerenciado pelo Flyway e ambiente local com Docker Compose.

## Requisitos

Windows PowerShell, Docker Desktop (ou outro mecanismo compatível com Docker) e acesso à rede para baixar dependências de build e imagens de containers. JDK 21 é necessário para builds e testes Maven no host, mas não precisa estar instalado no host para iniciar via Docker Compose: o Dockerfile compila em uma imagem JDK 21 e executa em uma imagem Java 21. O Maven é fornecido por `mvnw.cmd`.

Confirme que `java -version` mostra Java 21. Se a máquina usa um JDK antigo por padrão, configure `JAVA_HOME` apenas no processo atual do PowerShell e adicione seu diretório `bin` ao início do `PATH`:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

Isso não altera as configurações globais do Java na máquina.

## Iniciar localmente

Na raiz do repositório:

```powershell
.\scripts\New-LocalEnvironment.ps1
docker compose up --build -d --wait
```

O script cria `.env` com segredos aleatórios e se recusa a sobrescrever um caminho existente. `.env` é ignorado pelo Git. Por padrão, o Compose publica o PostgreSQL em `127.0.0.1:5432` e a aplicação em `127.0.0.1:8080`; altere `APP_PORT` em `.env` para mudar a porta local da aplicação. Dentro da rede Compose, o banco é `postgres:5432`.

O Compose lê `.env` para configurar os containers. Um processo Spring Boot iniciado diretamente **não** carrega `.env` automaticamente: exporte `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` no ambiente daquele processo. Os padrões locais em `application.yml` usam `localhost:5432` e `ledgerflow`; informe `DB_PASSWORD` e `JWT_SECRET` explicitamente.

Documentação da API: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Saúde: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). O Compose verifica `/actuator/health/readiness`.

Para parar os containers preservando o volume local do banco:

```powershell
docker compose down
```

## Verificar

Testes unitários (JDK 21):

```powershell
.\mvnw.cmd test
```

Perfil de integração com PostgreSQL 16 via Testcontainers:

```powershell
.\mvnw.cmd -Pintegration verify
```

O perfil de integração exige um mecanismo Docker funcional e falha se o Testcontainers não iniciar o PostgreSQL. Uma suíte indisponível ou ignorada não está verificada. Consulte [docs/verification.md](docs/verification.md) para resultados observados e limites da verificação local.

Após iniciar o Compose, exercite a API real com PowerShell 7:

```powershell
.\scripts\Verify-Api.ps1
```

O script cria usuários e contas sintéticos e exibe resultados sem senhas ou tokens. Repetir a execução adiciona dados de exemplo; não apaga o banco existente.

## API resumida

Endpoints públicos: `POST /api/v1/auth/register` e `POST /api/v1/auth/login`. As demais rotas `/api/**` exigem `Authorization: Bearer <accessToken>`.

| Recurso | Rotas |
| --- | --- |
| Contas | `POST /api/v1/accounts`, `GET /api/v1/accounts`, `GET /api/v1/accounts/{id}`, `PATCH /api/v1/accounts/{id}/status` |
| Operações financeiras | `POST /api/v1/transactions/deposits`, `/withdrawals`, `/transfers` |
| Relatórios | `GET /api/v1/accounts/{id}/entries`, `/summary`, `/statement.csv` |

Toda operação financeira exige `Idempotency-Key` no formato `[A-Za-z0-9._:-]{1,100}`. Após um timeout, repita a mesma chave e o mesmo pedido semântico; o resultado salvo será reproduzido. Reutilizar a chave com um pedido alterado gera conflito. Pedidos que falham não reservam uma chave bem-sucedida. Veja [docs/requests.http](docs/requests.http) para exemplos concretos.

## Limites financeiros e de segurança

- Valores usam `BigDecimal`, devem ser positivos, ter até duas casas decimais e até 15 dígitos inteiros. Frações de centavo são rejeitadas; não há arredondamento silencioso. O PostgreSQL armazena `NUMERIC(17,2)`.
- Há suporte somente a BRL. As moedas das contas de uma transferência devem coincidir.
- O titular autenticado vem do JWT validado. Transferências podem ter como destino uma conta ativa de outra pessoa, mas a resposta não revela dados do titular destinatário. Lançamentos recebidos aparecem no histórico da conta.
- A operação e seus lançamentos são confirmados atomicamente. Uma transferência cria uma operação e dois lançamentos. É um livro interno de contas, não um razão contábil completo de partidas dobradas com contas externas de compensação.
- Locks consultivos transacionais específicos do PostgreSQL serializam chaves de idempotência iguais. Locks de linha protegem saldos e status durante operações concorrentes.
- Relatórios usam instantes UTC e intervalos semiabertos `[from,to)`. A exportação CSV tem limite de 10.000 linhas e neutraliza fórmulas de planilha em texto fornecido pelo usuário.
- Tokens de acesso JWT expiram em 15 minutos. Não há refresh token, revogação imediata, limite de requisições, MFA nem fluxos KYC/AML.
- Senhas de cadastro devem ter de 12 a 72 bytes UTF-8. O limite superior explicita o limite de entrada do BCrypt; um caractere pode ocupar mais de um byte.

## Arquitetura e aprendizado

Este monólito modular se organiza nas capacidades `auth`, `account`, `ledger`, `reporting` e `shared`. Comece pela [arquitetura](docs/architecture.md), depois siga os [capítulos de aprendizado](docs/learning/01-java-fundamentals.md). Cada capítulo ensina Java com classes reais do projeto e contém exatamente três alterações práticas para implementar e verificar.

## Próximos passos

Possíveis evoluções incluem ferramentas melhores de conciliação e auditoria, outras moedas com regras explícitas de conversão, monitoramento operacional e controles de identidade e abuso adequados à produção. Isso exige decisões de produto e arquitetura. Este README não afirma nem configura uma implantação pública.
