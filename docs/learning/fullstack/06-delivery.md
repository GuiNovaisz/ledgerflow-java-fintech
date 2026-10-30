# 6. Docker, testes, CI e arquitetura completa

## Percurso de uma requisição

Uma pessoa preenche o formulário React. O cliente manda JSON e proteção CSRF para `/api`. Nginx ou Vite encaminham ao Java. Security autentica; o controller valida; o serviço aplica regras; o PostgreSQL confirma a transação. Só então o sucesso retorna e o cache é atualizado. Esse percurso conecta UI, HTTP, domínio e persistência sem colocar regras financeiras definitivas no navegador.

O projeto é um monólito modular, não microserviços. Há um backend de negócio. Node constrói e serve ferramentas de desenvolvimento; não existe um segundo backend financeiro Node. Essa escolha reduz operações distribuídas sem impedir organização interna por capacidade.

## Docker

Uma imagem descreve o ambiente executável; um contêiner é uma instância dessa imagem. O [Dockerfile](../../../Dockerfile) compila Java em uma etapa e executa com runtime em outra. O [frontend Dockerfile](../../../docker/frontend.Dockerfile) constrói assets com Node e os serve com Nginx sem root.

O [Compose](../../../docker-compose.yml) conecta três serviços por nomes internos. Dentro do contêiner Java, `postgres` é o banco; `localhost` apontaria para o próprio contêiner. Um volume preserva dados quando o contêiner é recriado. `down -v` remove volumes do projeto: não use isso como rotina de reinicialização quando pretende manter contas.

Healthcheck não prova toda a aplicação. O do Java inclui disponibilidade do banco; o do frontend verifica seu servidor HTTP. Um formulário pode continuar quebrado mesmo com todos os contêineres saudáveis, motivo para os testes de jornada.

## Camadas de testes

| Tipo | Exemplo | O que verifica |
| --- | --- | --- |
| Unidade Java | `AccountTest` | Regras de saldo/status sem servidor |
| Integração | `LedgerFlowIT`, `SessionSecurityIT`, `FullStackReportingIT` | API, segurança, SQL, Flyway e concorrência com PostgreSQL real |
| Unidade/componente UI | `money.test.ts`, `api.test.ts`, `ui.test.tsx` | Precisão, fronteira HTTP, validação e estados de tela |
| E2E | `fullstack.spec.ts` | Navegador, proxy, backend e banco juntos |
| Visual | `portfolio.spec.ts` | Captura dos fluxos reais e limites de largura |

Testcontainers inicia bancos isolados de teste. Um banco em memória com comportamento SQL diferente não seria evidência suficiente para locks do PostgreSQL. Playwright usa contexto headless isolado, sem cookies pessoais. Capturar uma tela bonita não demonstra atomicidade; passar testes de domínio não garante legibilidade no celular. As camadas se complementam.

## CI/CD e limites da automação

[ci.yml](../../../.github/workflows/ci.yml) instala versões de runtime, usa cache, executa testes/builds, consulta dependências, procura segredos e roda a stack com E2E. O ambiente CI recebe credenciais aleatórias e efêmeras. A remoção final com `-v` é apropriada somente para esse projeto descartável do runner.

CI significa integração contínua. CD pode significar entrega ou implantação contínua; aqui não há deploy público automático. Um workflow editado localmente não é um CI remoto aprovado. A v0.2.0 aguarda autorização para publicação, portanto seus resultados locais e remotos são reportados separadamente.

Gitleaks procura padrões de segredo; ausência de achados não garante que nenhum segredo exista. npm audit e OSV consultam bases conhecidas; não cobrem automaticamente sistema operacional das imagens, lógica de negócio ou toda configuração. Dois avisos Spring são mantidos com justificativa específica, não ignorados globalmente.

## Como defender a arquitetura

Apresente uma operação real, mostre onde suas invariantes são aplicadas e cite o teste que detectaria sua quebra. Explique por que metas manuais não alteram o ledger e por que transferências próprias não são renda. Finalize identificando uma limitação concreta e um plano testável de melhoria.
