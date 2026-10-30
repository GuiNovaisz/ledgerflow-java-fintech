# Fluxo de desenvolvimento / Development workflow

## Português (Brasil)

`DEV` é a branch de desenvolvimento contínuo. `main` representa a versão estável integrada; tags anotadas como `v0.2.0` identificam releases imutáveis. A branch principal do GitHub continua sendo `main`, para apresentar a versão estável do portfólio.

Antes de começar, consulte `git status` e atualize `DEV` com `git pull --ff-only`. Não substitua mudanças locais. Para tarefas isoladas, crie `codex/<tarefa>` a partir de `DEV` e integre por revisão/PR. Faça commits claros e execute testes proporcionais ao risco.

Uma release segue: autorização do proprietário → CI em `DEV` → verificação de mudanças remotas → integração segura em `main` → CI do commit integrado → tag anotada → GitHub Release → confirmação remota. Não use force push, não mova tags publicadas e não faça deploy público por consequência de um push.

Após publicar, sincronize `DEV` sem perder commits e deixe-a ativa. O fluxo é intencionalmente simples para este projeto: não exige branches permanentes adicionais ou aprovação externa fictícia. [Comandos de execução e testes](README.md#-português-brasil).

## English

`DEV` is the ongoing development branch. `main` contains the integrated stable version; annotated tags such as `v0.2.0` identify immutable releases. GitHub's default branch remains `main` so visitors see the stable portfolio version.

Before work, inspect `git status` and update `DEV` with `git pull --ff-only`. Preserve local changes. Optional `codex/<task>` branches start from `DEV` and integrate through review/PR. Use clear commits and risk-appropriate checks.

Release flow: owner authorization → `DEV` CI → remote-change check → safe integration into `main` → CI for the integrated commit → annotated tag → GitHub Release → remote confirmation. Never force push or move published tags. Pushing does not authorize a public deployment.

After publishing, synchronize `DEV` without discarding work and leave it checked out. This deliberately simple workflow does not require extra permanent branches or pretend external approval. [Running and testing](README.md#-english).
