# US-005: Tarefas e Lembretes Locais

**Prioridade**: P1
**Épico**: Core Features
**Fase**: 1

> **Nota de evolução:** Originalmente especificado como "Lembretes" (`Reminder`). Na implementação foi renomeado para "Tarefas" (`Task`), com simplificação do modelo: remoção de repetição automática e snooze (não implementados na Fase 1).

---

## História

> Como tutor de pets,
> Eu quero receber lembretes sobre cuidados pendentes,
> Para que eu não esqueça de vacinar, vermifugar ou pesar meus pets.

---

## Cenários de Aceite

### Cenário 1: Tarefa automática de próxima dose

```gherkin
DADO que Luna tem vacina V3 com próxima dose em 20/03/2026
E configurei receber lembretes 7 dias antes
QUANDO salvo o registro de vacina
ENTÃO uma Task é criada automaticamente via AutoTaskService
E a Task tem kind=VACCINATION, referenceEntityId=id-da-vacina
E a notificação será disparada na data agendada via WorkManager
```

### Cenário 2: Lembrete de pesagem periódica

```gherkin
DADO que configurei lembrete de pesagem ativo
E o intervalo configurado é 30 dias
QUANDO salvo uma pesagem
ENTÃO uma Task é criada com kind=WEIGHT agendada para +30 dias
E recebo notificação na data agendada
"Luna: Hora de registrar o peso!"
```

### Cenário 3: Criar tarefa personalizada

```gherkin
DADO que estou na área de tarefas
QUANDO crio uma nova tarefa
E informo título "Consulta veterinária"
E informo data/hora "20/03/2026 14:00"
E associo ao pet "Luna"
E salvo
ENTÃO a tarefa é salva com status PENDING
E receberei notificação na data/hora especificada
```

### Cenário 4: Tarefa sem internet

```gherkin
DADO que estou sem conexão de internet
QUANDO uma tarefa está agendada
ENTÃO a notificação dispara normalmente
(WorkManager + NotificationManager - 100% local)
```

### Cenário 5: Concluir tarefa

```gherkin
DADO que tenho uma tarefa PENDING
QUANDO marco como concluída
ENTÃO o status muda para COMPLETED
E a tarefa vai para a lista de tarefas concluídas
E o agendamento WorkManager é cancelado
```

### Cenário 6: Deletar registro delegado exclui a tarefa vinculada

```gherkin
DADO que existe uma tarefa vinculada à vacina V3 da Luna
QUANDO deleto o registro de vacina
ENTÃO as tarefas vinculadas (por referenceEntityId) são soft-deletadas
E o agendamento correspondente é cancelado no WorkManager
```

### Cenário 4: Lembrete com repetição

> ⚠️ **NÃO IMPLEMENTADO na Fase 1.** O campo `repeatInterval` foi removido do modelo `Task`. Tarefas são one-shot. Repetição automática não existe na versão atual.

### Cenário 5: Adiar lembrete (snooze)

> ⚠️ **NÃO IMPLEMENTADO na Fase 1.** O status `SNOOZED` foi removido do modelo. Não há botão de snooze nas notificações. A notificação é simples, sem action buttons.

### Cenário 6: Lembrete sem internet

```gherkin
DADO que estou sem conexão de internet
QUANDO um lembrete está agendado
ENTÃO a notificação dispara normalmente
(WorkManager + NotificationManager - 100% local)
```

---

## Tipos de Tarefa

| Tipo        | Gatilho                                 | Exemplo                |
| ----------- | --------------------------------------- | ---------------------- |
| VACCINATION | Próxima dose de vacina (auto-criado)    | "V3 em 7 dias"         |
| DEWORMING   | Próxima dose de vermífugo (auto-criado) | "Frontline em 5 dias"  |
| WEIGHT      | Pesagem periódica (auto-criado)         | "Hora de pesar"        |
| CUSTOM      | Manual do usuário                       | "Consulta veterinária" |

---

## Campos do Formulário (Tarefa Personalizada)

| Campo          | Tipo           | Obrigatório | Validação                  |
| -------------- | -------------- | ----------- | -------------------------- |
| Título         | TextField      | ✅          | 1-100 chars                |
| Descrição      | TextField      | ❌          | Máx 500 chars              |
| Pet           | Dropdown       | ❌          | Lista de pets ou "Geral"  |
| Tipo de tarefa | Dropdown       | ❌          | TaskKind (default: CUSTOM) |
| Data/Hora      | DateTimePicker | ✅          | Deve ser futura            |

---

## Configurações Globais de Tarefa

| Configuração             | Tipo                | Default    |
| ------------------------ | ------------------- | ---------- |
| Lembretes de vacina      | Toggle              | ✅ Ativo   |
| Antecedência vacina      | Slider (1–30 dias)  | 7 dias     |
| Lembretes de vermífugo   | Toggle              | ✅ Ativo   |
| Antecedência vermífugo   | Slider (1–30 dias)  | 7 dias     |
| Lembrete de pesagem      | Toggle              | ❌ Inativo |
| Intervalo pesagem        | configurado em dias | 30 dias    |
| Horário das notificações | TimePicker          | 09:00      |

---

## UI/UX

### Tela: Lista de Tarefas

```
┌────────────────────────────────┐
│ ← Tarefas               [+]   │
├────────────────────────────────┤
│ [TODOS] [HOJE] [SEMANA] [MÊS]  │  ← filtros
├────────────────────────────────┤
│ 🔔 PRÓXIMAS                    │
│ ┌────────────────────────────┐ │
│ │ 💉 Vacina V3 - Luna        │ │
│ │ Em 7 dias (20/03/2026)     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ⚖️ Pesagem - Simba         │ │
│ │ Em 3 dias (18/03/2026)     │ │
│ └────────────────────────────┘ │
│                                │
│ ✅ CONCLUÍDAS              ▶   │  ← navega para CompletedTasksScreen
│                                │
└────────────────────────────────┘
```

### Notificação

```
┌────────────────────────────────┐
│ 🐱 Petit                         │
│ Luna: Vacina V3 em 7 dias      │
│                                │
│ [Ver]                          │  ← sem Adiar/Concluir (one-shot)
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Entity

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val petId: String? = null,
    val kind: String,              // TaskKind: VACCINATION, DEWORMING, WEIGHT, CUSTOM
    val referenceEntityId: String? = null,
    val title: String,
    val description: String? = null,
    val scheduledFor: Long,        // Timestamp UTC
    val status: String = "PENDING", // TaskStatus: PENDING | COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY"
)
```

### WorkManager

```kotlin
@HiltWorker
class TaskNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val task = taskRepository.getTaskById(taskId) ?: return Result.failure()

        // Lança notificação local (PendingIntent com FLAG_IMMUTABLE)
        showNotification(task)

        return Result.success()
    }
}
```

### AutoTaskService

Responsável por criar/cancelar tarefas automaticamente quando registros de saúde são salvos ou deletados:

```kotlin
interface AutoTaskService {
    suspend fun handleVaccinationSaved(entry: VaccinationEntry)
    suspend fun handleVaccinationDeleted(entryId: String)
    suspend fun handleDewormingSaved(entry: DewormingEntry)
    suspend fun handleDewormingDeleted(entryId: String)
    suspend fun handleWeightSaved(petId: String, petName: String)
    suspend fun cancelWeightTask(petId: String)
}
```

### Telas Implementadas

| Tela                 | Rota                         | Descrição                                                           |
| -------------------- | ---------------------------- | ------------------------------------------------------------------- |
| TaskListScreen       | `tasks`                      | Lista de tarefas ativas com filtro (ALL/TODAY/THIS_WEEK/THIS_MONTH) |
| TaskFormScreen       | `tasks/form?taskId={taskId}` | Criar/editar tarefa                                                 |
| TaskSettingsScreen   | `tasks/settings`             | Configurações por categoria + slider dias-antes                     |
| CompletedTasksScreen | `tasks/completed`            | Histórico de tarefas concluídas                                     |

### DataStore para Configurações

```kotlin
data class ReminderPreferences(
    val vaccinationRemindersEnabled: Boolean = true,
    val vaccinationDaysBefore: Int = 7,
    val dewormingRemindersEnabled: Boolean = true,
    val dewormingDaysBefore: Int = 7,
    val weightRemindersEnabled: Boolean = false,
    val weightReminderIntervalDays: Int = 30,
    val defaultNotificationHour: Int = 9,
    val defaultNotificationMinute: Int = 0
)
```

---

## Definition of Done

- [x] Tarefa automática para próxima dose de vacina
- [x] Tarefa automática para próxima dose de vermífugo
- [x] Tarefa automática de pesagem periódica (condicional ao toggle)
- [x] Criação de tarefa personalizada (CUSTOM)
- [ ] Repetição de tarefas — **não implementado** (futuro)
- [ ] Snooze de tarefas — **não implementado** (futuro)
- [x] Notificações locais disparando (one-shot via WorkManager)
- [x] Funciona 100% offline
- [x] Tela de configurações de tarefas (TaskSettingsScreen)
- [x] Lista de tarefas ativas com filtros
- [x] Tela de tarefas concluídas
- [x] Auto-delete de tarefa ao deletar entidade vinculada
