# 3. Segurança, autenticação e navegador

## Identidade e autorização

Autenticação responde quem está fazendo a requisição. Autorização verifica se essa identidade pode agir sobre aquela conta. Uma interface pode esconder um botão, mas um cliente externo ainda pode enviar HTTP; por isso as verificações definitivas estão no backend.

[AuthService](../../../src/main/java/com/ledgerflow/auth/AuthService.java) verifica senhas com BCrypt. Um hash de senha não deve ser descriptografado: a verificação compara o resultado apropriado, incluindo salt e custo. Segredos de infraestrutura vêm do ambiente, não de arquivos versionados. O rate limiter é uma barreira básica por IP, não proteção completa contra abuso.

## Sessão e cookie

[SessionController](../../../src/main/java/com/ledgerflow/auth/SessionController.java) autentica o navegador e salva um contexto na sessão do servidor. O cookie carrega um identificador opaco; os dados da identidade ficam no servidor. `HttpOnly` impede leitura pelo JavaScript, `SameSite=Strict` restringe envio entre sites e `Secure` exige HTTPS quando ativado. Nenhuma dessas opções elimina sozinha o impacto de XSS.

Logout invalida a sessão no servidor. Remover apenas um item do localStorage não invalidaria uma credencial roubada. O projeto usa sessões em memória: reiniciar o backend desconecta usuários e múltiplas instâncias precisariam de armazenamento compartilhado ou outra arquitetura.

O contexto persistido usa uma autenticação de sessão cujo principal é adaptado para os controllers existentes. Não confunda esse objeto interno com um JWT assinado entregue ao navegador. O frontend não recebe um bearer token no login de sessão.

## JWT para clientes de API

As rotas antigas `/auth/login` continuam emitindo JWT para ferramentas e outros clientes. A assinatura protege integridade; não criptografa automaticamente o conteúdo. O servidor valida assinatura, emissor e expiração. Esse fluxo é stateless e não tem revogação imediata individual; a sessão do navegador, por outro lado, pode ser invalidada no logout.

[SecurityConfiguration](../../../src/main/java/com/ledgerflow/security/SecurityConfiguration.java) separa as cadeias. Requisições com Authorization passam pela cadeia stateless e não podem aproveitar um cookie válido para escapar da proteção CSRF. Veja os testes de header falso em [SessionSecurityIT](../../../src/test/java/com/ledgerflow/integration/SessionSecurityIT.java).

## CSRF e CORS são diferentes

O navegador envia cookies automaticamente. Um site externo poderia tentar fazer o navegador enviar uma mutação usando esses cookies: esse é o cenário de CSRF. A aplicação exige um token vinculado à sessão, obtido por GET e enviado num header nas mutações. O token fica em memória no cliente; não é uma senha ou JWT. Após login o frontend obtém um novo token para a sessão autenticada.

CORS trata permissões de leitura entre origens no navegador; não autentica clientes e não bloqueia todas as requisições externas. Aqui Vite/Nginx encaminham `/api`, fazendo UI e API aparecerem sob a mesma origem. Não há necessidade de `Access-Control-Allow-Origin: *` com credenciais.

## Fronteiras e limites

[api.ts](../../../frontend/src/lib/api.ts) manda cookies same-origin, aplica timeout e mostra códigos de erro traduzidos sem HTML arbitrário. Uma resposta 401 limpa o cache privado. Não repete mutações financeiras automaticamente. Uma rede indisponível não comprova rejeição de uma transferência.

CSV pode ser perigoso mesmo sendo texto: células iniciadas por fórmulas são interpretadas por planilhas. A exportação neutraliza texto de usuário. Limites de período, paginação e exportação também evitam consultas sem tamanho controlado.

Exercício de explicação: descreva o que ainda falta para produção — HTTPS operacional, gerenciamento de segredos, identidade mais forte, sessões distribuídas, observabilidade e resposta a incidentes — sem alegar que um teste automatizado certifica segurança completa.
