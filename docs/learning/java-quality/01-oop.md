# POO no LedgerFlow

**Problema.** O saldo de uma conta não deve ser alterado por qualquer código que tenha acesso à entidade. **Princípio.** Encapsulamento mantém estado e invariantes juntos. **Solução real.** `TransactionService` chama `Account.debit(amount)` e `Account.credit(amount)`; a entidade valida saldo e estado, em vez de expor um setter de saldo. Antes da refatoração desta etapa, o controller extraía `UUID.fromString(jwt.getSubject())`; agora recebe `@AuthenticatedUser UUID userId`. Isso também oculta um detalhe da representação da identidade.

**Trade-off.** Encapsular não exige criar uma classe para cada operação: o domínio da conta continua pequeno, enquanto a integração HTTP fica em `security/`.
