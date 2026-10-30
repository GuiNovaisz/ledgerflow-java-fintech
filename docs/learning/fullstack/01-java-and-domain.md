# 1. Java e POO aplicados

## Do algoritmo ao objeto

Em um exercício inicial, você pode ter `saldo`, `valor` e um `if` que impede um saque maior que o saldo. Em uma aplicação, muitas requisições podem tentar alterar a mesma conta, e o saldo não pode desaparecer quando o programa termina. A lógica precisa ficar organizada, persistida e coordenada.

Veja [Account.java](../../../src/main/java/com/ledgerflow/account/domain/Account.java). Uma **classe** define estrutura e comportamento; um **objeto** representa uma conta concreta. O identificador distingue contas mesmo quando possuem o mesmo nome. Campos privados e métodos como `debit`, `credit` e `ensureActive` mantêm a regra junto ao dado. Isso é encapsulamento: quem usa a conta não deve poder trocar o saldo arbitrariamente.

Um método é uma operação com entrada, resultado e possíveis falhas. `debit` verifica condições antes de alterar o estado. Uma exceção interrompe o caminho normal quando uma regra é violada. Ela não equivale automaticamente a um HTTP 400; essa tradução acontece na camada de API.

## Tipos e precisão

[Money.java](../../../src/main/java/com/ledgerflow/shared/Money.java) centraliza validações de valor e escala. `BigDecimal` representa decimais exatos quando construído de texto. `new BigDecimal("0.10")` expressa dez centavos; converter primeiro para `double` introduz uma representação binária desnecessária. O sistema rejeita frações de centavo em vez de arredondar silenciosamente.

`UUID` identifica entidades; não é uma senha nem autorização. `Category` é um enum: restringe valores a um conjunto conhecido. Traduzir “Alimentação” na tela não altera o código `FOOD` enviado pela API. DTOs transportam os campos do contrato, enquanto entidades modelam persistência e comportamento.

## Composição e dependências

[TransactionService](../../../src/main/java/com/ledgerflow/ledger/application/TransactionService.java) recebe repositórios pelo construtor. Ele **usa** esses colaboradores, em vez de herdar de uma classe “BancoUniversal”. Composição permite combinar responsabilidades pequenas. Injeção de dependência significa que o ambiente fornece os colaboradores; não é um mecanismo que executa sozinho as regras do negócio.

O serviço coordena a operação; a conta defende suas invariantes; o repositório acessa dados. Separar essas responsabilidades permite testar um débito sem iniciar o servidor inteiro. A camada HTTP não deve conter uma segunda implementação das regras financeiras.

## Leitura ativa

No método `execute`, identifique uma coleção, um laço, uma condição, um retorno antecipado e uma exceção. Explique por que os UUIDs das contas são ordenados antes dos locks. Compare o teste [AccountTest](../../../src/test/java/com/ledgerflow/account/AccountTest.java) com a classe de domínio: qual comportamento seria impossível verificar apenas olhando um screenshot?
