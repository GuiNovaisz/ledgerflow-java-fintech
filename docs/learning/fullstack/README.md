# Trilha Full Stack do LedgerFlow

Este material parte de quem já resolve exercícios de variáveis, condições e laços, mas ainda está aprendendo POO. Não é necessário dominar tudo de uma vez. Leia um módulo, localize o código citado, faça uma alteração pequena em uma branch de estudo e execute os testes relacionados.

| Ordem | Material | Conceitos cobertos |
| --- | --- | --- |
| 1 | [Java e objetos](01-java-and-domain.md) | 1. Java aplicado; 2. POO |
| 2 | [API e persistência](02-api-and-data.md) | 3. Spring Boot; 4. REST; 5. JPA; 6. PostgreSQL; 7. transações |
| 3 | [Segurança e identidade](03-security.md) | 8. segurança; 18. autenticação; 19. cookies/JWT; 20. CORS/CSRF |
| 4 | [Interface](04-react-typescript.md) | 9. React; 10. TypeScript; 11. componentes; 12. props; 13. state; 14. hooks |
| 5 | [Dados e formulários](05-data-and-forms.md) | 15. TanStack Query; 16. formulários; 17. Zod |
| 6 | [Entrega e arquitetura](06-delivery.md) | 21. Docker; 22. testes; 23. CI/CD; 24. arquitetura Full Stack |

Depois, use o [guia de entrevista](interview-guide.md) e implemente os [exercícios sem solução](exercises.md). O backend anterior também possui uma [introdução de Java](../01-java-fundamentals.md).

## Roteiro de exploração

1. Execute a aplicação e crie uma conta fictícia. Observe a requisição no painel Network do navegador, sem copiar cookies para documentos públicos.
2. Abra `AccountController`, siga para `AccountService` e depois para `AccountRepository`. Explique cada chamada com suas próprias palavras.
3. Faça um depósito e encontre a operação e seu lançamento. Diferencie saldo atual de histórico.
4. Abra o modal de transferência, siga `prepare`, `execute` e a consulta após falha. Por que um timeout não prova que houve rollback?
5. Execute um teste isolado, altere uma regra em uma branch descartável e confirme que o teste detecta a quebra. Restaure a regra depois.

Não memorize uma resposta sem conseguir localizar a evidência. Reconhecer uma limitação, como sessão em memória ou metas manuais, demonstra mais entendimento que alegar prontidão bancária inexistente.
