---
spec: "0007"
title: Dashboard inicial
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001", "0002", "0003", "0004", "0005"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Dashboard inicial

## Contexto e motivação

O tutor precisa enxergar rapidamente o estado de saúde dos pets e os cuidados que exigem atenção.

## Requisitos funcionais

- Exibir boas-vindas e ação para cadastrar o primeiro pet quando não há dados.
- Exibir cards dos pets com foto, nome, último peso, estado geral e próximo evento.
- Exibir até cinco tarefas próximas e atividade recente, com acesso às listas completas.
- Abrir o perfil ao tocar no pet e oferecer Quick Add para peso, vacina, desparasitação, tarefa e novo pet.
- Permitir acesso às configurações e atualização manual.

## Critérios de aceite

- Sem pets, quando abre a home, então vê boas-vindas e botão para cadastrar o primeiro pet.
- Com pets, quando abre a home, então vê resumo individual e indicação visual de saúde.
- Com tarefas, então vê até cinco próximas e pode abrir a lista completa.
- Com eventos de saúde, então vê atividade recente e pode abrir a timeline completa.
- Ao tocar num pet ou ação rápida, então navega ao perfil ou ao fluxo correto, selecionando pet quando necessário.

## Estratégia de testes

Unitários cobrem agregação e estado vazio; integração cobre repositórios e navegação; UI cobre estados, Quick Add, refresh e acessibilidade.

## Limitações conhecidas

- O banner “Tudo em ordem” e uma seção separada de alertas não foram implementados.
- O Quick Add substituiu o Speed Dial histórico.
