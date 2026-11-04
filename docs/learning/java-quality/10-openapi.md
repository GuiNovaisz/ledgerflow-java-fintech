# OpenAPI como contrato verificável

**Problema:** a segurança global documentava bearer e cookie também para `/api/v1/session/login`, embora login seja público. **Depois:** `OpenApiConfiguration` marca login, registro e CSRF públicos; rotas de sessão protegidas exigem cookie; as demais continuam com bearer ou cookie. O customizer adiciona respostas de erro e o cabeçalho CSRF às mutações aplicáveis sem espalhar anotações por todos os controllers.

O teste `FullStackReportingIT.openApiShowsPublicLoginAndBothProtectedAuthenticationModes` lê `/v3/api-docs` e verifica rotas e esquemas reais. **Trade-off:** a customização central precisa acompanhar mudanças de padrão de rota; por isso o teste exercita a especificação gerada. Documentação não substitui testes de autorização.
