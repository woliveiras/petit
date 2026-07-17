# US-401: Sincronização em Tempo Real

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Cloud Sync  
**Fase**: 5

---

## História

> Como usuário premium,  
> Eu quero que meus dados sincronizem automaticamente com a nuvem,  
> Para que eles estejam sempre atualizados e disponíveis em qualquer dispositivo.

---

## Cenários de Aceite

### Cenário 1: Sync após criar dados

```gherkin
DADO que sou usuário premium com sync ativado
E tenho conexão de internet
QUANDO cadastro um novo pet "Luna"
ENTÃO Luna é salva no Room imediatamente (syncStatus = PENDING)
E após alguns segundos, Luna é enviada para o Firestore
E o syncStatus muda para SYNCED
E vejo indicador de sync ✓
```

### Cenário 2: Sync em tempo real recebendo dados

```gherkin
DADO que tenho o app aberto
E alguém (ou outro dispositivo) adiciona dados no Firestore
QUANDO a mudança é detectada pelo snapshot listener do Firestore
ENTÃO os novos dados são baixados automaticamente
E salvos no Room local
E aparecem na UI sem precisar atualizar manualmente
```

### Cenário 3: Sync sem internet (queue)

```gherkin
DADO que estou sem internet
QUANDO cadastro um novo pet
ENTÃO o pet é salvo no Room (syncStatus = PENDING)
E o pet aparece na UI normalmente
E quando a internet voltar, o sync acontece automaticamente
```

### Cenário 4: Ativar sync pela primeira vez

```gherkin
DADO que tenho dados locais
E nunca sincronizei antes
QUANDO ativo "Sincronização na nuvem" nas configurações
ENTÃO todos os dados locais são enviados para o Firestore
E vejo progresso "Sincronizando X de Y itens..."
E ao final, todos estão com syncStatus = SYNCED
```

### Cenário 5: Premium expira

```gherkin
DADO que meu premium expira
QUANDO isso acontece
ENTÃO o snapshot listener do Firestore é desconectado
E novos dados são salvos apenas localmente (syncStatus = LOCAL_ONLY)
E os dados já sincronizados permanecem no dispositivo
E vejo aviso "Sincronização pausada - Renove seu premium"
```

---

## UI/UX

### Indicador de Sync na Toolbar

```
┌────────────────────────────────┐
│ 🐱 Petit                 ☁️✓  ⚙️  │  ← Sync OK
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️⟳  ⚙️  │  ← Sincronizando
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️!  ⚙️  │  ← Pendente (sem internet)
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘
```

### Configuração de Sync

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ ☁️ SINCRONIZAÇÃO NA NUVEM      │
│ ┌────────────────────────────┐ │
│ │ Ativar                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são sincronizados│
│ automaticamente entre todos   │
│ os seus dispositivos.         │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Sincronizado            │ │
│ │ Última sync: há 2 min      │ │
│ │                            │ │
│ │ 2 pets • 15 pesagens      │ │
│ │ 8 vacinas • 6 vermífugos   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ OPÇÕES                      │
│ ┌────────────────────────────┐ │
│ │ Sync apenas em Wi-Fi [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FORÇAR SYNC COMPLETO    │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### SyncEngine

```kotlin
interface SyncEngine {
    val syncState: StateFlow<SyncState>
    
    fun startSync()
    fun stopSync()
    suspend fun syncNow(): Result<SyncResult>
    suspend fun uploadPending(): Result<Int>
    suspend fun downloadAll(): Result<Int>
}

sealed class SyncState {
    object Disabled : SyncState()
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int
)
```

### SyncEngineImpl

```kotlin
class SyncEngineImpl(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val syncPreferences: SyncPreferencesRepository
) : SyncEngine {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disabled)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun startSync() {
        val userId = authRepository.getCurrentUser()?.id ?: return
        
        _syncState.value = SyncState.Idle

        // Upload pendentes primeiro
        scope.launch {
            uploadPending()
        }

        // Iniciar Firestore snapshot listeners para cada coleção
        startPetsListener(userId)
        startWeightsListener(userId)
        startVaccinationsListener(userId)
        startDewormingsListener(userId)
    }

    private fun startPetsListener(userId: String) {
        val registration = firestore.collection("pets")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> handlePetUpsert(change.document)
                            DocumentChange.Type.REMOVED -> handlePetDelete(change.document.id)
                        }
                    }
                }
            }
        listenerRegistrations.add(registration)
    }

    private suspend fun handlePetUpsert(document: DocumentSnapshot) {
        val remotePet = document.toPetEntity() ?: return
        val localPet = petDao.getPetById(remotePet.id)

        // Last-write-wins: atualizar se remoto for mais recente
        if (localPet == null || remotePet.updatedAt > localPet.updatedAt) {
            petDao.insertPet(remotePet.copy(syncStatus = "SYNCED"))
        }
    }

    private suspend fun handlePetDelete(petId: String) {
        val localPet = petDao.getPetById(petId)
        if (localPet != null && localPet.deletedAt == null) {
            petDao.softDeletePet(localPet.id)
        }
    }

    override suspend fun uploadPending(): Result<Int> {
        val userId = authRepository.getCurrentUser()?.id 
            ?: return Result.failure(Exception("Not logged in"))

        var uploadedCount = 0

        // Upload pets pendentes
        petDao.getPendingSyncPets().collect { pets ->
            pets.forEach { pet ->
                try {
                    firestore.collection("pets")
                        .document(pet.id)
                        .set(pet.toFirestoreMap(), SetOptions.merge())
                        .await()

                    petDao.updateSyncStatus(pet.id, "SYNCED")
                    uploadedCount++
                } catch (e: Exception) {
                    // Manter como PENDING para tentar depois
                }
            }
        }

        // Similar para outras entidades...

        return Result.success(uploadedCount)
    }

    override fun stopSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        _syncState.value = SyncState.Disabled
    }
}
```

### Firestore Models

```kotlin
data class PetFirestoreModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthDate: Long? = null,
    val sex: String? = null,
    val microchipNumber: String? = null,
    val passportNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null
) {
    fun toEntity() = PetEntity(
        id = id,
        ownerId = userId,
        name = name,
        birthDate = birthDate,
        sex = sex,
        microchipNumber = microchipNumber,
        passportNumber = passportNumber,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = "SYNCED"
    )
}

fun PetEntity.toFirestoreMap() = mapOf(
    "id" to id,
    "userId" to (ownerId ?: ""),
    "name" to name,
    "birthDate" to birthDate,
    "sex" to sex,
    "microchipNumber" to microchipNumber,
    "passportNumber" to passportNumber,
    "notes" to notes,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "deletedAt" to deletedAt
)

fun DocumentSnapshot.toPetEntity(): PetEntity? {
    return try {
        val model = toObject(PetFirestoreModel::class.java) ?: return null
        model.toEntity()
    } catch (e: Exception) {
        null
    }
}
```

### DAO Updates

```kotlin
@Dao
interface PetDao {
    // Existing queries...

    @Query("SELECT * FROM pets WHERE syncStatus = 'PENDING_SYNC' AND deletedAt IS NULL")
    fun getPendingSyncPets(): Flow<List<PetEntity>>

    @Query("UPDATE pets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
```

---

## Definition of Done

- [ ] SyncEngine inicia/para corretamente
- [ ] Firestore snapshot listeners funcionam
- [ ] Upload de dados pendentes funciona
- [ ] Download de dados remotos funciona
- [ ] syncStatus atualizado corretamente
- [ ] Indicador visual de sync na UI
- [ ] Configuração de sync nas settings
- [ ] Premium gate aplicado
- [ ] Erros de sync tratados
- [ ] Testes unitários do SyncEngine
- [ ] Testes de integração com Firebase Emulator Suite
