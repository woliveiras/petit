# US-404: Sync Offline-First

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Cloud Sync  
**Fase**: 5

---

## História

> Como usuário premium,  
> Eu quero que o app funcione normalmente mesmo offline,  
> Para que eu possa registrar dados sem conexão e eles sincronizem depois.

---

## Cenários de Aceite

### Cenário 1: Criar dados offline

```gherkin
DADO que estou sem internet
QUANDO cadastro um novo pet "Mia"
ENTÃO Mia é salva no Room (syncStatus = PENDING_SYNC)
E Mia aparece na lista normalmente
E vejo indicador "Pendente de sync" no item
```

### Cenário 2: Sync automático ao reconectar

```gherkin
DADO que tenho dados pendentes de sync
E estou offline
QUANDO a internet volta
ENTÃO o sync é iniciado automaticamente
E os dados pendentes são enviados
E o syncStatus muda para SYNCED
E o indicador de pendente desaparece
```

### Cenário 3: Múltiplas edições offline

```gherkin
DADO que estou offline
QUANDO faço várias edições:
  - Adiciono pet Mia
  - Adiciono pesagem para Mia
  - Edito nome de Luna para Luninha
ENTÃO todas as edições são salvas localmente
E todas ficam como PENDING_SYNC
E ao reconectar, todas são enviadas
```

### Cenário 4: Conflito após voltar online

```gherkin
DADO que editei Luna offline (updatedAt = 1000)
E outro dispositivo editou Luna online (updatedAt = 1500)
QUANDO volto online e sincronizo
ENTÃO a resolução de conflito acontece
E a versão mais recente (1500) vence
```

### Cenário 5: Queue de sync persiste após fechar app

```gherkin
DADO que fiz edições offline
E fecho o app
E reabro o app (ainda offline)
ENTÃO as edições ainda estão PENDING_SYNC
E ao reconectar, serão sincronizadas
```

---

## UI/UX

### Indicador em Item Pendente

```
┌────────────────────────────────┐
│ ← Meus Gatinhos                │
├────────────────────────────────┤
│ ┌──────────────────────────────┐
│ │ ┌────┐  Luna            ☁️✓  │  ← Synced
│ │ │ 📷 │  3.5 kg               │
│ │ └────┘                       │
│ └──────────────────────────────┘
│ ┌──────────────────────────────┐
│ │ ┌────┐  Mia             ☁️⏳  │  ← Pending
│ │ │ 📷 │  Novo                 │
│ │ └────┘                       │
│ └──────────────────────────────┘
└────────────────────────────────┘
```

### Banner de Status Offline

```
┌────────────────────────────────┐
│ ⚠️ Sem conexão                 │
│ Alterações serão sincronizadas │
│ quando a internet voltar.      │
└────────────────────────────────┘
┌────────────────────────────────┐
│ 🐱 Petit                    ⚙️    │
├────────────────────────────────┤
│ ...                            │
```

### Status de Sync com Detalhes

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS DA SYNC              │
│ ┌────────────────────────────┐ │
│ │ ⚠️ 3 itens pendentes       │ │
│ │                            │ │
│ │ • 1 pet novo              │ │
│ │ • 1 pesagem                │ │
│ │ • 1 vacina editada         │ │
│ │                            │ │
│ │ Aguardando conexão...      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Network Listener

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        
        // Estado inicial
        val isCurrentlyOnline = connectivityManager.activeNetwork != null
        trySend(isCurrentlyOnline)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
}
```

### Auto-Sync on Reconnect

```kotlin
class SyncOnReconnectManager(
    private val networkMonitor: NetworkMonitor,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .filter { isOnline -> isOnline }  // Apenas quando volta online
                .collect {
                    if (premiumRepository.isPremium()) {
                        syncEngine.uploadPending()
                    }
                }
        }
    }
}
```

### Contagem de Pendentes

```kotlin
class PendingSyncCounter(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao
) {
    data class PendingCount(
        val pets: Int = 0,
        val weights: Int = 0,
        val vaccinations: Int = 0,
        val dewormings: Int = 0
    ) {
        val total: Int get() = pets + weights + vaccinations + dewormings
        val isEmpty: Boolean get() = total == 0
    }

    fun getPendingCount(): Flow<PendingCount> = combine(
        petDao.countPendingSync(),
        weightDao.countPendingSync(),
        vaccinationDao.countPendingSync(),
        dewormingDao.countPendingSync()
    ) { pets, weights, vaccinations, dewormings ->
        PendingCount(pets, weights, vaccinations, dewormings)
    }
}

// DAOs
@Query("SELECT COUNT(*) FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
fun countPendingSync(): Flow<Int>
```

### Firestore Offline Handling

```kotlin
// Firestore tem persistência offline nativa (isPersistenceEnabled = true).
// Room continua sendo a fonte de verdade para queries locais.
// Dados pendentes ficam em Room com syncStatus = PENDING_SYNC
// e são enviados via WorkManager quando a conexão volta.
```

### WorkManager para Sync Pendente

```kotlin
class UploadPendingWorker(
    context: Context,
    params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val premiumRepository: PremiumRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!premiumRepository.isPremium()) {
            return Result.success()
        }

        return syncEngine.uploadPending()
            .map { Result.success() }
            .getOrElse { Result.retry() }
    }

    companion object {
        fun scheduleIfPending(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadPendingWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniqueWork(
                "upload_pending",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
```

### Marcar Como Pending ao Salvar

```kotlin
class PetRepositoryImpl(
    private val petDao: PetDao,
    private val premiumRepository: PremiumRepository,
    private val syncEngine: SyncEngine
) : PetRepository {

    override suspend fun insertPet(pet: PetEntity) {
        val syncStatus = if (premiumRepository.isPremium()) {
            "PENDING_SYNC"
        } else {
            "LOCAL_ONLY"
        }

        petDao.insertPet(pet.copy(syncStatus = syncStatus))

        // Tentar sync imediato se online
        if (syncStatus == "PENDING_SYNC") {
            syncEngine.uploadPending()
        }
    }
}
```

---

## Definition of Done

- [ ] Dados salvos offline com syncStatus correto
- [ ] Indicador visual de pendente
- [ ] Banner de status offline
- [ ] Auto-sync ao reconectar funciona
- [ ] Contagem de pendentes exibida
- [ ] WorkManager agenda upload quando tem rede
- [ ] Room + WorkManager garante sync offline
- [ ] Queue persiste após fechar app
- [ ] Conflitos resolvidos após reconectar
- [ ] Testes com airplane mode
