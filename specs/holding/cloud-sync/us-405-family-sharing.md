# US-405: Compartilhamento com Família

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P2  
**Épico**: Cloud Sync  
**Fase**: 5

---

## História

> Como usuário premium,  
> Eu quero compartilhar os dados dos meus pets com minha família,  
> Para que todos possamos acompanhar e registrar informações dos pets juntos.

---

## Cenários de Aceite

### Cenário 1: Criar grupo familiar

```gherkin
DADO que sou usuário premium
QUANDO acesso Configurações > "Família"
E toco em "Criar grupo familiar"
ENTÃO um grupo é criado
E eu me torno o administrador
E recebo um código de convite
```

### Cenário 2: Convidar membro

```gherkin
DADO que sou admin de um grupo familiar
QUANDO compartilho o código de convite "PETIT-ABC123"
E outra pessoa insere o código no app dela
ENTÃO ela entra no grupo familiar
E vê todos os pets do grupo
```

### Cenário 3: Todos veem e editam

```gherkin
DADO que Pessoa A e Pessoa B estão no mesmo grupo familiar
QUANDO Pessoa B adiciona uma pesagem para um pet
ENTÃO Pessoa A vê a pesagem automaticamente
E Pessoa A também pode adicionar/editar dados
```

### Cenário 4: Permissões de admin

```gherkin
DADO que sou admin do grupo
QUANDO acesso a lista de membros
ENTÃO posso:
  - Remover membros
  - Gerar novo código de convite
  - Deletar o grupo
```

### Cenário 5: Membro sai do grupo

```gherkin
DADO que sou membro de um grupo familiar
QUANDO escolho "Sair do grupo"
ENTÃO perco acesso aos dados compartilhados
E os dados permanecem para os outros membros
E meus dados pessoais (não compartilhados) permanecem comigo
```

### Cenário 6: Pets privados vs compartilhados

```gherkin
DADO que sou membro de um grupo familiar
QUANDO cadastro um novo pet
ENTÃO posso escolher:
  - "Compartilhar com família" (todos veem)
  - "Manter privado" (só eu vejo)
```

---

## UI/UX

### Tela: Família

```
┌────────────────────────────────┐
│ ← Família                      │
├────────────────────────────────┤
│                                │
│ 👨‍👩‍👧 GRUPO FAMILIAR             │
│ ┌────────────────────────────┐ │
│ │ Família Exemplo            │ │
│ │ 3 membros                  │ │
│ └────────────────────────────┘ │
│                                │
│ 👥 MEMBROS                     │
│ ┌────────────────────────────┐ │
│ │ 👤 Pessoa A (você)          │ │
│ │    Admin                   │ │
│ │                            │ │
│ │ 👤 Pessoa B                 │ │
│ │    Membro                  │ │
│ │                            │ │
│ │ 👤 Pessoa C                 │ │
│ │    Membro                  │ │
│ └────────────────────────────┘ │
│                                │
│ 🔗 CONVITE                     │
│ ┌────────────────────────────┐ │
│ │ Código: PETIT-ABC123         │ │
│ │ [Copiar]  [Compartilhar]   │ │
│ │                            │ │
│ │ Expira em 7 dias           │ │
│ │ [Gerar novo código]        │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │     SAIR DO GRUPO          │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Tela: Entrar em Grupo

```
┌────────────────────────────────┐
│ ← Entrar em Grupo Familiar     │
├────────────────────────────────┤
│                                │
│ Digite o código de convite:    │
│                                │
│ ┌────────────────────────────┐ │
│ │ PETIT-                       │ │
│ └────────────────────────────┘ │
│                                │
│ Peça o código para quem        │
│ criou o grupo familiar.        │
│                                │
│ ┌────────────────────────────┐ │
│ │         ENTRAR             │ │
│ └────────────────────────────┘ │
│                                │
│           ou                   │
│                                │
│ ┌────────────────────────────┐ │
│ │   CRIAR NOVO GRUPO         │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Seletor ao Criar Pet

```
┌────────────────────────────────┐
│                                │
│ Compartilhamento               │
│                                │
│ ○ 👤 Privado                   │
│   Apenas você verá este pet   │
│                                │
│ ● 👨‍👩‍👧 Família Exemplo           │
│   Todos os membros verão       │
│                                │
└────────────────────────────────┘
```

---

## Estrutura do Firestore (Firebase)

```
// families/{familyId}
{
  "name": "Família Exemplo",
  "createdAt": 1700000000000,
  "createdBy": "uid-do-admin",
  "memberIds": ["uid-admin", "uid-membro1"],
  "inviteCode": "PETIT-ABC123"
}

// families/{familyId}/members/{userId}
{
  "userId": "uid-do-usuario",
  "role": "member",  // "admin" | "member"
  "displayName": "Pessoa B",
  "joinedAt": 1700000000000
}

// invites/{code}
{
  "familyId": "family-id",
  "expiresAt": 1700604800000,
  "createdBy": "uid-do-admin"
}

// pets/{petId} — campo familyId opcional para compartilhamento
{
  "userId": "uid-do-dono",
  "familyId": "family-id",  // null se privado
  // ... demais campos
}
```

---

## Requisitos Técnicos

### FamilyRepository

```kotlin
interface FamilyRepository {
    val currentFamily: StateFlow<Family?>
    
    suspend fun createFamily(name: String): Result<Family>
    suspend fun joinFamily(inviteCode: String): Result<Family>
    suspend fun leaveFamily(): Result<Unit>
    suspend fun generateInviteCode(): Result<String>
    suspend fun removeMember(userId: String): Result<Unit>
    suspend fun deleteFamily(): Result<Unit>
    fun getFamilyMembers(): Flow<List<FamilyMember>>
}

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String?,
    val memberCount: Int,
    val isAdmin: Boolean
)

data class FamilyMember(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: MemberRole,
    val joinedAt: Long
)

enum class MemberRole {
    ADMIN, MEMBER
}
```

### Firestore Security Rules para Família

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Família: membros podem ler, admin pode escrever
    match /families/{familyId} {
      allow read: if request.auth != null &&
        request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null &&
        request.auth.uid == resource.data.createdBy;

      match /members/{memberId} {
        allow read: if request.auth != null &&
          request.auth.uid in get(/databases/$(database)/documents/families/$(familyId)).data.memberIds;
        allow write: if request.auth != null &&
          request.auth.uid == get(/databases/$(database)/documents/families/$(familyId)).data.createdBy;
      }
    }

    // Membros podem ler/escrever pets compartilhados
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }

    // Convites: qualquer autenticado pode ler para entrar
    match /invites/{code} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Sync com Suporte a Família

```kotlin
class FamilySyncEngine(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val familyRepository: FamilyRepository,
    private val petDao: PetDao
) {
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    fun startSync() {
        val userId = authRepository.getCurrentUser()?.id ?: return
        
        // Sync dados pessoais
        startPersonalSync(userId)
        
        // Sync dados da família (se houver)
        familyRepository.currentFamily.value?.let { family ->
            startFamilySync(family.id)
        }
    }

    private fun startFamilySync(familyId: String) {
        // Firestore snapshot listener para pets compartilhados
        val registration = firestore.collection("pets")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // Processar mudanças dos pets da família
            }
        listenerRegistrations.add(registration)
    }
}
```

### Pet com FamilyId

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = null,
    val familyId: String? = null,  // Se compartilhado com família
    val name: String,
    // ...
)

// Query para listar pets (pessoais + família)
@Query("""
    SELECT * FROM pets 
    WHERE (ownerId = :userId OR familyId = :familyId) 
    AND deletedAt IS NULL 
    ORDER BY name
""")
fun getPetsForUserAndFamily(userId: String, familyId: String?): Flow<List<PetEntity>>
```

---

## Invite Flow

```kotlin
class InviteManager(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun generateInviteCode(familyId: String): String {
        val code = "PETIT-" + UUID.randomUUID().toString().take(6).uppercase()
        
        val invite = mapOf(
            "familyId" to familyId,
            "expiresAt" to (System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)),
            "createdBy" to firebaseAuth.currentUser?.uid
        )
        
        firestore.collection("invites").document(code).set(invite).await()
        
        return code
    }

    suspend fun validateAndJoin(code: String): Result<String> {
        val inviteDoc = firestore.collection("invites")
            .document(code).get().await()

        if (!inviteDoc.exists()) {
            return Result.failure(Exception("Código inválido"))
        }

        val expiresAt = inviteDoc.getLong("expiresAt") ?: 0
        if (System.currentTimeMillis() > expiresAt) {
            return Result.failure(Exception("Código expirado"))
        }

        val familyId = inviteDoc.getString("familyId")
            ?: return Result.failure(Exception("Família não encontrada"))

        // Adicionar membro à família
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(Exception("Não autenticado"))

        val displayName = firebaseAuth.currentUser?.displayName

        firestore.collection("families").document(familyId)
            .collection("members").document(userId)
            .set(mapOf(
                "userId" to userId,
                "role" to "member",
                "joinedAt" to System.currentTimeMillis(),
                "displayName" to displayName
            )).await()

        return Result.success(familyId)
    }
}
```

---

## Definition of Done

- [ ] Criar grupo familiar funciona
- [ ] Gerar código de convite
- [ ] Entrar via código de convite
- [ ] Lista de membros exibida
- [ ] Permissões de admin funcionam
- [ ] Sair do grupo funciona
- [ ] Sync de dados compartilhados
- [ ] Pets podem ser privados ou compartilhados
- [ ] Firestore Security Rules
- [ ] Testes de integração
