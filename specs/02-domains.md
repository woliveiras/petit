# Domínios e Entidades

## Diagrama de Relacionamentos

```
┌─────────────┐
│     Pet     │
└──────┬──────┘
       │
       │ 1:N
       │
       ├──────────────┬──────────────┬──────────────┐
       │              │              │              │
       ▼              ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ WeightEntry │ │ Vaccination │ │  Deworming  │ │    Task     │
│             │ │    Entry    │ │    Entry    │ │             │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

---

## Entidades

### Pet (Animal de estimação)

| Campo             | Tipo      | Obrigatório | Descrição                     |
| ----------------- | --------- | ----------- | ----------------------------- |
| `id`              | UUID      | ✅          | Identificador único           |
| `name`            | String    | ✅          | Nome do pet (máx 50 chars)    |
| `petType`         | Enum      | ✅          | `CAT`, `DOG`, `RABBIT`, `BIRD`, `HAMSTER`, `OTHER` |
| `birthDate`       | LocalDate | ❌          | Data de nascimento            |
| `sex`             | Enum      | ❌          | `MALE`, `FEMALE`, `UNKNOWN`   |
| `breed`           | String    | ❌          | Raça do pet (máx 50 chars)    |
| `color`           | String    | ❌          | Cor/pelagem (máx 50 chars)    |
| `microchipNumber` | String    | ❌          | Número do microchip (máx 50)  |
| `passportNumber`  | String    | ❌          | Número do passaporte (máx 50) |
| `photoUri`        | String    | ❌          | URI da foto local             |
| `notes`           | String    | ❌          | Observações (máx 500 chars)   |
| `createdAt`       | Long      | ✅          | Timestamp de criação          |
| `updatedAt`       | Long      | ✅          | Timestamp de atualização      |
| `deletedAt`       | Long      | ❌          | Timestamp de soft delete      |
| `syncStatus`      | Enum      | ✅          | Status de sincronização       |

**Regras de Negócio**:

- Nome é obrigatório e não pode ser vazio
- `updatedAt` deve ser atualizado em toda modificação
- Validação de tamanho aplicada no ViewModel antes do salvamento

---

### WeightEntry (Registro de Peso)

| Campo         | Tipo      | Obrigatório | Descrição                  |
| ------------- | --------- | ----------- | -------------------------- |
| `id`          | UUID      | ✅          | Identificador único        |
| `petId`       | UUID      | ✅          | FK para Pet                |
| `date`        | LocalDate | ✅          | Data da pesagem            |
| `weightGrams` | Int       | ✅          | Peso em gramas             |
| `note`        | String    | ❌          | Observação (máx 200 chars) |
| `createdAt`   | Long      | ✅          | Timestamp de criação       |
| `updatedAt`   | Long      | ✅          | Timestamp de atualização   |
| `deletedAt`   | Long      | ❌          | Timestamp de soft delete   |
| `syncStatus`  | Enum      | ✅          | Status de sincronização    |

**Regras de Negócio**:

- Peso deve ser > 0
- Peso armazenado em gramas para precisão (ex: 3500 = 3.5kg)
- Apenas um registro por dia por pet (upsert por data)
- Peso máximo aceito: 50 kg (rejeição hard no ViewModel)

---

### VaccinationEntry (Registro de Vacinação)

| Campo             | Tipo      | Obrigatório | Descrição                |
| ----------------- | --------- | ----------- | ------------------------ |
| `id`              | UUID      | ✅          | Identificador único      |
| `petId`           | UUID      | ✅          | FK para Pet              |
| `vaccineType`     | Enum      | ✅          | Tipo de vacina           |
| `customVaccineTypeName` | String | ❌       | Nome quando o tipo é `OTHER` |
| `applicationDate` | LocalDate | ✅          | Data de aplicação        |
| `nextDueDate`     | LocalDate | ❌          | Próxima dose             |
| `veterinarian`    | String    | ❌          | Nome do veterinário      |
| `clinic`          | String    | ❌          | Nome da clínica          |
| `batchNumber`     | String    | ❌          | Lote da vacina           |
| `note`            | String    | ❌          | Observação               |
| `createdAt`       | Long      | ✅          | Timestamp de criação     |
| `updatedAt`       | Long      | ✅          | Timestamp de atualização |
| `deletedAt`       | Long      | ❌          | Timestamp de soft delete |
| `syncStatus`      | Enum      | ✅          | Status de sincronização  |

> **Nota:** O campo `status` NÃO é armazenado no banco. É calculado em runtime a partir de `nextDueDate` vs a data atual, via `HealthStatus`.

**Tipos de Vacina**:

```kotlin
enum class VaccineType {
    RABIES,       // Todos os tipos de pet
    OTHER,        // Todos os tipos de pet
    V3,           // Trivalente (Panleucopenia, Rinotraqueíte, Calicivirose)
    V4,           // Tetravalente (V3 + Clamidiose)
    V5,           // Pentavalente (V4 + Leucemia Felina)
    FELV,         // Leucemia Felina (separada)
    FIV,          // Imunodeficiência Felina
    DHPP,         // Cães
    BORDETELLA,   // Cães
    LEPTOSPIROSIS,// Cães
    LEISHMANIA,   // Cães
    GRIPE_CANINA, // Cães
    RHDV,         // Coelhos
    MYXOMATOSIS,  // Coelhos
    POLYOMAVIRUS  // Aves
}
```

`VaccineType.forPetType` filtra as opções aplicáveis à espécie selecionada.

**Status de saúde** (calculado, via `HealthStatus` — compartilhado com DewormingEntry):

```kotlin
enum class HealthStatus {
    OK,           // Em dia
    SCHEDULED,    // Próxima dose definida (≤30 dias)
    OVERDUE       // Atrasada
}
```

**Regras de Negócio**:

- Status é calculado em runtime baseado em `nextDueDate` vs hoje
- Se `nextDueDate` < hoje → `OVERDUE`
- Se `nextDueDate` está nos próximos 30 dias → `SCHEDULED`
- Se `nextDueDate` > 30 dias ou null → `OK`

---

### DewormingEntry (Registro de Desparasitação)

| Campo             | Tipo      | Obrigatório | Descrição                |
| ----------------- | --------- | ----------- | ------------------------ |
| `id`              | UUID      | ✅          | Identificador único      |
| `petId`           | UUID      | ✅          | FK para Pet              |
| `type`            | Enum      | ✅          | Tipo de vermífugo        |
| `medication`      | String    | ✅          | Nome do medicamento      |
| `applicationDate` | LocalDate | ✅          | Data de aplicação        |
| `nextDueDate`     | LocalDate | ❌          | Próxima dose             |
| `note`            | String    | ❌          | Observação               |
| `createdAt`       | Long      | ✅          | Timestamp de criação     |
| `updatedAt`       | Long      | ✅          | Timestamp de atualização |
| `deletedAt`       | Long      | ❌          | Timestamp de soft delete |
| `syncStatus`      | Enum      | ✅          | Status de sincronização  |

> **Nota:** O campo `status` NÃO é armazenado no banco. É calculado em runtime via `HealthStatus` (mesmo enum compartilhado com VaccinationEntry).

**Tipos**:

```kotlin
enum class DewormingType {
    INTERNAL,     // Vermífugo interno (comprimido/pasta)
    EXTERNAL,     // Antipulgas/carrapatos (pipeta/coleira)
    BOTH          // Combo (ambos)
}
```

**Status**: Mesma lógica de `HealthStatus` (vide VaccinationEntry)

---

### Task (Tarefa / Lembrete)

> **Nota de evolução:** Na documentação histórica do MiW, esta entidade era
> chamada `Reminder`. O Petit atual usa `Task` e a tabela `tasks`, com um modelo
> sem repetição ou snooze.

| Campo               | Tipo          | Obrigatório | Descrição                        |
| ------------------- | ------------- | ----------- | -------------------------------- |
| `id`                | UUID          | ✅          | Identificador único              |
| `petId`             | UUID          | ❌          | FK para Pet (null = geral)       |
| `kind`              | Enum          | ✅          | Tipo da tarefa (`TaskKind`)      |
| `referenceEntityId` | UUID          | ❌          | ID da entidade relacionada       |
| `title`             | String        | ✅          | Título da tarefa (máx 100 chars) |
| `description`       | String        | ❌          | Descrição (máx 500 chars)        |
| `scheduledFor`      | LocalDateTime | ✅          | Quando disparar                  |
| `status`            | Enum          | ✅          | Status atual (`TaskStatus`)      |
| `createdAt`         | Long          | ✅          | Timestamp de criação             |
| `updatedAt`         | Long          | ✅          | Timestamp de atualização         |
| `deletedAt`         | Long          | ❌          | Timestamp de soft delete         |
| `syncStatus`        | Enum          | ✅          | Status de sincronização          |

**Tipos de Tarefa**:

```kotlin
enum class TaskKind {
    WEIGHT,           // Lembrete de pesagem
    VACCINATION,      // Lembrete de vacina
    DEWORMING,        // Lembrete de vermífugo
    MEDICATION,       // Lembrete de medicação (futuro)
    CUSTOM            // Lembrete personalizado
}
```

**Status**:

```kotlin
enum class TaskStatus {
    PENDING,      // Pendente (ativo)
    COMPLETED     // Concluído
}
```

**Propriedades calculadas** (não persistidas):

- `isPastDue` — `scheduledFor` anterior à data atual e ainda `PENDING`
- `isPending` — status igual a `PENDING`
- `isCompleted` — status igual a `COMPLETED`

---

## Tipos Comuns

### SyncStatus

```kotlin
enum class SyncStatus {
    LOCAL_ONLY,      // Nunca sincronizado
    PENDING_SYNC,    // Aguardando sync
    SYNCED,          // Sincronizado
    CONFLICT         // Conflito
}
```

> **Nota de implementação:** Todas as entidades possuem os campos `createdAt`, `updatedAt`, `deletedAt` e `syncStatus` por convenção, mas não compartilham uma interface comum em código. A interface `SyncableEntity` é um conceito arquitetural de referência.

---

## Modelos de Suporte

### TimelineEvent (Evento de Timeline)

Modelo de _leitura_ agregado — não possui tabela própria no banco. Gerado por `TimelineDao` via queries cross-table:

| Campo      | Tipo              | Descrição                                  |
| ---------- | ----------------- | ------------------------------------------ |
| `id`       | String            | ID da entidade de origem                   |
| `petId`    | String            | FK para Pet                                |
| `petName`  | String            | Nome do pet (join)                        |
| `type`     | TimelineEventType | Tipo do evento                             |
| `date`     | LocalDate         | Data do evento                             |
| `title`    | String            | Título exibível                            |
| `subtitle` | String?           | Subtítulo/detalhe (dose, medicacao, etc.)  |
| `isDue`    | Boolean           | `true` = data futura pendente (vencimento) |

```kotlin
enum class TimelineEventType {
    WEIGHT,
    VACCINATION,
    DEWORMING,
    VACCINATION_DUE,   // Próxima dose de vacina (futura)
    DEWORMING_DUE,     // Próxima dose de vermífugo (futura)
    REMINDER           // Task/Lembrete agendado
}
```

---

### ExportBundle (Bundle de Export/Import)

Modelo de serialização JSON para backup. Não possui tabela Room — é transiente.

```kotlin
data class ExportBundle(
    val metadata: ExportMetadata,
    val pets: List<Pet>,
    val weightEntries: List<WeightEntry>,
    val vaccinationEntries: List<VaccinationEntry>,
    val dewormingEntries: List<DewormingEntry>,
    val tasks: List<Task>           // chave JSON: "tasks"
)

data class ExportMetadata(
    val appVersion: String,
    val exportDate: String,         // ISO-8601
    val schemaVersion: Int          // versão do schema de export
)

data class ImportAnalysis(
    val totalPets: Int,
    val totalWeightEntries: Int,
    val totalVaccinationEntries: Int,
    val totalDewormingEntries: Int,
    val totalTasks: Int,
    val conflictingPetNames: List<String>,
    val schemaVersion: Int,
    val exportDate: String
)

enum class ConflictResolution {
    REPLACE,         // Dados do backup substituem os locais
    KEEP,            // Dados locais são mantidos
    MERGE            // Last-write-wins por updatedAt
}
```

`ImportAnalysis.hasConflicts` é calculado a partir de
`conflictingPetNames.isNotEmpty()`.

> **Compatibilidade:** na data desta migração, o import do Petit lê a chave
> `"tasks"`; arquivos históricos que contenham apenas `"reminders"` precisam de
> migração explícita antes da importação.

---

### AppSettings (Preferências do App)

Armazenado via DataStore (`user_preferences`):

```kotlin
enum class AppTheme { SYSTEM, LIGHT, DARK }
enum class AppLanguage { SYSTEM, ENGLISH, PORTUGUESE_BR }
```

---

## DataStore — Chaves de Preferências

### `user_preferences`

| Chave      | Tipo   | Default              | Descrição     |
| ---------- | ------ | -------------------- | ------------- |
| `theme`    | String | `AppTheme.SYSTEM`    | Tema do app   |
| `language` | String | `AppLanguage.SYSTEM` | Idioma do app |

### `reminder_preferences`

| Chave                           | Tipo    | Default | Descrição                               |
| ------------------------------- | ------- | ------- | --------------------------------------- |
| `vaccination_reminders_enabled` | Boolean | `true`  | Ativar tarefas automáticas de vacina    |
| `vaccination_days_before`       | Int     | `7`     | Dias antes do vencimento                |
| `deworming_reminders_enabled`   | Boolean | `true`  | Ativar tarefas automáticas de vermífugo |
| `deworming_days_before`         | Int     | `7`     | Dias antes do vencimento                |
| `weight_reminders_enabled`      | Boolean | `false` | Ativar lembretes periódicos de pesagem  |
| `weight_interval_days`          | Int     | `30`    | Intervalo em dias entre lembretes       |
| `notification_hour`             | Int     | `9`     | Hora das notificações (0–23)            |
| `notification_minute`           | Int     | `0`     | Minuto das notificações (0–59)          |
