# Auditoria final / Final normalization — 2026-11-19

## Genealogia

Estado inicial remoto: `main=f9d5a745333bd84e9b3e99e732ff9ab87cd91e5a`, `dev=3acff66576d99c99c9f8e34ee9f91b3c610ff550`. Base comum: `95dfbe7a98239690bb44e87399c2fcfc1772ce73`. A divergência era 1 commit exclusivo de main (merge do PR #1) e 3 de dev (`a5aa54a`, `665ae15`, `3acff66`, trabalho e merge do PR #2). Não eram implementações concorrentes: main recebeu a etapa anterior, enquanto dev avançou com o Flow Summary. `codex/insight-flow-summary` já era ancestral de dev. As outras duas branches auxiliares não tinham commits exclusivos em relação a main.

O merge `66c8c4a` incorpora a história de main em DEV sem conflitos, rebase, cherry-pick, perda de commits ou alteração de tags. A promoção usa PR dev → main com merge commit e CI verde; depois dev pode avançar para o mesmo commit por fast-forward.

## Revisão técnica

Flow Summary aprovado: o calculador é determinístico, usa BigDecimal e não depende de banco, HTTP ou apresentação. O record copia o mapa e o expõe como não modificável; valores BigDecimal são imutáveis. A consulta PostgreSQL agrupa direção/categoria e exclui transferências entre contas do mesmo dono; o calculador também soma categorias repetidas. Não há arredondamento prematuro. O DTO preserva strings monetárias de duas casas, endpoints e autorização.

`FlowAmount` é interno ao pacote, produzido pelo repository com campos obrigatórios do esquema SQL (incluindo direção restrita a CREDIT/DEBIT). Null não faz parte do contrato de entrada. Optional: caso o calculador passe a receber dados externos, adicionar validação explícita de null e montantes negativos nessa fronteira. A service retém a montagem da série diária e coordena políticas/consultas; não se justificou outra abstração nesta manutenção. Não houve correção de código de produto.

## Regressão e segurança

- JDK 21, Maven wrapper: `clean verify -Pintegration` e inventário de dependências; 38 testes unitários e 23 PostgreSQL/Testcontainers aprovados.
- Frontend: `npm ci`, lint, typecheck, 13 testes em 3 arquivos e build aprovados. npm audit: 9 moderados, 0 altos, 0 críticos; os avisos são da cadeia ESLint/typescript-eslint/minimatch de desenvolvimento. Important: atualizar essa cadeia em manutenção própria; o gate existente `--audit-level=high` passa.
- Docker: stack reconstruído com volumes preservados e três serviços saudáveis. Playwright Chromium headless: 6 E2E aprovados, incluindo dinheiro, recuperação após timeout, CSRF, sessão e mobile.
- OSV: 141 dependências, 2 avisos já avaliados pela política existente, 0 novas revisões exigidas.
- Gitleaks com redação: histórico de todas as referências examinado, 0 segredos encontrados. Arquivos `.local`, `.env`, logs e ferramentas privadas permanecem ignorados e excluídos do contexto Docker; somente `.env.example` é versionado.
- Critical: nenhum achado identificado nesta auditoria. Important: alertas Dependabot estão desabilitados; a consulta retornou 403 informando isso. A auditoria não habilitou integrações ou alterou permissões.
- O ruleset `mainRule` ativo proíbe deleção e non-fast-forward, sem bypass. Branches residuais só podem ser removidas se o GitHub permitir e após comprovar zero commits exclusivos; não enfraquecer o ruleset para limpá-las.

## English summary

The initial divergence was one main-only merge commit versus three dev-only commits containing the approved Flow Summary and its documentation. The common ancestor was `95dfbe7`. Merging main into dev preserves both histories without rewriting public commits. The calculator improves testability, retains BigDecimal precision and immutable category results, and preserves API behavior. No product code changes were necessary. Existing v0.1.0 and v0.2.0 tags/releases remain unchanged; this maintenance does not introduce a patch release.

Regression passed 38 Java unit tests, 23 PostgreSQL integration tests, 13 frontend tests, lint, typecheck and build. OSV required no new advisory review, and the full-history secret scan found no leaks. Nine moderate npm findings affect development tooling; Dependabot alerts are disabled. These remain documented follow-up items rather than being hidden by this normalization.
