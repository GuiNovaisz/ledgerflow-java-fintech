# 4. React e TypeScript sem atalhos conceituais

## Componentes e props

React descreve a tela como uma função do estado. Um componente combina estrutura e comportamento de uma parte da interface. Em [common.tsx](../../../frontend/src/components/common.tsx), `Panel`, `Field`, `Loading` e `Empty` evitam repetir o mesmo padrão em muitas páginas. **Props** são entradas: título, conteúdo, valor ou callback. Um componente filho não deve alterar uma prop recebida.

[Button](../../../frontend/src/components/ui/button.tsx) oferece variantes usando CVA e Radix Slot. [Dialog](../../../frontend/src/components/ui/dialog.tsx) usa Radix para a base de foco e interação. São componentes mantidos no repositório no padrão shadcn, não um serviço externo que renderiza a interface.

TypeScript verifica combinações permitidas em tempo de desenvolvimento. `type Account` documenta a forma esperada de uma conta. Ele não inspeciona magicamente JSON remoto em tempo de execução, nem valida requisições feitas por um atacante. Zod e as validações do servidor cumprem papéis diferentes.

## State e renderização

Em [operation.tsx](../../../frontend/src/components/operation.tsx), `step` escolhe a etapa do modal; `busy` representa processamento; `uncertain` representa um resultado ainda desconhecido. `useState` mantém esses valores entre renderizações. Chamar um setter solicita outra renderização; não transforma imediatamente a variável local atual.

Props fluem para baixo; callbacks comunicam eventos para cima. `onClose` permite que o modal peça ao componente pai que o remova. Uma variável comum criada dentro da função não persiste entre renderizações; `useRef` persiste sem provocar renderização. O modal usa uma ref para impedir duas submissões no mesmo intervalo antes da atualização visual do botão.

## Hooks e efeitos

Hooks devem manter a mesma ordem em cada renderização; por isso não devem ser chamados dentro de condições variáveis. `useEffect` sincroniza com algo externo, como eventos de sistema ou listeners. Em [state.tsx](../../../frontend/src/state.tsx), a preferência de tema observa `matchMedia`; o retorno do efeito remove o listener. Sem limpeza, remontagens poderiam acumular callbacks.

O efeito não deve ser o lugar padrão para calcular um texto derivado de uma prop; esse cálculo pode acontecer durante renderização. Estado do servidor é gerenciado por TanStack Query, não por uma coleção de efeitos que fazem fetch sem cache e sem controle de corrida.

## Rotas, apresentação e acessibilidade

[App.tsx](../../../frontend/src/App.tsx) carrega páginas por rota e protege as rotas autenticadas. A proteção melhora UX, mas a API continua sendo a autoridade. `React.lazy` separa código das páginas; isso não significa que os dados sejam buscados automaticamente.

Traduções de [i18n.ts](../../../frontend/src/i18n.ts) separam texto de apresentação do identificador de categoria. O tema altera variáveis CSS; não exige duas implementações do formulário. Labels, foco visível, botões semânticos e mensagens de erro importam porque a interface deve ser operável além do mouse.

Veja `dashboard.tsx`: `data.balanceSeries` alimenta o gráfico real. Dados ausentes produzem estado vazio; não devem ser substituídos por uma curva inventada. Valores financeiros usam os utilitários de `money.ts`; gráficos podem converter valores para números para desenho, mas não para debitar dinheiro.

Pratique seguindo uma prop de `PageTitle` e um evento de `PeriodPicker` até a nova query. Explique por que alterar um objeto em memória não é suficiente para persistir uma meta no PostgreSQL.
