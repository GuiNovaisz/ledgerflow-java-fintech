# 2. API, banco e transações

## Spring Boot e REST

[LedgerFlowApplication](../../../src/main/java/com/ledgerflow/LedgerFlowApplication.java) inicia a aplicação. O Boot configura infraestrutura a partir de dependências e propriedades; Spring cria e conecta controllers, services e repositories. Anotações não substituem a necessidade de compreender o que cada camada faz.

Em [AccountController](../../../src/main/java/com/ledgerflow/account/api/AccountController.java), uma rota HTTP recebe uma requisição, valida o DTO e delega ao serviço. REST organiza recursos e operações usando métodos e status: GET consulta, POST cria, PATCH altera parcialmente. Um sucesso HTTP deve representar o resultado confirmado, não apenas o clique no botão. Erros são tratados por [ApiExceptionHandler](../../../src/main/java/com/ledgerflow/shared/ApiExceptionHandler.java) sem entregar stack traces ao cliente.

O dono vem da identidade autenticada, não de um campo `ownerId` livremente enviado pelo formulário. Uma URL com UUID válido não concede acesso. Respostas 404 para recursos de outro dono evitam revelar sua existência.

## JPA, SQL e PostgreSQL

JPA mapeia objetos para tabelas. [AccountRepository](../../../src/main/java/com/ledgerflow/account/persistence/AccountRepository.java) encapsula consultas e locks. Uma entidade gerenciada alterada dentro de uma transação pode ser sincronizada com o banco no commit; não é necessário um `UPDATE` escrito no controller. `open-in-view=false` evita depender de sessão de persistência aberta durante serialização da resposta.

JPA não elimina SQL. [InsightRepository](../../../src/main/java/com/ledgerflow/insights/InsightRepository.java) usa consultas agregadas para extrato, categorias e evolução diária. Fazer essa busca no servidor permite filtrar todo o histórico, não só os dez itens já carregados na tela. Parâmetros vinculados mantêm os valores separados do texto SQL. Um `%` digitado na busca é tratado como conteúdo literal, não como permissão para alterar a consulta.

PostgreSQL persiste dados, aplica restrições e coordena acesso concorrente. `NUMERIC(17,2)` oferece quinze dígitos inteiros e dois decimais. Flyway aplica migrations ordenadas; V1 contém o domínio original e V2 acrescenta metas. Não edite uma migration já aplicada para mudar o banco: crie uma próxima migration e teste a evolução.

## Atomicidade e concorrência

Transferir 50 exige debitar a origem, creditar o destino e registrar os lançamentos. Se o destino falhar, o débito não pode permanecer sozinho. `@Transactional` em `TransactionService.execute` define essa unidade de commit/rollback. O proxy Spring envolve a chamada; uma chamada interna ao mesmo objeto não atravessaria esse proxy do mesmo modo.

Atomicidade sozinha não resolve duas requisições lendo o mesmo saldo. O serviço bloqueia as linhas das contas em ordem de UUID; outro escritor aguarda. Usar sempre a mesma ordem reduz ciclos de espera em transferências opostas. Mudança de status usa os mesmos locks, para não competir de forma inconsistente com um débito.

Idempotência é outra regra: `(owner, key)` identifica a tentativa lógica. Um lock advisory transacional serializa requisições com essa chave. O fingerprint compara conteúdo: mesma chave e mesmo conteúdo devolvem o resultado anterior; mesma chave e conteúdo diferente gera conflito. A chave não substitui autenticação nem controle de saldo.

## Relatórios e metas

O intervalo `[from,to)` inclui o início e exclui o fim; isso permite períodos consecutivos sem contar a fronteira duas vezes. O projeto considera a precisão de microssegundos do PostgreSQL. Transferências entre contas próprias somam zero no patrimônio e são excluídas de renda/despesa, embora apareçam como dois lançamentos no extrato.

Uma meta é planejamento independente do ledger. [GoalService](../../../src/main/java/com/ledgerflow/goals/GoalService.java) compara a versão enviada com a persistida sob lock. Uma aba com versão antiga recebe conflito em vez de sobrescrever a outra. “R$ 500 acumulados” na meta não prova que esses R$ 500 estão reservados em uma conta.

Pratique explicando os casos de concorrência e rollback de [LedgerFlowIT](../../../src/test/java/com/ledgerflow/integration/LedgerFlowIT.java) antes de alterar o serviço financeiro.
