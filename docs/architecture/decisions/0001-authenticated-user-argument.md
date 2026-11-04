# ADR 0001 — UUID autenticado nos controllers

**Estado:** aceito. **Contexto:** 19 métodos em seis controllers repetiam `UUID.fromString(jwt.getSubject())`. O decodificador já valida assinatura, claims e UUID canônico para bearer e sessão.

**Decisão:** usar `@AuthenticatedUser UUID` com `HandlerMethodArgumentResolver` registrado no Spring MVC. O resolver somente traduz o principal autenticado; não concede acesso nem substitui as checagens de owner.

**Alternativas:** um principal customizado exigiria adaptar também o fluxo de sessão e resource server; `@AuthenticationPrincipal` direto manteria o parsing distribuído; um serviço chamado manualmente repetiria dependências nos controllers.

**Consequência:** um ponto de conversão e controllers menos acoplados ao JWT, ao custo de uma anotação e configuração MVC. A especificação OpenAPI oculta esse parâmetro interno.
