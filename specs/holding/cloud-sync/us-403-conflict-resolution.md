# US-403: Resolução de Conflitos

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Cloud Sync  
**Fase**: 5

---

## História

> Como usuário com múltiplos dispositivos,  
> Eu quero que conflitos de edição sejam resolvidos automaticamente,  
> Para que eu não perca dados e não precise resolver conflitos manualmente.

---

## Cenários de Aceite

### Cenário 1: Last-write-wins básico

```gherkin
DADO que o pet "Luna" tem updatedAt = 1000 no dispositivo A
E o dispositivo B edita Luna (updatedAt = 1500)
QUANDO o dispositivo A recebe a mudança do snapshot listener do Firestore
ENTÃO a versão do dispositivo B (mais recente) é mantida
E o dispositivo A mostra as alterações do B
```

### Cenário 2: Edição offline mais antiga

```gherkin
DADO que dispositivo A está offline e edita Luna (updatedAt = 1000)
E dispositivo B edita Luna online (updatedAt = 1500)
QUANDO dispositivo A volta online e tenta sync
ENTÃO a versão do dispositivo B vence (updatedAt maior)
E as alterações do dispositivo A são descartadas
E o dispositivo A atualiza para a versão do B
```

### Cenário 3: Edição offline mais recente

```gherkin
DADO que dispositivo A está offline e edita Luna (updatedAt = 2000)
E a versão no Firestore tem updatedAt = 1500
QUANDO dispositivo A volta online e faz sync
ENTÃO a versão do dispositivo A vence (updatedAt maior)
E o Firestore é atualizado com a versão do A
E outros dispositivos recebem a versão do A
```

### Cenário 4: Delete vs Edit

```gherkin
DADO que dispositivo A deleta Luna (deletedAt = 1500)
E dispositivo B editou Luna (updatedAt = 1600) antes de receber o delete
QUANDO o sync acontece
ENTÃO se a edição é mais recente que o delete, Luna é restaurada
OU se o delete é mais recente, Luna permanece deletada
```

### Cenário 5: Campos diferentes editados

```gherkin
DADO que dispositivo A edita o nome de Luna para "Luninha"
E dispositivo B edita o peso de Luna ao mesmo tempo
QUANDO o sync acontece
ENTÃO ambas as mudanças são mantidas (se a estratégia for field-level)
OU a versão mais recente vence completamente (se document-level)
```

---

## Estratégia de Resolução

### Document-Level (Implementação Atual)

```
Regra: Last-Write-Wins baseado em updatedAt

Local:  { id: "1", name: "Luna",    updatedAt: 1000 }
Remote: { id: "1", name: "Luninha", updatedAt: 1500 }

Resultado: Remote vence (1500 > 1000)
           Local é substituído por Remote
```

### Field-Level (Futura Melhoria)

```
Local:  { id: "1", name: "Luna",    weight: 3.5, updatedAt: 1000, weightUpdatedAt: 1000 }
Remote: { id: "1", name: "Luninha", weight: 3.4, updatedAt: 1500, weightUpdatedAt: 900 }

Resultado: Merge
           name: "Luninha" (remote mais recente)
           weight: 3.5 (local mais recente)
```

---

## Requisitos Técnicos

### ConflictResolver

```kotlin
class ConflictResolver {
    
    sealed class Resolution {
        object KeepLocal : Resolution()
        object UseRemote : Resolution()
        data class Merge(val merged: Any) : Resolution()
    }

    fun <T : SyncableEntity> resolve(local: T?, remote: T): Resolution {
        // Não existe localmente: usar remoto
        if (local == null) {
            return Resolution.UseRemote
        }

        // Verificar deletions
        val localDeleted = local.deletedAt != null
        val remoteDeleted = remote.deletedAt != null

        return when {
            // Ambos deletados: usar mais recente
            localDeleted && remoteDeleted -> {
                if (remote.updatedAt >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }
            
            // Apenas remoto deletado
            remoteDeleted -> {
                // Se delete é mais recente que local update, aceitar delete
                if (remote.deletedAt!! >= local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }
            
            // Apenas local deletado
            localDeleted -> {
                // Se remote update é mais recente que local delete, restaurar
                if (remote.updatedAt > local.deletedAt!!) Resolution.UseRemote
                else Resolution.KeepLocal
            }
            
            // Nenhum deletado: comparar updatedAt
            else -> {
                if (remote.updatedAt > local.updatedAt) Resolution.UseRemote
                else Resolution.KeepLocal
            }
        }
    }
}
```

### Aplicando Resolução

```kotlin
class SyncProcessor(
    private val conflictResolver: ConflictResolver,
    private val petDao: PetDao
) {
    suspend fun processRemotePet(remote: PetFirestoreModel) {
        val local = petDao.getPetById(remote.id)
        
        when (val resolution = conflictResolver.resolve(local, remote.toEntity())) {
            is ConflictResolver.Resolution.UseRemote -> {
                petDao.insertPet(remote.toEntity().copy(syncStatus = "SYNCED"))
            }
            is ConflictResolver.Resolution.KeepLocal -> {
                // Local é mais recente, precisa re-upload
                if (local != null && local.syncStatus != "SYNCED") {
                    // Será enviado no próximo upload cycle
                }
            }
            is ConflictResolver.Resolution.Merge -> {
                // Implementação futura para field-level merge
            }
        }
    }
}
```

### Clock Synchronization

Para garantir que `updatedAt` seja confiável entre dispositivos:

```kotlin
object SyncClock {
    /**
     * Retorna timestamp para uso em updatedAt
     * Considera possível diferença de relógio entre dispositivos
     */
    fun now(): Long {
        // Por simplicidade, usar System.currentTimeMillis()
        // Em produção, considerar usar Firestore server timestamps
        // ou NTP para sincronizar relógios
        return System.currentTimeMillis()
    }
}

// No Firestore, usar FieldValue.serverTimestamp() para updated_at
// A coluna updated_at pode usar FieldValue.serverTimestamp()
// Mas para Last-Write-Wins, o client envia o timestamp local
```

### Logging de Conflitos (Debug)

```kotlin
class ConflictLogger(
    private val analyticsTracker: AnalyticsTracker
) {
    fun logConflict(
        entityType: String,
        entityId: String,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long,
        resolution: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.d("ConflictResolver", """
                Conflict detected:
                  Entity: $entityType/$entityId
                  Local updatedAt: $localUpdatedAt
                  Remote updatedAt: $remoteUpdatedAt
                  Resolution: $resolution
            """.trimIndent())
        }

        // Analytics para monitorar frequência de conflitos
        analyticsTracker.trackEvent("sync_conflict", mapOf(
            "entity_type" to entityType,
            "resolution" to resolution
        ))
    }
}
```

---

## Casos Edge

### 1. Timestamps Iguais
```kotlin
// Se timestamps são exatamente iguais (raro), preferir remoto
// Isso evita loops de sync
if (remote.updatedAt == local.updatedAt && local.syncStatus == "SYNCED") {
    return Resolution.KeepLocal  // Já está sincronizado
}
```

### 2. Clock Drift Grande
```kotlin
// Se a diferença de timestamp for absurda (> 1 ano), algo está errado
val MAX_REASONABLE_DIFF = 365L * 24 * 60 * 60 * 1000  // 1 ano em ms

if (abs(remote.updatedAt - local.updatedAt) > MAX_REASONABLE_DIFF) {
    Log.w("Sync", "Suspicious timestamp difference, preferring local")
    return Resolution.KeepLocal
}
```

### 3. Dados Corrompidos
```kotlin
// Validar dados antes de aceitar
if (!remote.isValid()) {
    Log.e("Sync", "Invalid remote data, keeping local")
    return Resolution.KeepLocal
}
```

---

## Definition of Done

- [ ] ConflictResolver implementado
- [ ] Last-write-wins funciona para todos os tipos
- [ ] Delete vs Edit resolvido corretamente
- [ ] Logging de conflitos (debug)
- [ ] Server timestamp usado para updatedAt
- [ ] Casos edge tratados
- [ ] Testes unitários do ConflictResolver
- [ ] Testes de integração com cenários de conflito
