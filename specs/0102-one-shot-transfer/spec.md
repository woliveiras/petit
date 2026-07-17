---
spec: "0102"
title: Transferência pontual de dados
family: local-sharing
phase: 2
status: In Progress
owner: ""
depends_on: ["0101"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Transferência pontual de dados

## Contexto e motivação

Depois do pareamento, uma pessoa cuidadora precisa enviar o histórico completo
dos pets a outro dispositivo sem servidor ou internet. O receptor escolhe
mesclar os dados recebidos ou substituir sua base local.

## Estado atual

`NearbyTransferRepository`, `SendDataUseCase`, `MergeDataUseCase` e a interface
de transferência existem. O modo `REPLACE` sobrescreve entidades coincidentes,
mas não remove registros locais ausentes do bundle. Progresso, falhas e o fluxo
completo ainda não foram validados em dois dispositivos.

## Requisitos

### Funcionais

- [x] Serializar os dados compartilháveis em um `ExportBundle`.
- [x] Enviar e receber o bundle pela conexão Nearby autorizada.
- [x] Oferecer ao receptor as opções de mesclar e substituir.
- [x] Mesclar entidades por UUID e `updatedAt`.
- [ ] Fazer `REPLACE` remover dados locais ausentes antes da importação.
- [ ] Exibir progresso real e resumo por tipo de entidade.
- [ ] Descartar payload parcial quando a transferência falhar.
- [ ] Confirmar o modo destrutivo antes de substituir.

### Não funcionais

- [ ] Integridade: aplicar a importação de modo atômico.
- [ ] Segurança: aceitar payload somente do endpoint pareado.
- [ ] Performance: escolher BYTES ou FILE conforme o limite do payload sem truncar o bundle.
- [ ] Acessibilidade e i18n: anunciar progresso e manter textos em pt-BR, en e es.

## Test strategy

Testes unitários cobrem serialização, merge, replace, contadores e tratamento de
payload incompleto. Testes de integração cobrem Room, Nearby e o fluxo entre
dois dispositivos, inclusive sem internet. Consulte a
[pesquisa de protocolos](../../docs/local-sharing-protocols.md).

## Critérios de aceitação

- [ ] Dado um dispositivo pareado com dados locais, quando envia, então o receptor recebe um `ExportBundle` completo e vê progresso e conclusão.
- [ ] Dado um receptor com dados existentes, quando escolhe mesclar, então UUIDs são combinados e a versão com `updatedAt` mais recente prevalece.
- [ ] Dado um receptor com registros ausentes do bundle, quando confirma substituir, então a base compartilhável é limpa e passa a refletir somente o bundle.
- [ ] Dada uma conexão interrompida, quando chega apenas parte do payload, então nenhuma alteração é persistida e uma nova tentativa é oferecida.
- [ ] Dados dois dispositivos sem internet, quando transferem por Nearby, então o fluxo termina com sucesso.
- [ ] Dada uma importação concluída, quando o resumo é exibido, então os contadores correspondem às entidades realmente adicionadas, atualizadas e removidas.

## Casos extremos

- Bundle vazio, incompatível ou maior que o limite de BYTES.
- Espaço insuficiente ou falha transacional no receptor.
- Entidades filhas cujo pet não existe no bundle.
- Mesmo bundle recebido mais de uma vez.
- Soft delete mais recente que a cópia ativa.

## Decisões

| Decisão | Escolha | Justificativa |
| --- | --- | --- |
| Formato | `ExportBundle` serializado | Reutiliza o limite de exportação/importação local existente. |
| Mesclagem | UUID + `updatedAt` | Mantém resultado determinístico para versões com timestamps distintos. |
| Substituição | Limpeza transacional seguida de importação | Faz o comportamento corresponder ao significado apresentado à pessoa usuária. |
| Transporte | Nearby após a spec 0101 | Funciona localmente e reutiliza o canal autorizado. |

## Fora de escopo

- Descobrir ou parear dispositivos.
- Sincronização automática ou incremental.
- Resolver o desempate de timestamps idênticos; consulte a spec 0105.
- Backup em nuvem.
