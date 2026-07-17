# US-103: Grupo Familiar Local

> **Documento duplicado/superseded.** Esta variante histórica de
> [`us-103-family-group.md`](./us-103-family-group.md) foi preservada porque tem
> pequenas diferenças de navegação e wireframe. O estado é parcialmente
> implementado: lista, remoção local, saída e DataStore existem; renomeação e
> propagação entre dispositivos não foram comprovadas. Origem:
> `getmiw/specs-miw@09b4497`, migrado em 2026-07-17.

**Prioridade**: P1
**Épico**: Compartilhamento Familiar
**Fase**: 2

---

## História

> Como usuário do app,
> Eu quero gerenciar o grupo familiar local,
> Para que eu saiba quais dispositivos estão compartilhando dados e possa controlar o acesso.

---

## Cenários de Aceite

### Cenário 1: Ver grupo familiar

```gherkin
DADO que estou pareado com outro dispositivo
QUANDO acesso "Compartilhar com Família"
ENTÃO vejo a lista de dispositivos do grupo
E vejo meu dispositivo marcado como "Este dispositivo"
E vejo a data da última sincronização de cada membro
```

### Cenário 2: Ver dispositivo remoto

```gherkin
DADO que estou no grupo familiar
QUANDO vejo a lista de membros
ENTÃO vejo o nome do dispositivo (ex: "Device B")
E vejo "Última sync: há 2 min" ou "Nunca sincronizou"
```

### Cenário 3: Renomear meu dispositivo

```gherkin
DADO que estou no grupo familiar
QUANDO toco no meu dispositivo
E edito o nome para "Device A"
E salvo
ENTÃO o nome é atualizado
E será propagado na próxima sync
```

### Cenário 4: Remover membro do grupo

```gherkin
DADO que estou no grupo familiar
QUANDO toco em "Remover" ao lado de um dispositivo
E confirmo "Remover este dispositivo do grupo?"
ENTÃO o dispositivo é removido da lista local
E na próxima sync, o outro device recebe a remoção
E o device removido mantém seus dados mas perde referência de sync
```

### Cenário 5: Sair do grupo

```gherkin
DADO que sou membro de um grupo familiar
QUANDO toco em "Sair do Grupo"
E confirmo
ENTÃO saio do grupo
E meus dados locais permanecem
E a family group key é removida do DataStore
E o outro device verá que saí na próxima sync
```

---

## UI/UX

### Tela: Grupo Familiar

```
┌────────────────────────────────┐
│ ← Compartilhar com Família      │
├────────────────────────────────┤
│                                │
│ [grupo] GRUPO FAMILIAR             │
│ ┌────────────────────────────┐ │
│ │ [dispositivo] Device A  │ │
│ │    Este dispositivo        │ │
│ │    Sync: agora              │ │
│ │                            │ │
│ │ [dispositivo] Device B    │ │
│ │    Última sync: há 5 min   │ │
│ │                   [Remover]│ │
│ └────────────────────────────┘ │
│                                │
│ [status] STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Sim Sincronizado            │ │
│ │ 2 pets compartilhados     │ │
│ │ 51 registros no total      │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │   PAREAR NOVO DISPOSITIVO  │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │   ENVIAR DADOS AGORA       │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │      SAIR DO GRUPO         │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Room Entities

```kotlin
@Entity(tableName = "family_group_members")
data class FamilyGroupMemberEntity(
    @PrimaryKey val id: String,          // UUID
    val deviceName: String,
    val familyGroupKey: String,
    val isLocalDevice: Boolean,
    val lastSyncAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
)
```

### DataStore Keys

```kotlin
object FamilyGroupPreferences {
    val FAMILY_GROUP_KEY = stringPreferencesKey("family_group_key")
    val LOCAL_DEVICE_ID = stringPreferencesKey("local_device_id")
    val LOCAL_DEVICE_NAME = stringPreferencesKey("local_device_name")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
}
```

---

## Critérios de Aceite

- [ ] Lista de membros do grupo é visível
- [ ] Nome do dispositivo pode ser editado
- [ ] É possível remover membro do grupo
- [ ] É possível sair do grupo
- [ ] Dados locais permanecem ao sair do grupo
- [ ] Status de última sync é visível por membro
- [ ] Family group key é gerenciada via DataStore
