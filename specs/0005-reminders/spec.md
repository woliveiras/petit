---
spec: "0005"
title: Tarefas e lembretes locais
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Tarefas e lembretes locais

## Contexto e motivação

O tutor precisa ser lembrado de cuidados pendentes mesmo sem conexão. O conceito histórico de lembrete evoluiu para tarefas one-shot.

## Requisitos funcionais

- Criar tarefas automáticas para próximas vacinas, desparasitações e pesagens periódicas habilitadas.
- Criar e editar tarefa customizada, opcionalmente associada a um pet.
- Agendar notificação local com WorkManager e concluir ou excluir tarefas.
- Cancelar agendamento e soft-delete da tarefa vinculada quando o registro de saúde é excluído.
- Configurar categorias, antecedência, intervalo de pesagem e horário padrão.
- Filtrar tarefas ativas e exibir histórico de concluídas.

## Critérios de aceite

- Dada uma próxima dose, quando salva o registro, então uma tarefa vinculada é criada e agendada conforme preferências.
- Dado lembrete de pesagem habilitado, quando salva uma pesagem, então a próxima tarefa é criada no intervalo configurado.
- Dada uma tarefa customizada futura, quando salva, então fica `PENDING` e recebe notificação local no horário.
- Dada uma tarefa pendente, quando conclui, então fica `COMPLETED`, sai da lista ativa e o agendamento é cancelado.
- Dado um registro vinculado excluído, então suas tarefas são soft-deletadas e os trabalhos cancelados.
- Dado o dispositivo offline, então a notificação local continua funcionando.

## Estratégia de testes

Unitários cobrem preferências, datas e mapeamentos; integração cobre Room, AutoTaskService e WorkManager; UI cobre formulários, filtros e conclusão.

## Fora de escopo

- Repetição automática de tarefas e snooze de notificações.
