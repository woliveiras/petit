# US-402: Múltiplos Dispositivos

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Cloud Sync  
**Fase**: 5

---

## História

> Como usuário premium com múltiplos dispositivos,  
> Eu quero que meus dados estejam disponíveis em todos eles,  
> Para que eu possa acessar e editar de qualquer lugar.

---

## Cenários de Aceite

### Cenário 1: Segundo dispositivo recebe dados

```gherkin
DADO que tenho dados no dispositivo A
E instalo o app no dispositivo B
QUANDO faço login no dispositivo B
E ativo a sincronização
ENTÃO todos os meus dados são baixados do Firestore
E vejo os mesmos pets que no dispositivo A
```

### Cenário 2: Edição aparece em tempo real

```gherkin
DADO que tenho o app aberto no dispositivo A e B
QUANDO edito o nome do pet para "Luninha" no dispositivo A
ENTÃO em poucos segundos, o dispositivo B mostra "Luninha"
Sem precisar atualizar manualmente
```

### Cenário 3: Criar em um, ver em outro

```gherkin
DADO que adiciono um novo pet "Simba" no dispositivo A
QUANDO o sync completa
ENTÃO o dispositivo B recebe "Simba" automaticamente
E Simba aparece na lista de pets
```

### Cenário 4: Deletar em um, reflete em outro

```gherkin
DADO que deleto o pet "Simba" no dispositivo A
QUANDO o sync completa
ENTÃO o dispositivo B também não mostra mais "Simba"
```

### Cenário 5: Dispositivo offline vs online

```gherkin
DADO que dispositivo A está offline
E dispositivo B adiciona pet "Mia"
QUANDO dispositivo A volta online
ENTÃO dispositivo A recebe "Mia" automaticamente
```

---

## UI/UX

### Indicador de Dispositivos

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ 📱 DISPOSITIVOS CONECTADOS     │
│ ┌────────────────────────────┐ │
│ │ 📱 Este dispositivo        │ │
│ │    Galaxy S24 • Online     │ │
│ │                            │ │
│ │ 📱 Outro dispositivo       │ │
│ │    Pixel 8 • Há 5 min      │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são sincronizados│
│ automaticamente entre todos   │
│ os dispositivos logados.      │
│                                │
└────────────────────────────────┘
```

### Primeiro Sync em Novo Dispositivo

```
┌────────────────────────────────┐
│                                │
│         ☁️ ↓                   │
│                                │
│   Sincronizando seus dados...  │
│                                │
│   ████████░░░░░░  60%          │
│                                │
│   Baixando: 2 pets            │
│             15 pesagens        │
│             8 vacinas          │
│                                │
│   Mantenha a conexão ativa     │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Download Inicial

```kotlin
class InitialSyncUseCase(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val database: PetitDatabase
) {
    sealed class Progress {
        object Starting : Progress()
        data class Downloading(val entity: String, val current: Int, val total: Int) : Progress()
        object Complete : Progress()
        data class Error(val message: String) : Progress()
    }

    suspend fun execute(): Flow<Progress> = flow {
        emit(Progress.Starting)
        
        val userId = authRepository.getCurrentUser()?.id 
            ?: throw Exception("Not logged in")

        database.withTransaction {
            // Download pets
            val petsSnapshot = firestore.collection("pets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("deletedAt", null)
                .get().await()
            val pets = petsSnapshot.documents.mapNotNull { it.toPetEntity() }
            
            emit(Progress.Downloading("pets", pets.size, pets.size))
            pets.forEach { petDao.insertPet(it) }

            // Download weights
            val weightsSnapshot = firestore.collection("weight_entries")
                .whereEqualTo("userId", userId)
                .get().await()
            val weights = weightsSnapshot.documents.mapNotNull { it.toWeightEntity() }
            
            emit(Progress.Downloading("pesagens", weights.size, weights.size))
            weights.forEach { weightDao.upsertWeightEntry(it) }

            // Similar para vaccinations, dewormings...
        }

        emit(Progress.Complete)
    }.catch { e ->
        emit(Progress.Error(e.message ?: "Erro no sync"))
    }
}
```

### Tracking de Dispositivos

```kotlin
// No Firestore: coleção devices
// devices (user_id, device_id, device_name, last_seen_at, app_version)
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val lastSeenAt: Long = 0,
    val appVersion: String = ""
)

class DeviceTracker(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun registerDevice() {
        val userId = authRepository.getCurrentUser()?.id ?: return
        
        val deviceInfo = mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "deviceName" to Build.MODEL,
            "lastSeenAt" to System.currentTimeMillis(),
            "appVersion" to BuildConfig.VERSION_NAME
        )

        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .set(deviceInfo, SetOptions.merge())
            .await()
    }

    suspend fun updateLastSeen() {
        val userId = authRepository.getCurrentUser()?.id ?: return
        
        firestore.collection("devices")
            .document("${userId}_${deviceId}")
            .update("lastSeenAt", System.currentTimeMillis())
            .await()
    }

    fun getConnectedDevices(): Flow<List<DeviceInfo>> {
        val userId = authRepository.getCurrentUser()?.id ?: return flowOf(emptyList())
        
        return callbackFlow {
            val registration = firestore.collection("devices")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    val devices = snapshot?.documents?.mapNotNull {
                        it.toObject(DeviceInfo::class.java)
                    } ?: emptyList()
                    trySend(devices)
                }
            awaitClose { registration.remove() }
        }
    }
}
```

### Heartbeat para Last Seen

```kotlin
class SyncHeartbeatWorker(
    context: Context,
    params: WorkerParameters,
    private val deviceTracker: DeviceTracker
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        deviceTracker.updateLastSeen()
        return Result.success()
    }

    companion object {
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncHeartbeatWorker>(
                15, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "sync_heartbeat",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
```

---

## Resolução de Conflitos Multi-Device

Quando dois dispositivos editam o mesmo dado simultaneamente:

```kotlin
suspend fun handleIncomingChange(remote: PetFirestoreModel) {
    val local = petDao.getPetById(remote.id)
    
    when {
        // Novo remotamente
        local == null -> {
            petDao.insertPet(remote.toEntity())
        }
        // Remoto mais recente: aceitar
        remote.updatedAt > local.updatedAt -> {
            petDao.updatePet(remote.toEntity())
        }
        // Local mais recente: manter local e re-upload
        local.updatedAt > remote.updatedAt && local.syncStatus == "SYNCED" -> {
            // Local é mais recente mas já foi marcado como synced
            // Isso significa que a mudança local ainda não foi enviada
            // Manter local e enviar para nuvem
            uploadToFirestore(local)
        }
        // Mesmo timestamp: considerar empate, manter local
        else -> {
            // Não fazer nada, local já está correto
        }
    }
}
```

---

## Definition of Done

- [ ] Download inicial funciona em novo dispositivo
- [ ] Progresso de sync exibido
- [ ] Alterações refletem em tempo real
- [ ] Lista de dispositivos conectados
- [ ] Heartbeat atualiza lastSeen
- [ ] Conflitos resolvidos automaticamente
- [ ] Deletions propagam entre devices
- [ ] Testes com múltiplos emuladores
