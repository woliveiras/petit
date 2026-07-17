---
spec: "0105"
title: Resolução local de conflitos
family: local-sharing
phase: 2
status: In Progress
owner: ""
depends_on: ["0102"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Resolução local de conflitos

## Contexto e motivação

Duas pessoas cuidadoras podem editar ou excluir o mesmo registro antes de seus
dispositivos voltarem a se comunicar. O Petit precisa convergir sem intervenção
e sem perder silenciosamente uma alteração. A regra histórica é last-write-wins
por `updatedAt`, complementada por tratamento explícito de soft delete.

## Estado atual

O merge existente compara `updatedAt` e grava `SyncLog`. Não há um
`ConflictResolver` dedicado, interface de histórico nem testes para todos os
casos de soft delete. Timestamps iguais com payloads diferentes não têm regra
de desempate; portanto, simetria ainda não pode ser garantida.

## Requisitos

### Funcionais

- [x] Inserir entidade remota cujo UUID não existe localmente.
- [x] Preferir a versão com `updatedAt` mais recente no merge atual.
- [x] Registrar operações em `SyncLog`.
- [ ] Resolver soft delete comparando a exclusão com a edição concorrente.
- [ ] Definir desempate estável para timestamps iguais e payloads diferentes.
- [ ] Centralizar a regra para que transferência pontual e LAN produzam o mesmo resultado.
- [ ] Garantir determinismo, idempotência e simetria com testes.
- [ ] Exibir histórico de sync com enviados, recebidos e conflitos resolvidos.

### Não funcionais

- [ ] Integridade: aplicar cada lote em uma transação.
- [ ] Auditabilidade: registrar peer, tipo, horário e contadores sem dados clínicos no log.
- [ ] Performance: processar por UUID com consultas/lotes apropriados.
- [ ] Privacidade: manter logs localmente e sem conteúdo sensível desnecessário.

## Test strategy

Testes unitários tabelados cobrem todas as combinações local/remota, incluindo
ausência, edição, exclusão, timestamps iguais e repetição. Testes de integração
cobrem Room, transação, `SyncLog` e os mesmos resultados via spec 0102. Depois,
a spec 0104 deve reutilizar exatamente o mesmo resolver.

## Critérios de aceitação

- [ ] Dadas duas versões com timestamps distintos, quando são resolvidas em qualquer ordem, então a versão com `updatedAt` mais recente prevalece.
- [ ] Dado um UUID remoto inexistente, quando o lote é aplicado, então o registro é inserido uma única vez.
- [ ] Dado um soft delete e uma edição concorrente, quando são comparados, então o evento efetivamente mais recente prevalece.
- [ ] Dados timestamps iguais e payloads diferentes, quando são resolvidos em ambos os dispositivos, então o desempate documentado produz o mesmo resultado.
- [ ] Dado o mesmo changeset aplicado repetidamente, quando o merge termina, então estado e contadores não mudam após a primeira aplicação.
- [ ] Dada uma sincronização concluída, quando o histórico é aberto, então mostra peer, horário, tipo e contadores corretos.
- [ ] Dada uma falha durante o lote, quando a transação é revertida, então entidades e log permanecem consistentes.

## Casos extremos

- `updatedAt` igual com um lado deletado.
- Relógio local retrocede ou diverge do outro dispositivo.
- Entidade filha chega antes do pet pai.
- Mesmo soft delete é reaplicado.
- Lote contém versões duplicadas do mesmo UUID.
- Falha depois das entidades e antes do log.

## Decisões

| Decisão | Escolha | Justificativa |
| --- | --- | --- |
| Regra principal | Last-write-wins por `updatedAt` | Preserva a regra já usada e é simples quando timestamps diferem. |
| Exclusão | `deletedAt` participa como evento concorrente | Uma edição posterior pode desfazer uma exclusão anterior. |
| Implementação | Resolver único e puro | Evita divergência entre importação Nearby e futura sincronização LAN. |
| Empate | Decisão pendente antes da implementação final | Sem uma chave estável adicional, “manter local” quebra simetria. |
| Auditoria | `SyncLog` local com metadados e contadores | Permite diagnosticar sem duplicar conteúdo de saúde. |

## Fora de escopo

- Edição colaborativa em tempo real.
- Interface para a pessoa escolher manualmente cada conflito.
- Restaurar versões históricas de um registro.
- Transporte ou descoberta entre dispositivos.
