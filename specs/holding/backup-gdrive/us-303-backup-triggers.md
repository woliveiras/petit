# US-303: Triggers de Backup

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Auto Sync  
**Fase**: 4

---

## História

> Como usuário premium,  
> Eu quero que o backup seja feito automaticamente após eu fazer alterações importantes,  
> Para que meus dados mais recentes estejam sempre protegidos.

---

## Cenários de Aceite

### Cenário 1: Backup após criar pet

```gherkin
DADO que backup automático está ativado
E tenho conexão de internet
QUANDO cadastro um novo pet
ENTÃO após 5 minutos de inatividade
O backup é executado automaticamente
E inclui o novo pet
```

### Cenário 2: Debounce de múltiplas alterações

```gherkin
DADO que faço várias alterações em sequência:
  - Adiciono pet Luna
  - Adiciono pesagem 3.5kg
  - Adiciono vacina V3
  - Tudo em menos de 5 minutos
ENTÃO apenas UM backup é executado
(após 5 minutos da última alteração)
E inclui todas as mudanças
```

### Cenário 3: Backup após delete

```gherkin
DADO que backup automático está ativado
QUANDO deleto um pet
ENTÃO após 5 minutos sem alterações
O backup é executado
E reflete a exclusão
```

### Cenário 4: Cancelar backup pendente

```gherkin
DADO que alterei dados e backup está pendente (em 3 min)
QUANDO faço outra alteração
ENTÃO o timer é resetado para 5 minutos novamente
E apenas um backup será feito
```

### Cenário 5: Não duplicar com backup periódico

```gherkin
DADO que um backup por alteração está pendente
E o backup periódico deveria executar agora
ENTÃO apenas um backup é feito
E o timer de backup por alteração é cancelado
```

### Cenário 6: App fechado após alteração

```gherkin
DADO que fiz alterações
E fecho o app imediatamente
ENTÃO o backup pendente ainda será executado
(WorkManager persiste a tarefa)
```

---

## Triggers de Backup

| Evento | Trigger? | Debounce |
|--------|----------|----------|
| Criar pet | ✅ | 5 min |
| Editar pet | ✅ | 5 min |
| Deletar pet | ✅ | 5 min |
| Adicionar pesagem | ✅ | 5 min |
| Adicionar vacina | ✅ | 5 min |
| Adicionar vermífugo | ✅ | 5 min |
| Criar lembrete | ❌ | - |
| Editar configurações | ❌ | - |

---

## Arquitetura

### Fluxo de Trigger

```
┌─────────────────────────────────────────────────────────────┐
│                           App                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PetRepository        WeightRepository       VaccinationRepo│
│  ┌───────────┐        ┌───────────┐         ┌───────────┐  │
│  │  insert() │        │  insert() │         │  insert() │  │
│  └─────┬─────┘        └─────┬─────┘         └─────┬─────┘  │
│        │                    │                     │         │
│        └────────────────────┼─────────────────────┘         │
│                             │                               │
│                             ▼                               │
│                   ┌─────────────────┐                       │
│                   │ BackupTrigger   │                       │
│                   │   Manager       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │   Debouncer     │                       │
│                   │   (5 min)       │                       │
│                   └────────┬────────┘                       │
│                            │                                │
│                            ▼                                │
│                   ┌─────────────────┐                       │
│                   │  WorkManager    │                       │
│                   │ OneTimeRequest  │                       │
│                   └─────────────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Requisitos Técnicos

### BackupTriggerManager

```kotlin
class BackupTriggerManager(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository,
    private val premiumRepository: PremiumRepository
) {
    companion object {
        const val DEBOUNCE_DELAY_MINUTES = 5L
        const val WORK_TAG = "backup_on_change"
    }

    fun onDataChanged() {
        // Verificar se backup automático está ativado e é premium
        if (!backupPreferences.isAutoBackupEnabled() || !premiumRepository.isPremium()) {
            return
        }

        // Cancelar trabalho pendente anterior (debounce)
        workManager.cancelAllWorkByTag(WORK_TAG)

        // Agendar novo trabalho com delay
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly())
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BackupOnChangeWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelPendingBackup() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    fun hasPendingBackup(): Boolean {
        val workInfos = workManager.getWorkInfosByTag(WORK_TAG).get()
        return workInfos.any { !it.state.isFinished }
    }
}
```

### BackupOnChangeWorker

```kotlin
class BackupOnChangeWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Verificar premium (pode ter expirado enquanto esperava)
        if (!premiumRepository.isPremium()) {
            return Result.success()  // Não é falha, apenas skip
        }

        // Executar backup
        return backupUseCase()
            .map {
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
                backupPreferences.setLastBackupError(null)
                Result.success()
            }
            .getOrElse { error ->
                // Não fazer retry para backup por alteração
                // O próximo periódico tentará novamente
                backupPreferences.setLastBackupError(error.message)
                Result.success()  // Marcar como sucesso para não ficar tentando
            }
    }
}
```

### Integração nos Repositories

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val backupTriggerManager: BackupTriggerManager
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        petDao.insertPet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun updatePet(pet: PetEntity) {
        petDao.updatePet(pet)
        backupTriggerManager.onDataChanged()
    }

    override suspend fun deletePet(id: String) {
        petDao.softDeletePet(id)
        backupTriggerManager.onDataChanged()
    }
}
```

### Usando Callback/Listener Pattern

```kotlin
// Alternativa: usar padrão de eventos
interface DataChangeListener {
    fun onDataChanged(entityType: EntityType)
}

enum class EntityType {
    PET, WEIGHT, VACCINATION, DEWORMING
}

class DataChangePublisher {
    private val listeners = mutableListOf<DataChangeListener>()

    fun addListener(listener: DataChangeListener) {
        listeners.add(listener)
    }

    fun notifyDataChanged(entityType: EntityType) {
        listeners.forEach { it.onDataChanged(entityType) }
    }
}

// BackupTriggerManager implementa DataChangeListener
class BackupTriggerManager(...) : DataChangeListener {
    override fun onDataChanged(entityType: EntityType) {
        // Trigger backup com debounce
        onDataChanged()
    }
}
```

### Evitar Conflito com Backup Periódico

```kotlin
class AutoBackupWorker(...) : CoroutineWorker(...) {
    
    override suspend fun doWork(): Result {
        // Cancelar backup por alteração pendente (evitar duplicação)
        WorkManager.getInstance(applicationContext)
            .cancelAllWorkByTag(BackupTriggerManager.WORK_TAG)
        
        // Continuar com backup normal...
        return backupUseCase()
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }
}
```

---

## UI Feedback (Opcional)

### Indicador Sutil

Não mostrar nada visualmente. O backup por alteração é "invisível" para o usuário, apenas garante que dados estão protegidos.

### Para Debug/Desenvolvimento

```kotlin
// Apenas em debug builds
if (BuildConfig.DEBUG && hasPendingBackup()) {
    Snackbar.make(
        view,
        "Backup pendente em ${getRemainingTime()} min",
        Snackbar.LENGTH_SHORT
    ).show()
}
```

---

## Definition of Done

- [ ] BackupTriggerManager implementado
- [ ] Debounce de 5 minutos funciona
- [ ] Trigger após criar/editar/deletar entidades
- [ ] Não dispara para lembretes/configurações
- [ ] Cancelamento de backup pendente ao fazer nova alteração
- [ ] Não duplica com backup periódico
- [ ] Funciona com app em background
- [ ] Verificação de premium antes de executar
- [ ] Testes unitários do debounce
- [ ] Testes de integração
