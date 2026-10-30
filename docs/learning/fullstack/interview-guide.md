# Guia de entrevista — respostas e demonstrações

Use as respostas como critérios de entendimento, não como texto a decorar. Aponte o arquivo e execute um exemplo quando possível. Seja transparente sobre o uso de assistência na implementação e sobre o que você consegue modificar sozinho.

| Pergunta | Elementos esperados na resposta |
| --- | --- |
| Por que Java? | Tipagem estática, modelagem explícita, `BigDecimal`, ecossistema de testes; não alegar que outras linguagens não suportam consistência. |
| Por que Spring Boot? | Configuração e integração de HTTP, segurança, validação, transações e persistência; entender os proxies e não apenas as anotações. |
| Por que PostgreSQL? | Restrições, decimais exatos, transações, locks e SQL agregado; o projeto realmente testa sua concorrência no PostgreSQL. |
| Por que React? | Composição de páginas e estados interativos; componentes reutilizáveis e integração com uma API existente. |
| Por que TypeScript? | Verificar contratos de uso em desenvolvimento; reconhecer que o tipo não valida JSON em runtime. |
| Por que Vite? | Servidor de desenvolvimento, proxy same-origin e build de assets; não é o servidor Java nem banco. |
| Como funciona autenticação? | Login verifica BCrypt, cria contexto de sessão, cookie HttpOnly identifica a sessão; GET `/session/me` recupera perfil; logout invalida servidor. |
| Onde ficam os tokens? | Cookie de sessão gerenciado pelo navegador; CSRF em memória; localStorage só contém preferências. JWT existe no fluxo separado de clientes de API. |
| O que são CSRF e CORS? | CSRF combate mutação não desejada com cookies automáticos; CORS controla acesso entre origens no navegador. Proxy elimina a necessidade de CORS aberto. |
| Como uma transferência é atômica? | Débito, crédito, operação e lançamentos na mesma transação; falhas propagadas causam rollback. |
| Como funciona idempotência? | Chave por dono + fingerprint + lock transacional; mesma intenção retorna resultado, conteúdo diferente conflita. |
| Como evita saldo incorreto? | Precisão decimal, validações de domínio, locks coordenados e commit único; explicar concorrência além de `@Transactional`. |
| Timeout significa falha? | Não. O commit pode ter ocorrido; consultar pela mesma chave ou repetir a mesma tentativa, sem gerar chave nova. |
| Como React conversa com Spring? | JSON via `/api`, proxy same-origin, cookies e CSRF; servidor valida identidade e dados. |
| Por que TanStack Query, não Redux? | O maior estado compartilhado é remoto; cache, invalidação e estados de fetch atendem esse problema. Estado local/contexto basta para tema e modal. Redux não é inadequado universalmente. |
| O que acontece ao editar meta em duas abas? | Versionamento rejeita gravação antiga; revisar os dados atuais, não sobrescrever silenciosamente. |
| Como Docker funciona aqui? | Build em estágios, três serviços, rede interna, volume do banco, bind loopback e healthchecks. |
| Como são organizados os testes? | Unidade, PostgreSQL com Testcontainers, componentes e jornadas Chromium; cada camada oferece evidência diferente. |
| Por que o dashboard não conta transferência própria como entrada? | Não altera patrimônio do dono; extrato ainda mostra débito e crédito. |
| Isso é um banco pronto para produção? | Não. É simulação de ledger interno, sem infraestrutura regulatória, liquidação, conciliação externa e operação de produção. |
| O que mudaria primeiro? | Identidade/sessões robustas, reconciliação/observabilidade e acessibilidade/múltiplos navegadores; priorizar conforme o uso real. |

## Ensaio prático

1. Em cinco minutos, faça um depósito, uma transferência e uma busca no extrato. Explique como os três números do dashboard mudam.
2. Desenhe o caminho entre o botão Confirmar e o commit. Marque onde identidade, saldo e repetição são verificados.
3. Explique o teste que corta a resposta após o commit. Mostre por que um simples mock de sucesso não demonstraria a mesma propriedade.
4. Escolha uma restrição atual, proponha uma mudança e descreva migration, API, UI e testes necessários antes de programar.
5. Responda honestamente: quais partes você já consegue implementar sem assistência? Use os exercícios para reduzir essa diferença.
