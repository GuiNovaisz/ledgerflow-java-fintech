# Principal autenticado no Spring Security

**Antes:** 19 métodos em seis controllers recebiam `@AuthenticationPrincipal Jwt` e repetiam `UUID.fromString(jwt.getSubject())`. Uma mudança na representação do sujeito exigiria editar os seis arquivos.

**Depois:** métodos recebem `@AuthenticatedUser UUID userId`. `AuthenticatedUserResolver` lê o `Jwt` autenticado do `SecurityContext`, converte o sujeito uma vez e rejeita principal ausente ou inesperado. `JwtConfiguration` continua validando assinatura, emissor, audiência, prazo e UUID canônico antes da resolução; o resolver não substitui essa validação.

**Trade-off:** há uma anotação e um resolver específicos do projeto. Em troca, controllers desconhecem o parsing e usam um tipo seguro. A sessão do navegador e o bearer continuam cobertos por testes de integração.
