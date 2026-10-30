# Verificação e prontidão — LedgerFlow v0.2.0

**Consolidado em 30/10/2026. Status: candidato local pronto para revisão; publicação não autorizada nesta etapa.**

Branch: `feature/fullstack-dashboard`. A base pública `main` e o destino da tag v0.1.0 permanecem em `1e631f5dc95e6800a6902cd07b48967391c2b123`. Consulta remota de leitura em 30/10 confirmou essa preservação e ausência de tag v0.2.0. Não houve merge, push, tag, release nova nem deploy público.

## Evolução e arquitetura

Frontend React/TypeScript estrito integrado ao monólito Java por proxy same-origin. PostgreSQL continua sendo a fonte da verdade. O backend acrescenta sessões de navegador com CSRF, analytics/extrato por proprietário, consulta por chave de operação e metas versionadas via Flyway V2. A cadeia bearer e o ledger anterior são preservados.

Entrega funcional: cadastro/login/logout, dashboard real, contas/status/detalhes, depósitos/saques/transferências, busca/filtros/paginação/CSV, relatórios, categorias fixas, metas manuais, perfil de leitura, preferências de idioma e tema. A [arquitetura](fullstack-architecture.md) separa funcionalidades reais, extensões justificadas e elementos do mockup omitidos por não existir domínio correspondente.

## Evidências observadas

| Verificação | Resultado | Escopo |
| --- | --- | --- |
| Maven `-Pintegration verify` | 51 testes; zero falhas/erros/skips; BUILD SUCCESS | 29 unitários + 22 integrações com PostgreSQL/Testcontainers e Flyway V1/V2; execução concluída em 29/10 |
| ESLint | Aprovado | Frontend e testes; reexecutado em 30/10 |
| TypeScript estrito | Aprovado | Configuração `frontend/tsconfig.json` |
| Vitest / Testing Library | 13 testes em 3 arquivos; zero falhas | Precisão, HTTP/CSRF, erros, formulário, preferências e rotas |
| Build Vite | Aprovado | Build local e dentro da imagem Node 24; páginas carregadas sob demanda |
| Playwright funcional | 6 cenários aprovados | Navegador Chromium, proxy real, Java e PostgreSQL |
| Captura/revisão visual | 1 cenário aprovado, 7 PNGs | Dados persistidos, desktop/temas/mobile; sem erros de página/console, HTTP >=400 ou falhas inesperadas de rede no percurso final |
| Layout | Sem overflow horizontal observado | Dashboard preenchido em 320/390/768 pixels; estado vazio mobile; desktop inspecionado a 1600 |
| Compose | app/frontend/postgres saudáveis | Projeto isolado `ledgerflow-demo-v02`, volumes preservados após retomada |
| Reconciliação SQL | Zero divergências e zero transferências desequilibradas | 22 contas, 31 operações, 37 lançamentos no instante da consulta; banco PostgreSQL 16.15, migration 2 aplicada |
| npm audit | Zero achados | 374 dependências totais reportadas; 88 prod e 286 dev, incluindo sobreposições de opcionais/peers |
| OSV Maven | 141 dependências; 2 avisos revisados; zero novos pendentes | Árvore efetivamente resolvida, dependências de teste incluídas |
| Gitleaks 8.30.1 | Zero segredos detectados após exceção específica já existente | Candidato de arquivos publicáveis e histórico local; sem imprimir valores sensíveis |
| GitHub Actions v0.2.0 | Preparado, não executado remotamente | Aprovação de publicação ainda necessária; sucesso local não é apresentado como sucesso remoto |

Arquivos sanitizados: [Java](evidence/v0.2.0/java-tests.json), [Vitest](evidence/v0.2.0/frontend-tests.txt), [E2E](evidence/v0.2.0/browser-tests.txt), [captura final](evidence/v0.2.0/visual-tests.txt), [banco](evidence/v0.2.0/database.json), [npm](evidence/v0.2.0/npm-audit.json), [OSV](evidence/v0.2.0/osv-audit.json), [hashes das imagens](evidence/v0.2.0/screenshots.json).

Os logs brutos privados de execução ficam em `.tools/fullstack/`, ignorado por Git. Credenciais da demo ficam somente em `.local/demo/`. Os números de contas/operações são uma observação daquele instante, não valores fixos esperados para todas as instalações.

## Cenários e correções verificadas

1. Cadastro pela interface estabelece sessão; cookie HttpOnly/SameSite Strict; tema/idioma persistem; logout invalida e protege as rotas.
2. Criar conta, depositar, sacar, confirmar transferência, buscar e baixar CSV. O saldo final é verificado diretamente na API.
3. Cortar a resposta após o servidor efetivar uma transferência; consultar a chave original; confirmar um único débito e uma requisição financeira.
4. Outro usuário não lê a conta (404), mutação sem CSRF é negada (403), logout exige novo login.
5. Criar, editar e excluir meta pela UI; confirmar que progresso da meta não altera saldo. Desativar conta impede ações financeiras.
6. Navegação/estado vazio mobile; captura adicional com dados preenchidos e temas.

Os testes expuseram e ajudaram a corrigir persistência do contexto de sessão, alias SQL incompatível e atualização do cache de identidade após cadastro. Os testes E2E também receberam seletores coerentes com nomes acessíveis dos botões, e a captura após resize passou a aguardar estabilidade de largura. Falhas anteriores não são contadas como aprovação; os resultados acima correspondem às execuções corrigidas.

## Segurança e limites da auditoria

As regras financeiras permanecem no servidor: propriedade, estado da conta, valor, atomicidade e idempotência. Autorização explícita não herda cookie na cadeia stateless. Novos testes cobrem CSRF, rotação de sessão, logout, owner isolation, CSV, períodos e versões de metas. Rate limiting é básico por IP e compartilha o endereço do proxy local. Cadastro pode revelar e-mail já utilizado; não há fluxo de verificação de e-mail.

OSV mantém dois avisos em `spring-webmvc:6.2.19`: `GHSA-j9f9-w8pj-32f8` (CVE-2026-47890, SSE com fragmentos) e `GHSA-pc63-qcmh-9cmg` (CVE-2026-47884, XSLT/view implícita). As funcionalidades condicionantes não existem nesta aplicação REST. A análise original e a diferença entre severidade do agregador e do fornecedor estão no [relatório anterior](release-audit.md). O script exige revisão para qualquer aviso diferente e limita a exceção à versão exata; adicionar essas funcionalidades exige reavaliação.

Gitleaks mantém somente a exceção combinada por caminho e UUID sintético de subject em `JwtValidationTest`; não ignora diretórios inteiros de código ou todos os testes. A primeira execução sobre caminho absoluto não aplicou a regra ancorada; a execução relativa ao candidato confirmou o mesmo UUID de fixture, sem nova exceção. Configurações privadas não entram no candidato nem na imagem.

Não foi executado pentest, teste de carga, auditoria regulatória, scan completo do sistema operacional das imagens ou matriz de navegadores. Não há alegação de cobertura percentual total, certificação WCAG, disponibilidade de produção ou zero vulnerabilidades desconhecidas.

## Revisão visual

[Design e comparação](design-system.md) registram a relação com as duas referências. O dashboard preserva a hierarquia azul-marinho/azul, quatro indicadores, gráficos, metas, contas e ações. Tema claro e mobile usam os mesmos dados. Valores, gráficos e categorias refletem o seed real; não há percentuais de crescimento, notificações, cartões ou IA fictícios.

Sete telas foram abertas e inspecionadas: [escuro](assets/screenshots/dashboard-dark-desktop.png), [claro](assets/screenshots/dashboard-light-desktop.png), [mobile](assets/screenshots/dashboard-mobile.png), [contas](assets/screenshots/accounts.png), [transações](assets/screenshots/transactions.png), [transferência](assets/screenshots/transfer.png), [metas](assets/screenshots/goals.png). O resultado aproxima composição e identidade, sem afirmar equivalência pixel a pixel.

## Commits e revisão

| Commit | Mudança |
| --- | --- |
| `edae34e` | Sessões revogáveis, CSRF e isolamento bearer |
| `1d78975` | Analytics, extrato e metas versionadas |
| `8fd10c2` | Frontend bilíngue, UI integrada e testes de componente |
| `c38c70f` | Compose Full Stack e demo isolada |
| `87ecd24` | Jornadas E2E e captura reproduzível |
| `07c4c60` | CI e auditorias de dependências |

A documentação, evidências e imagens são registradas em commit posterior a esses commits de implementação. Consulte `git log main..HEAD --oneline` para a lista final e `git status --short` para o estado de trabalho. Não foram fabricadas datas ou alterada a release histórica.

## Reprodução e avaliação de prontidão

Use os comandos do [README](../README.md), incluindo `Start-FullStack.ps1` para retomar os serviços sem recriar a demo. URLs locais: frontend `http://localhost:18081`, API/Swagger `http://localhost:18080/swagger-ui.html`. Credenciais fictícias em `.local/demo/latest.json`. O projeto é adequado para revisão local e apresentação de portfólio; não está qualificado como serviço bancário de produção.

Pontos fortes: invariantes financeiras testadas com PostgreSQL; UX completa sobre dados reais; recuperação de resultado incerto; documentação que conecta código a decisões. Limitações principais: sessões em memória, proteção de abuso simples, planejamento manual de metas, contexto de tentativa perdido ao fechar a aba, apenas Chromium e sem operação pública.

Três evoluções de maior impacto: (1) identidade/sessões distribuídas e recuperação de acesso; (2) conciliação e observabilidade operacional; (3) acessibilidade e testes entre navegadores. A [trilha educacional](learning/fullstack/README.md) e o [guia de entrevistas](learning/fullstack/interview-guide.md) cobrem os conceitos necessários para defender o projeto.

**Pendências antes de publicar:** revisão do proprietário e autorização explícita para merge, push final, tag anotada v0.2.0 e GitHub Release. Depois da autorização: reconferir remoto, integrar sem force push, validar criticamente o commit integrado, acompanhar o CI e confirmar tag/release. [Release notes bilíngues preparadas](releases/v0.2.0.md). Nenhuma aprovação genérica de autonomia substitui essa etapa específica definida na missão.
