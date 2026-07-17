# US-103: Vinculação de Dados

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Autenticação  
**Fase**: 2

---

## História

> Como usuário que acabou de fazer login,  
> Eu quero que meus dados locais sejam vinculados à minha conta,  
> Para que futuramente eu possa sincronizá-los na nuvem.

---

## Cenários de Aceite

### Cenário 1: Primeiro login vincula dados existentes

```gherkin
DADO que tenho 2 pets cadastrados localmente
E nunca fiz login antes
QUANDO faço login com "pessoa-a@example.com"
ENTÃO meus 2 pets são marcados com ownerId = meu userId
E posso continuar usando normalmente
```

### Cenário 2: Dados criados após login já vêm vinculados

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO cadastro um novo pet "Luna"
ENTÃO Luna é criada com ownerId = meu userId
```

### Cenário 3: Dados em modo anônimo não têm owner

```gherkin
DADO que estou usando sem login
QUANDO cadastro um pet "Simba"
ENTÃO Simba é criado com ownerId = null
```

### Cenário 4: Logout não remove vinculação

```gherkin
DADO que tenho pets vinculados ao meu userId
QUANDO faço logout
ENTÃO os pets continuam com o ownerId preenchido
E continuam visíveis no app
```

### Cenário 5: Login com conta diferente

```gherkin
DADO que tenho pets do "user-a" no dispositivo
E faço login como "pessoa-b@example.com" (user-b)
ENTÃO vejo os pets da Pessoa A (dados locais)
MAS eles continuam com ownerId = user-a
E novos dados criados terão ownerId = user-b
(gestão de múltiplos owners é tratada em sync futuro)
```

---

## Modelo de Dados

### Adição de ownerId nas entidades

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // Novo campo
    val name: String,
    // ... outros campos
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,  // Novo campo
    val petId: String,
    // ... outros campos
)

// Similar para VaccinationEntry, DewormingEntry, Reminder
```

---

## Requisitos Técnicos

### Migration do Room

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Adicionar coluna ownerId em todas as tabelas
        database.execSQL("ALTER TABLE pets ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE vaccination_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE deworming_entries ADD COLUMN ownerId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE reminders ADD COLUMN ownerId TEXT DEFAULT NULL")
    }
}
```

### DataOwnershipManager

```kotlin
class DataOwnershipManager(
    private val petDao: PetDao,
    private val weightDao: WeightEntryDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val reminderDao: ReminderDao,
    private val database: PetitDatabase
) {
    /**
     * Vincula todos os dados locais sem owner ao userId atual
     * Chamado após primeiro login
     */
    suspend fun claimOrphanedData(userId: String) {
        database.withTransaction {
            petDao.claimOrphanedPets(userId)
            weightDao.claimOrphanedEntries(userId)
            vaccinationDao.claimOrphanedEntries(userId)
            dewormingDao.claimOrphanedEntries(userId)
            reminderDao.claimOrphanedEntries(userId)
        }
    }
}
```

### DAOs atualizados

```kotlin
@Dao
interface PetDao {
    // Queries existentes...
    
    @Query("UPDATE pets SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun claimOrphanedPets(userId: String)
    
    @Query("SELECT * FROM pets WHERE (ownerId = :userId OR ownerId IS NULL) AND deletedAt IS NULL ORDER BY name")
    fun getPetsForUser(userId: String?): Flow<List<PetEntity>>
}
```

### Integração no Login Flow

```kotlin
class AuthRepositoryImpl(...) {
    
    override suspend fun signInWithGoogle(idToken: String): Result<UserInfo> {
        return try {
            // ... código de login existente ...
            
            val user = firebaseAuth.currentUser!!
            
            // Verificar se é primeiro login
            val isFirstLogin = userPreferencesRepository.getFirstLoginDate() == null
            
            if (isFirstLogin) {
                // Vincular dados órfãos ao novo usuário
                dataOwnershipManager.claimOrphanedData(user.id)
                userPreferencesRepository.setFirstLoginDate(System.currentTimeMillis())
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Criar entidades com owner

```kotlin
class CreatePetUseCase(
    private val petRepository: PetRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pet: Pet): Result<Pet> {
        val ownerId = authRepository.getCurrentUser()?.id
        
        val petWithOwner = pet.copy(ownerId = ownerId)
        
        return petRepository.insertPet(petWithOwner)
    }
}
```

---

## Visualização de Dados

### Fase 2: Mostrar todos os dados locais

```kotlin
// Por enquanto, mostrar todos os dados locais independente do owner
fun getAllPets(): Flow<List<PetEntity>> {
    return petDao.getAllPets()  // Sem filtro por owner
}
```

### Fase futura (5): Filtrar por owner para sync

```kotlin
// Quando implementar sync, filtrar por owner
fun getPetsForSync(userId: String): Flow<List<PetEntity>> {
    return petDao.getPetsForUser(userId)
}
```

---

## Definition of Done

- [ ] Campo ownerId adicionado em todas as entidades
- [ ] Migration do Room implementada
- [ ] Dados criados após login têm ownerId preenchido
- [ ] Primeiro login vincula dados órfãos
- [ ] Logout não altera ownerId dos dados
- [ ] Testes de migration
- [ ] Testes de vinculação
