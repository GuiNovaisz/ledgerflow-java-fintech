# Organização por feature

O LedgerFlow já agrupa `account`, `ledger`, `insights`, `reporting`, `auth` e `goals`. Algumas features maiores têm `api`, `application`, `domain` e `persistence`; outras permanecem em um único pacote quando são pequenas.

**Problema de uma organização só por camada:** encontrar uma operação exigiria circular entre diretórios globais de controllers, services e repositories. **Solução mantida:** navegação por feature, com separação interna somente onde a complexidade justifica. A nova `InsightCsvExporter` permanece junto de insights; `CsvCell` fica em `shared` por ser realmente usada por insights e reporting. **Trade-off:** a estrutura não é perfeitamente uniforme; uniformizá-la agora criaria pacotes de um arquivo sem ganho.
