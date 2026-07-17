---
spec: "0006"
title: Exportação e importação JSON
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001", "0002", "0003", "0004", "0005"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Exportação e importação JSON

## Contexto e motivação

O tutor precisa criar backup manual e restaurar seus dados em outro dispositivo ou após reinstalar o app.

## Requisitos funcionais

- Exportar pets, pesos, vacinas, desparasitações e tarefas para JSON por URI escolhida pelo usuário.
- Incluir versão do app, data da exportação e versão do schema; nomear `petit_backup_YYYY-MM-DD.json`.
- Ler e validar backup, apresentar contagens e conflitos antes da confirmação.
- Resolver conflitos por substituir, manter ou mesclar pelo `updatedAt` mais recente.
- Aplicar importação atomicamente e rejeitar arquivo inválido sem alterar dados.
- Permitir exportar apenas um pet e seus registros relacionados.

## Critérios de aceite

- Dados registros locais, quando exporta tudo, então o JSON contém todos os domínios e metadados.
- Dado backup válido, quando seleciona, então vê contagens, conflitos e pode confirmar ou cancelar.
- Dado conflito de ID, quando escolhe uma estratégia, então o resultado respeita `REPLACE`, `KEEP` ou `MERGE`.
- Dado arquivo inválido ou corrompido, então nenhum dado local é alterado.
- Dado um pet, quando exporta seu perfil, então somente ele e seus dados relacionados são incluídos.

## Estratégia de testes

Unitários cobrem serialização, parsing e merge; integração cobre ContentResolver, transação Room e fluxos de seleção; UI cobre análise e confirmação.

## Limitações conhecidas

- `exportForPet(petId)` existe, mas não há ponto de entrada correspondente no perfil do pet.
- O formato atual usa a chave `tasks`; backups históricos com apenas `reminders` exigem conversão explícita.
