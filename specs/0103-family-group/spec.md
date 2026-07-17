---
spec: "0103"
title: Grupo familiar local
family: local-sharing
phase: 2
status: In Progress
owner: ""
depends_on: ["0101"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Grupo familiar local

## Contexto e motivação

Depois de parear dispositivos, pessoas cuidadoras precisam saber quem participa
do compartilhamento e controlar o acesso sem uma conta central. O grupo é
mantido localmente e sair dele nunca apaga o histórico de saúde dos pets.

Esta spec consolida a história canônica `us-103-family-group`; a variante
`us-103-household-group` continha somente uma navegação e um wireframe antigos,
sem requisitos adicionais.

## Estado atual

A lista de membros, a remoção local, a saída do grupo e as preferências no
DataStore existem. Ainda não há evidência de renomeação do dispositivo nem de
propagação de remoção ou saída ao outro dispositivo na sincronização seguinte.

## Requisitos

### Funcionais

- [x] Exibir os membros conhecidos e identificar o dispositivo local.
- [x] Remover um membro da lista local mediante confirmação.
- [x] Sair do grupo removendo a chave local e preservando dados dos pets.
- [x] Gerenciar chave, ID e nome local nas preferências do grupo.
- [ ] Permitir renomear o dispositivo local.
- [ ] Propagar renomeação, remoção e saída aos demais membros.
- [ ] Exibir o horário da última sincronização ou “nunca sincronizou” por membro.
- [ ] Garantir que um dispositivo removido perca autorização para sincronizações futuras.

### Não funcionais

- [ ] Consistência: operações repetidas de remoção e saída devem ser idempotentes.
- [ ] Privacidade: dados de saúde permanecem locais ao remover acesso.
- [ ] Acessibilidade: confirmação e ações destrutivas têm rótulos e alvos adequados.
- [ ] Internacionalização: textos visíveis permanecem em `strings.xml` para pt-BR, en e es.

## Test strategy

Testes unitários cobrem ViewModel e casos de uso de renomear, remover e sair.
Testes de integração cobrem Room, DataStore e a propagação entre dois
dispositivos. O bloqueio de um membro removido deve ser exercitado no canal de
transferência e, quando existir, no canal LAN.

## Critérios de aceitação

- [ ] Dado um grupo existente, quando a tela é aberta, então lista os membros, marca o dispositivo local e informa a última sincronização de cada um.
- [ ] Dado o membro local, quando seu nome é alterado, então o novo nome é persistido e aparece no outro dispositivo após sincronização.
- [ ] Dado um membro remoto, quando sua remoção é confirmada, então ele sai da lista e não consegue iniciar nova sincronização com a chave anterior.
- [ ] Dado um membro que escolhe sair, quando confirma, então a chave e os vínculos locais são removidos, mas os dados dos pets permanecem.
- [ ] Dada uma remoção ou saída offline, quando os dispositivos voltam a se comunicar, então a alteração de membresia é propagada de forma idempotente.

## Casos extremos

- Grupo sem membros remotos ou com nomes repetidos.
- Remoção simultânea em dois dispositivos.
- Dispositivo removido tenta transferir usando uma chave antiga.
- Aplicativo é encerrado entre a confirmação e a persistência.
- Última sincronização desconhecida.

## Decisões

| Decisão | Escolha | Justificativa |
| --- | --- | --- |
| Autoridade | Estado local propagado entre pares | Mantém o produto sem servidor central. |
| Saída do grupo | Preservar todos os dados dos pets | Remover compartilhamento não significa apagar histórico de saúde. |
| Identidade | UUID estável por dispositivo | Evita usar nome editável como chave. |
| Remoção | Revogar referência de sincronização e propagar um marcador | Impede que uma exclusão apenas visual seja revertida pelo próximo contato. |

## Fora de escopo

- Pareamento inicial, definido na spec 0101.
- Transferência de todo o histórico, definida na spec 0102.
- Interface de histórico detalhado de sincronizações, tratada na spec 0105.
- Papéis administrativos ou recuperação do grupo por cloud.
