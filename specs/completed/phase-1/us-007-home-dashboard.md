# US-007: Dashboard Home

**Prioridade**: P0
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero ver um resumo da saúde dos meus pets na tela inicial,
> Para que eu saiba rapidamente se há algo que precisa de atenção.

---

## Cenários de Aceite

### Cenário 1: Dashboard vazio (primeiro uso)

```gherkin
DADO que abro o app pela primeira vez
E não tenho pets cadastrados
QUANDO vejo a tela inicial
ENTÃO vejo mensagem de boas-vindas
E vejo botão "Cadastrar primeiro pet"
```

### Cenário 2: Dashboard com pets saudáveis

```gherkin
DADO que tenho 2 pets cadastrados
E todos estão com vacinas e vermífugos em dia
QUANDO vejo a tela inicial
ENTÃO vejo cards resumidos de cada pet
E o status de cada card exibe "OK" implicitamente
```

> ⚠️ **Nota:** O banner "Tudo em ordem! 🎉" descrito na especificação oríginal não foi implementado. A saúde é exibida implicitamente via cards.

### Cenário 3: Dashboard com alertas

```gherkin
DADO que Luna tem vacina atrasada
E Simba tem vermífugo vencendo em 5 dias
QUANDO vejo a tela inicial
ENTÃO vejo seção "⚠️ Atenção necessária"
E vejo "Luna: Vacina V3 atrasada"
E vejo "Simba: Vermífugo em 5 dias"
E posso tocar para ir direto ao detalhe
```

### Cenário 4: Resumo rápido de cada pet

```gherkin
DADO que tenho pets cadastrados
QUANDO vejo a tela inicial
ENTÃO cada pet mostra:
  - Foto e nome
  - Último peso registrado
  - Status geral (OK / Atenção / Atrasado)
  - Próximo evento (vacina, vermífugo, etc.)
```

### Cenário 5: Acesso rápido às ações

```gherkin
DADO que estou na home
QUANDO toco em um pet
ENTÃO vou para o perfil completo do pet

QUANDO toco no botão (+) da bottom nav bar
ENTÃO abro a tela QuickAdd com 5 opções:
  - "Registrar pesagem"
  - "Registrar vacina"
  - "Registrar vermífugo"
  - "Novo lembrete" (cria Task personalizada)
  - "Cadastrar novo pet"
```

> **Nota:** O Quick Add é uma tela separada (`quick-add`), não um Speed Dial overlay. Quando a ação requer escolher um pet, abre `PetSelectionScreen` antes do formulário.

### Cenário 6: Próximas tarefas e timeline de atividade

```gherkin
DADO que tenho tarefas agendadas
QUANDO vejo a home
ENTÃO vejo seção "Next Tasks" com até 5 tarefas próximas
E posso tocar "SEE ALL" para ir para TaskListScreen

DADO que tenho eventos registrados (vacinas, pesagens, etc.)
QUANDO vejo a home
ENTÃO vejo a seção de timeline com atividades recentes
E posso tocar "SEE ALL" para ir para ActivityTimelineScreen
```

---

## UI/UX

### Tela: Home (Estado Normal)

```
┌────────────────────────────────┐
│ 🐱 Petit                    ⚙️    │
├────────────────────────────────┤
│                                │
│ ⚠️ ATENÇÃO NECESSÁRIA          │
│ ┌────────────────────────────┐ │
│ │ 🔴 Luna: Vacina V3 atrasada│ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ⚠️ Simba: Vermífugo em 5d  │ │
│ └────────────────────────────┘ │
│                                │
│ 🐾 MEUS PETS                   │
│ ┌──────────────────────────────┐
│ │ ┌────┐  Luna                 │
│ │ │ 📷 │  3.5 kg • ⚠️ Atenção  │
│ │ └────┘  Próx: Vacina V3      │
│ └──────────────────────────────┘
│ ┌──────────────────────────────┐
│ │ ┌────┐  Simba                │
│ │ │ 📷 │  4.2 kg • ✅ OK       │
│ │ └────┘  Próx: Vermífugo 5d   │
│ └──────────────────────────────┘
│                                │
│ 📅 PRÓXIMOS                    │
│ • 20/03 Vacina V3 - Luna       │
│ • 25/03 Vermífugo - Simba      │
│                                │
│                           [+]  │
└────────────────────────────────┘
```

### Tela: Home (Primeiro Uso)

```
┌────────────────────────────────┐
│ 🐱 Petit                    ⚙️    │
├────────────────────────────────┤
│                                │
│                                │
│         🐱                     │
│                                │
│    Bem-vindo ao Petit!           │
│                                │
│    Cadastre seu primeiro       │
│    pet para começar        │
│                                │
│ ┌────────────────────────────┐ │
│ │      CADASTRAR PET         │ │
│ └────────────────────────────┘ │
│                                │
│                                │
└────────────────────────────────┘
```

### Tela: Home (Tudo OK)

> ⚠️ **NÃO IMPLEMENTADO.** O banner "Tudo em ordem! 🎉" não foi adicionado. Saúde é exibida implicitamente via cards de pet.

### Speed Dial (obsoleto — substituído pelo QuickAdd)

> ⚠️ O Speed Dial overlay não foi implementado. O `SpeedDialFab.kt` existe como componente reutilizável mas não é usado na HomeScreen. O [+] da bottom nav direciona para `QuickAddScreen`.

---

## Requisitos Técnicos

### ViewModel

```kotlin
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val pets: List<PetWithSummary> = emptyList(),
    val tasksDueToday: List<Task> = emptyList(),
    val tasksDueThisWeek: List<Task> = emptyList(),
    val tasksDueThisMonth: List<Task> = emptyList(),
    val nextTasks: List<Task> = emptyList(),
    val hasMoreTasks: Boolean = false,
    val recentActivity: List<TimelineEvent> = emptyList(),
    val upcomingTimeline: List<TimelineEvent> = emptyList()
)
```

### Repositórios utilizados pela HomeViewModel

- `PetRepository.getAllPets()` — Flow de pets ativos
- `TaskRepository.getPendingTasks()` — Flow de tarefas PENDING (incluindo atrasadas)
- `TimelineRepository.getRecentActivity()` — eventos recentes agregados
- `TimelineRepository.getUpcomingEvents(daysAhead)` — próximos eventos
- `WeightEntryRepository.getLatestWeightEntry(petId)` — último peso por pet
- `GetPetHealthSummaryAction.execute(petId)` — resumo de peso, vacina e vermifugação

### Telas adicionais implementadas na Fase 1

| Tela                   | Rota                | Descrição                                                                           |
| ---------------------- | ------------------- | ----------------------------------------------------------------------------------- |
| ActivityTimelineScreen | `activity-timeline` | Timeline completa com filtro por período (5/10/15 dias ou personalizado) e por pet |
| QuickAddScreen         | `quick-add`         | 5 ações rápidas: peso, vacina, vermífugo, lembrete, novo pet                       |

---

## Definition of Done

- [x] Tela de boas-vindas para primeiro uso
- [x] Lista de pets com resumo
- [x] Indicação visual de status por pet (via cards)
- [x] Seção "Next Tasks" (próximas tarefas)
- [x] Seção de timeline de atividade recente
- [x] QuickAdd com 5 ações rápidas
- [x] Navegação para perfil do pet
- [x] Acesso rápido a configurações
- [x] Pull-to-refresh
- [ ] Banner "Tudo em ordem" — **não implementado** (futuro)
- [ ] Seção de alertas separada — **não implementado** (futuro)
