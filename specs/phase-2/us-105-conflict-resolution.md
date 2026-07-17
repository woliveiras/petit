# US-105: Resolução de Conflitos Local

> **Estado no Petit (2026-07-17): parcialmente implementado.** O merge atual
> usa `updatedAt` e registra `SyncLog`, mas não existe `ConflictResolver`
> dedicado, tela de histórico ou cobertura de testes para todos os casos de
> soft delete descritos aqui. A regra também precisa definir um desempate para
> timestamps iguais com payloads diferentes antes de garantir simetria.
> Origem: `getmiw/specs-miw@09b4497`.

**Prioridade**: P1
**Épico**: Compartilhamento Familiar
**Fase**: 2

---

## História

> Como usuário do app,
> Eu quero que conflitos de dados sejam resolvidos automaticamente quando duas pessoas cuidadoras editam o mesmo registro,
> Para que não perdamos informações importantes.

---

## Cenários de Aceite

### Cenário 1: Edição simultânea — last-write-wins

```gherkin
DADO que duas pessoas cuidadoras editaram o peso do Pet A no mesmo dia
E eu salvei às 10:00 (3500g)
E a outra pessoa salvou às 10:05 (3520g)
QUANDO a sync acontece
ENTÃO o valor de 3520g prevalece (updatedAt mais recente)
E ambos devices mostram 3520g
```

### Cenário 2: Adicionar registros diferentes

```gherkin
DADO que eu adicionei uma vacinação do Pet A
E outra pessoa cuidadora adicionou uma pesagem do Pet A
QUANDO a sync acontece
ENTÃO ambos registros são adicionados em ambos devices
(UUIDs diferentes, não há conflito)
```

### Cenário 3: Um deleta, outro edita

```gherkin
DADO que eu deletei (soft delete) um registro de peso
E outra pessoa cuidadora editou o mesmo registro antes da sync
E a edição dela tem updatedAt mais recente que meu deletedAt
QUANDO a sync acontece
ENTÃO a edição dela prevalece (register stays, undone delete)
E o registro reaparece em ambos devices
```

### Cenário 4: Ambos deletam

```gherkin
DADO que duas pessoas cuidadoras deletaram o mesmo registro
QUANDO a sync acontece
ENTÃO o registro permanece deletado (soft delete) em ambos
```

### Cenário 5: Log de sync visível

```gherkin
DADO que uma sync completou
QUANDO acesso Perfil > "Grupo Familiar" > "Gerenciar Grupo" > "Histórico de Sync"
ENTÃO vejo:
  "Hoje 10:15 — 3 enviados, 2 recebidos, 0 conflitos"
  "Hoje 09:30 — 1 enviado, 5 recebidos, 1 conflito resolvido"
```

---

## Regras de Resolução

### Regra Principal: Last-Write-Wins por `updatedAt`

```
Para cada entidade no changeset remoto:
  1. Buscar entidade local pelo UUID
  2. Se não existe localmente → INSERT
  3. Se existe e remote.updatedAt > local.updatedAt → UPDATE com dados remotos
  4. Se existe e remote.updatedAt <= local.updatedAt → MANTER local (dados locais são mais recentes)
  5. Se remote.deletedAt != null:
     a. Se local.updatedAt > remote.deletedAt → MANTER local (edição desfaz delete)
     b. Se local.updatedAt <= remote.deletedAt → APLICAR soft delete
```

### Garantias

- **Determinístico**: mesma entrada sempre gera mesma saída
- **Idempotente**: aplicar o mesmo changeset duas vezes não muda nada
- **Simétrico**: resultado é o mesmo independente de qual device envia primeiro

---

## Requisitos Técnicos

### ConflictResolver

```kotlin
class ConflictResolver {
    fun resolve(local: SyncableEntity?, remote: SyncableEntity): ResolutionAction {
        return when {
            // Remote não existe localmente → inserir
            local == null -> ResolutionAction.INSERT

            // Remote foi deletado
            remote.deletedAt != null -> {
                if (local.updatedAt > remote.deletedAt) {
                    ResolutionAction.KEEP_LOCAL // edição local mais recente desfaz delete
                } else {
                    ResolutionAction.APPLY_DELETE
                }
            }

            // Remote é mais recente → atualizar
            remote.updatedAt > local.updatedAt -> ResolutionAction.UPDATE

            // Local é mais recente ou igual → manter
            else -> ResolutionAction.KEEP_LOCAL
        }
    }
}

enum class ResolutionAction {
    INSERT,
    UPDATE,
    KEEP_LOCAL,
    APPLY_DELETE
}
```

### SyncLog

```kotlin
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val peerId: String,
    val peerName: String,
    val syncTimestamp: Long,
    val entitiesSent: Int,
    val entitiesReceived: Int,
    val conflictsResolved: Int,
    val syncType: String,  // "LAN_AUTO", "NEARBY_MANUAL"
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
)
```

---

## Critérios de Aceite

- [ ] Last-write-wins por updatedAt funciona corretamente
- [ ] Soft deletes são propagados respeitando updatedAt
- [ ] Registros novos (UUID inexistente) são inseridos
- [ ] Resolução é determinística e idempotente
- [ ] Log de sync é gravado e visível
- [ ] Sem perda de dados em cenários de edição simultânea
