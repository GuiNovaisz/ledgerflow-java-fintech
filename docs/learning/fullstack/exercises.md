# Exercícios sem solução pronta

Crie uma branch de estudo. Não use dados pessoais nem altere a release histórica. Para cada exercício, registre hipótese, arquivos alterados, verificação e limitações. Os critérios abaixo descrevem resultados, não a implementação completa.

## Fácil

1. **Melhore um estado vazio.** Altere o texto da página de metas em PT-BR e EN e acrescente uma ação útil. Verifique teclado, celular e conta sem metas.
2. **Validação de nome.** Rejeite nomes de conta contendo somente espaços com uma mensagem clara. Confirme a regra existente no servidor, sem enfraquecê-la. Teste envio pelo navegador e HTTP direto.
3. **Novo período.** Adicione a opção “Últimos 14 dias” ao seletor, preservando o contrato UTC. Verifique a duração exata e as traduções.
4. **Preferência visual.** Ajuste um token de espaçamento do tema e compare desktop/mobile. Não duplique páginas para os dois temas.

## Intermediário

1. **Apelido de conta.** Proponha um campo opcional, desenhe uma migration nova, atualize DTO/serviço/formulário e teste owner isolation. Defina como nomes antigos serão apresentados.
2. **Visão de categorias.** Crie uma página de consulta com totais por categoria para um período selecionado. Reutilize contratos existentes quando suficientes; diferencie zero de falha ao carregar.
3. **Recuperação de conflito.** Ofereça uma ação explícita para recarregar uma meta após conflito de versão. Não descarte silenciosamente o que foi digitado; teste duas abas com versões diferentes.
4. **Filtro compartilhável.** Faça a URL preservar todos os filtros do extrato e a página atual. Teste atualizar, voltar no histórico e abrir a URL em outra aba autenticada.

## Difícil

1. **Sessões distribuídas.** Desenhe uma evolução para duas instâncias do backend. Demonstre login/logout, expiração e CSRF entre instâncias; documente armazenamento, falhas e custo operacional.
2. **Conciliação interna.** Crie uma consulta somente leitura que compare saldo com lançamentos por conta. Teste conjunto consistente e inconsistência sintética isolada sem adulterar dados da demonstração.
3. **Concorrência reproduzível.** Em ambiente descartável, investigue dois débitos simultâneos que ultrapassariam o saldo se ambos usassem uma leitura antiga. Mostre um teste que falha sem coordenação e passa com a correção.
4. **Recuperação durável de tentativa.** Proponha como recuperar uma transferência incerta após fechar a aba. Considere privacidade entre usuários no mesmo navegador, chave original, consulta 404 durante processamento e expiração dos registros locais.
5. **Nova regra financeira.** Modele um estorno por operação compensatória, sem editar lançamentos anteriores. Defina autorização, limite, idempotência, concorrência, saldo e apresentação no extrato antes de implementar.

## Entrega de cada exercício

Inclua um commit claro, explicação da regra, demonstração e teste que detecte um erro plausível. Não crie testes que apenas repitam a implementação. Para alterações de baixo impacto visual, uma revisão direcionada pode bastar; regras financeiras exigem evidência de integração.
