# Interface e revisão visual / v0.2.0

## Referências e decisões

A referência principal fornecida pelo proprietário mostra um painel LedgerFlow azul-marinho com quatro indicadores, gráfico de saldo, despesas por categoria, metas, contas, transações e ações rápidas. A referência secundária usa preto e violeta, maior respiro e cartões arredondados. A implementação prioriza a primeira estrutura e usa espaçamento e superfícies discretas para evitar densidade excessiva.

As referências são inspiração visual fornecida pelo usuário; os screenshots versionados são capturas da aplicação implementada, não montagens ou reutilização do mockup.

| Elemento | Implementação real e diferença deliberada |
| --- | --- |
| Navegação lateral | Grupos de navegação, ferramentas e perfil; pode recolher. No mobile vira menu e barra inferior. |
| Cabeçalho | Identidade real, busca no extrato, tema e idioma; sem notificações fictícias. |
| Quatro indicadores | Saldo, entradas, saídas e operações calculados na API; sem percentuais de crescimento inventados. |
| Evolução do saldo | Área azul baseada em lançamentos diários; curva do seed difere do mockup porque retrata dados persistidos. |
| Categorias | Donut, legenda e percentuais reais; categorias fixas do domínio. |
| Metas | Barras e CRUD completo; aviso de planejamento manual sem reserva de dinheiro. |
| Contas e histórico | Saldo/status reais e lançamentos consultáveis; sem atribuir bancos ou investimentos externos inexistentes. |
| Transferência | Modal de três etapas com validação, confirmação e resultado/recuperação. |
| Tema claro | Mesma hierarquia, branco/azul claro e texto escuro; nenhuma página duplicada. |
| Mobile | Saldo em destaque, três indicadores compactos, cartões empilhados, navegação inferior. |

Não se declara reprodução pixel a pixel. Tipografia usa fontes do sistema para evitar dependência externa; valores, datas, distribuição de categorias e textos refletem o produto implementado. O segundo mockup não justifica adicionar cartões VISA, IA ou ações sem backend.

## Tokens e componentes

Fonte de verdade: [styles.css](../frontend/src/styles.css). Tema escuro: fundo `#07121f`, superfícies azul-marinho e acento `#3294ff`; o tema claro usa superfícies claras. Verde indica crédito, vermelho débito e azul ações, sempre acompanhados de texto/sinais. Bordas discretas, cantos arredondados, espaçamento consistente e estados de foco compõem a hierarquia.

Primitivas próprias no padrão shadcn/Radix: `Button` e `Dialog`. Componentes compartilhados: `Field`, `Panel`, `PageTitle`, `Loading`, `Empty`, `ErrorMessage`, `EntryList` e `PeriodPicker`. Tailwind está integrado ao build; os tokens e layouts principais são CSS mantido no projeto, sem alegar geração oficial por CLI shadcn.

Formulários possuem labels e mensagens traduzidas. Dialog usa gerenciamento de foco Radix, Escape e bloqueio de fechamento durante processamento/incerteza. Há link para pular ao conteúdo, foco visível, estados de carregamento/vazio/erro e preferência de movimento reduzido. A revisão não equivale a certificação WCAG nem a testes com todas as tecnologias assistivas.

## Capturas verificadas

Captura por [portfolio.spec.ts](../frontend/e2e/portfolio.spec.ts), Chromium headless, dados fictícios persistidos. Desktop: viewport 1600×1000 com captura da página; mobile: 390×844 do viewport; modal: viewport para preservar o enquadramento do overlay. A execução também verifica ausência de overflow horizontal em 320, 390 e 768 pixels. O desktop foi inspecionado visualmente; não há benchmark de similaridade por porcentagem.

- [Dashboard escuro](assets/screenshots/dashboard-dark-desktop.png)
- [Dashboard claro](assets/screenshots/dashboard-light-desktop.png)
- [Dashboard mobile](assets/screenshots/dashboard-mobile.png)
- [Contas](assets/screenshots/accounts.png)
- [Transações](assets/screenshots/transactions.png)
- [Transferência](assets/screenshots/transfer.png)
- [Metas](assets/screenshots/goals.png)

A revisão corrigiu a captura feita durante a adaptação dos gráficos após resize: o teste agora aguarda uma condição de layout estável, sem sleeps fixos nem ocultação artificial do overflow. O cadastro também recebeu correção de cache para a identidade autenticada aparecer imediatamente. Os botões rápidos receberam nomes acessíveis explícitos.

## Demonstração / Demo

**PT-BR:** o seed cria uma pessoa fictícia e três contas novas. O modo histórico é explícito, restrito a loopback e a um projeto Compose `ledgerflow-demo[-nome]`; insere somente fixtures novas em contas vazias e reconcilia seus saldos na mesma transação. Ele não retrodata ou edita operações existentes. O modo padrão utiliza apenas a API e concentra os eventos no dia atual. Credenciais aleatórias são salvas em `.local/demo/`, ignorado por Git. Nunca use esse seed como fluxo financeiro de produção.

**EN:** the seed creates a synthetic identity and three new accounts. Historical mode is explicit and restricted to loopback and a `ledgerflow-demo[-name]` Compose project; it inserts new fixtures into empty accounts and reconciles their balances in one transaction. It does not alter existing operations. Default mode uses only the API and creates today's events. Random credentials stay in ignored `.local/demo/`. This fixture loader is not a production financial workflow.
