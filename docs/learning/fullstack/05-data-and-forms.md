# 5. Dados remotos, formulários e falhas

## TanStack Query

O banco é a fonte da verdade. A interface guarda uma cópia temporária dos resultados para evitar requisições desnecessárias. `useQuery` administra carregamento, erro, cache e atualização. Em [transactions.tsx](../../../frontend/src/pages/transactions.tsx), os filtros participam da chave: duas páginas ou períodos diferentes não devem compartilhar indevidamente o mesmo resultado.

`staleTime` indica por quanto tempo um dado é considerado recente; não é prazo de expiração da sessão. `invalidateQueries` sinaliza que a cópia pode estar desatualizada depois de uma mutação. Após uma transferência, saldos, extrato e dashboard precisam refletir a operação persistida. Uma atualização otimista financeira exigiria rollback e reconciliação cuidadosos; esta aplicação aguarda confirmação do servidor.

O cache da sessão é preservado ao estabelecer a identidade, enquanto os dados privados anteriores são removidos. Apagar a query ativa da sessão e criar outra com a mesma chave pode deixar o observador ligado ao objeto anterior; o teste E2E de cadastro detectou esse problema. Cache também é parte da correção funcional, não apenas desempenho.

## React Hook Form e Zod

[auth.tsx](../../../frontend/src/pages/auth.tsx) conecta inputs a React Hook Form. A biblioteca acompanha valores, erros e envio. Zod descreve restrições e o resolver as traduz para o formulário. Uma validação local oferece retorno rápido, mas a API valida novamente porque qualquer cliente pode ignorar JavaScript.

A senha é medida em bytes UTF-8, respeitando o limite aceito pelo BCrypt. O valor financeiro é texto; `inputMoney` valida casas decimais antes de converter para centavos. Não use `parseFloat` e arredondamento indiscriminado para aceitar uma quantia com precisão inválida.

`useWatch` acompanha o destino da transferência para exibir o campo de UUID externo quando necessário. As regras verificam origem, destino diferente, valor positivo e descrição limitada. O passo de confirmação apresenta o snapshot que será enviado; editar os dados cria outra tentativa lógica antes do envio.

## Quando a rede mente por omissão

Considere: o servidor efetiva a transferência, mas a resposta se perde. O cliente vê timeout. Repetir com uma nova chave cobraria duas vezes. O modal mantém o mesmo snapshot e a mesma chave, oferece consulta em `/operations/by-key/{key}` e permite repetir exatamente a tentativa original.

Uma consulta 404 durante a incerteza não comprova falha definitiva: a primeira transação pode estar em andamento. Por isso o modal não anuncia “cancelada”. [O cenário E2E](../../../frontend/e2e/fullstack.spec.ts) deixa o servidor concluir a transferência, corta a resposta e recupera o resultado, verificando que o saldo foi debitado uma vez.

O contexto incerto fica em memória na aba e dispara aviso ao sair. Ele não é um histórico durável de tentativas. Após fechar/recarregar, consulte o extrato antes de iniciar outra transferência; uma futura evolução poderia criar recuperação persistente com escopo de usuário e revisão de privacidade.

## Conflitos de edição

O formulário de meta envia a versão conhecida. Se outro cliente alterou o registro, o servidor rejeita a versão antiga. O usuário deve fechar o formulário, atualizar os dados e revisar novamente; reenviar cegamente ocultaria o conflito. Esse problema difere de idempotência: uma coisa é repetir a mesma intenção; outra é tentar sobrescrever informação mais recente.

Pratique descrevendo os estados carregando, vazio, sucesso, erro validado e resultado incerto. Qual deles permite afirmar que uma operação financeira não aconteceu?
