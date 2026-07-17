# Arquitetura

## Princípios

### 1. Local-First

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   UI/View   │────▶│  ViewModel  │────▶│ Repository  │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
             ┌─────────────┐           ┌─────────────┐           ┌─────────────┐
             │    Room     │           │  DataStore  │           │ WorkManager │
             │  (verdade)  │           │   (prefs)   │           │   (sync)    │
             └─────────────┘           └─────────────┘           └─────────────┘
```

**Regras**:

- UI sempre lê do banco local (Room)
- Operações de escrita vão direto para Room
- Sync remoto acontece em background via WorkManager
- Ausência de internet **NUNCA** bloqueia operações

### 2. Soft Delete

Todas as entidades sincronizáveis usam soft delete:

- `deletedAt: Long?` - timestamp de exclusão ou null
- Queries filtram `WHERE deletedAt IS NULL` por padrão
- Sync propaga deletions para resolver em outros devices

### 3. Sync Status

```kotlin
enum class SyncStatus {
    LOCAL_ONLY,      // Nunca sincronizado (free tier)
    PENDING_SYNC,    // Modificado, aguardando sync
    SYNCED,          // Sincronizado com sucesso
    CONFLICT         // Conflito detectado (resolver)
}
```

---

## Camadas

### Presentation Layer

- **Jetpack Compose** para UI
- **ViewModel** com StateFlow para estado
- **Navigation Compose** para navegação

### Domain Layer

- **Use Cases** encapsulam regras de negócio
- **Domain Models** puros (sem anotações Room)

### Data Layer

- **Repository** abstrai fonte de dados
- **Room DAOs** para acesso ao banco
- **DataStore** para preferências

### Background Layer

- **WorkManager Workers** para:
  - Disparo de notificações de tarefas (`TaskNotificationWorker` — one-shot)
  - Automação de criação de tarefas ao salvar registros de saúde (`AutoTaskService`)
  - Agendamento e cancelamento de notificações (`TaskScheduler` / `TaskSchedulerImpl`)
  - Sync na rede local via NSD (Network Service Discovery) — Fase 2
  - Sincronização remota via Firebase Firestore — Fase N+2 (holding)

---

## Estrutura de Pacotes

```
com.woliveiras.petit/
├── data/
│   ├── local/
│   │   ├── db/                     — PetitDatabase e migrações
│   │   ├── dao/                    — @Dao interfaces (PetDao, WeightEntryDao, etc.)
│   │   └── entity/                 — @Entity classes Room
│   ├── mapper/                     — conversão Entity ↔ Domain model
│   └── repository/                 — interfaces e implementações de Repository
├── domain/
│   ├── model/                      — modelos de domínio puros (sem deps Android)
│   └── usecase/                    — ações cross-repository (ExportImport, DeleteAll)
├── presentation/
│   ├── feature/
│   │   ├── home/                   — HomeScreen, HomeViewModel, HomeUiState
│   │   ├── pets/                   — PetList, PetDetail, PetForm, PetDeleteConfirmation, PetSelection
│   │   ├── weight/                 — WeightEntry, WeightForm
│   │   ├── vaccination/            — VaccinationRecords, VaccinationForm
│   │   ├── deworming/              — DewormingRecords, DewormingForm
│   │   ├── tasks/                  — TaskList, TaskForm, CompletedTasks, TaskSettings, TaskFilter
│   │   ├── timeline/              — ActivityTimeline, ActivityTimelineViewModel
│   │   ├── settings/              — Settings, ExportImport, DeleteAllData
│   │   ├── onboarding/            — OnboardingScreen, OnboardingViewModel
│   │   ├── familygroup/           — FamilyGroup, Pairing e Transfer
│   │   └── quickadd/              — QuickAddScreen
│   ├── components/                 — composables compartilhados (PetCard, EmptyState, HealthStatusBadge,
│   │                               —   PetitTopAppBar, SpeedDialFab, TimelineEventCard, WeightChart)
│   ├── navigation/                 — Screen routes, PetitNavGraph, PetitBottomNavBar
│   └── util/                       — EnumExtensions, LocalizedEnums
├── ui/
│   └── theme/                      — Color, Typography, Theme (PetitTheme, LightColorScheme, DarkColorScheme)
├── util/                           — LocaleHelper
├── worker/                         — TaskNotificationWorker, TaskScheduler, AutoTaskService
├── di/                             — AppModule, DatabaseModule, FamilyGroupModule, RepositoryModule
└── PetitApplication.kt             — @HiltAndroidApp
```

Esta árvore reflete os pacotes existentes na data da migração. Elementos de
sincronização contínua descritos abaixo permanecem como arquitetura planejada.

---

## Resolução de Conflitos

**Estratégia**: Last-Write-Wins por `updatedAt`

```
Local:  { id: "abc", name: "Luna", updatedAt: 1000 }
Remote: { id: "abc", name: "Luninha", updatedAt: 1500 }

Resultado: Remote vence (updatedAt maior)
```

Sync remoto (Fase N+2, holding) usará Firebase Firestore como transporte.

### Bateria e Protocolos de Sync Local

O sync local (Fase 2) segue regras estritas de bateria:

- **Sync contínuo** usa **NSD + TCP sobre Wi-Fi de infraestrutura** (roteador da casa) — custo ~5-15mW
- **Wi-Fi Direct NUNCA é usado para sync contínuo** — apenas para transferência one-shot
- **Nearby Connections** (Google Play Services) gerencia o transporte do one-shot automaticamente (BLE → BT → Wi-Fi Direct)
- **WorkManager** controla sync em background com constraints de rede
- **NSD é lifecycle-aware**: ativo em foreground, desregistrado ao fechar o app
