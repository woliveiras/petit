# US-004: Registro de Desparasitação

**Prioridade**: P0
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero registrar os vermífugos e antipulgas dos meus pets,
> Para que eu possa manter a proteção deles sempre atualizada.

---

## Cenários de Aceite

### Cenário 1: Registrar vermífugo interno

```gherkin
DADO que estou no perfil do pet "Luna"
QUANDO toco em "Vermífugos"
E toco em "Novo registro"
E seleciono tipo "Interno"
E informo medicamento "Milbemax"
E informo data de aplicação "15/03/2026"
E informo próxima dose "15/06/2026" (3 meses)
QUANDO toco em "Salvar"
ENTÃO o registro é salvo
E o status é calculado automaticamente
```

### Cenário 2: Registrar antipulgas/carrapatos (externo)

```gherkin
DADO que estou registrando desparasitação
QUANDO seleciono tipo "Externo"
E informo medicamento "Frontline"
E informo data "15/03/2026"
E informo próxima aplicação "15/04/2026" (1 mês)
ENTÃO o registro é salvo com tipo EXTERNAL
```

### Cenário 3: Registrar combo (interno + externo)

```gherkin
DADO que estou registrando desparasitação
QUANDO seleciono tipo "Combo"
E informo medicamento "Broadline"
ENTÃO o registro é salvo com tipo BOTH
E conta para ambas categorias
```

### Cenário 4: Cálculo automático de status

```gherkin
DADO que Luna tem vermífugo externo com próxima dose em 20/03/2026
E hoje é 15/03/2026 (5 dias para próxima)
ENTÃO o status é "SCHEDULED"
E exibe indicador amarelo

DADO que Luna tem vermífugo interno vencido há 15 dias
ENTÃO o status é "OVERDUE"
E exibe indicador vermelho
```

### Cenário 5: Separação por categoria

```gherkin
DADO que Luna tem registros de vermífugo interno e externo
QUANDO acesso "Vermífugos" da Luna
ENTÃO vejo seções separadas para:
  - Interno (vermífugos)
  - Externo (antipulgas/carrapatos)
E cada seção mostra seu próprio status
```

---

## Tipos de Desparasitação

| Tipo | Nome | Descrição | Frequência típica |
|------|------|-----------|-------------------|
| INTERNAL | Interno | Vermífugos (comprimido, pasta) | 3-6 meses |
| EXTERNAL | Externo | Antipulgas/carrapatos (pipeta, coleira) | 1-3 meses |
| BOTH | Combo | Produtos combinados (ex: Broadline) | 1-3 meses |

---

## Campos do Formulário

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| Tipo | Dropdown | ✅ | INTERNAL/EXTERNAL/BOTH |
| Medicamento | TextField | ✅ | 1-100 chars |
| Data de aplicação | DatePicker | ✅ | Não futura |
| Próxima aplicação | DatePicker | ❌ | Se preenchida, > data aplicação |
| Observação | TextField | ❌ | Máx 500 chars |

---

## UI/UX

### Tela: Lista de Desparasitação

```
┌────────────────────────────────┐
│ ← Vermífugos do Luna      [+]  │
├────────────────────────────────┤
│                                │
│ 🪱 INTERNO (Vermífugos)        │
│ ┌────────────────────────────┐ │
│ │ ✅ Em dia                  │ │
│ │ Último: Milbemax           │ │
│ │ 15/03/2026                 │ │
│ │ Próximo: 15/06/2026        │ │
│ │ 92 dias restantes          │ │
│ └────────────────────────────┘ │
│                                │
│ 🦟 EXTERNO (Antipulgas)        │
│ ┌────────────────────────────┐ │
│ │ ⚠️ Próximo em 10 dias      │ │
│ │ Último: Frontline          │ │
│ │ 15/02/2026                 │ │
│ │ Próximo: 25/03/2026        │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│ Histórico completo         ▶   │
└────────────────────────────────┘
```

### Tela: Histórico

```
┌────────────────────────────────┐
│ ← Histórico de Vermífugos      │
├────────────────────────────────┤
│ Março 2026                     │
│ ├─ 15/03 Milbemax (interno)    │
│                                │
│ Fevereiro 2026                 │
│ ├─ 15/02 Frontline (externo)   │
│ ├─ 01/02 Milbemax (interno)    │
│                                │
│ Janeiro 2026                   │
│ ├─ 15/01 Frontline (externo)   │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Entity

```kotlin
@Entity(
    tableName = "deworming_entries",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petId")]
)
data class DewormingEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val type: String,  // INTERNAL, EXTERNAL, BOTH
    val medication: String,
    val applicationDate: Long,
    val nextDueDate: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY"
)
```

### DAO

```kotlin
@Dao
interface DewormingEntryDao {
    @Query("""
        SELECT * FROM deworming_entries
        WHERE petId = :petId AND deletedAt IS NULL
        ORDER BY applicationDate DESC
    """)
    fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntryEntity>>

    fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDewormingEntry(entry: DewormingEntryEntity)
}
```

### Status por Categoria — proposta histórica pendente

O trecho abaixo preserva a intenção original. A API atual calcula
`HealthStatus` por registro e não expõe `getLatestByType`; a separação por
categoria continua marcada como pendente na Definition of Done.

```kotlin
data class DewormingCategoryStatus(
    val internal: DewormingStatus,
    val external: DewormingStatus
)

suspend fun calculateDewormingStatus(petId: String): DewormingCategoryStatus {
    val latestInternal = dao.getLatestByType(petId, "INTERNAL")
    val latestExternal = dao.getLatestByType(petId, "EXTERNAL")

    return DewormingCategoryStatus(
        internal = calculateStatus(latestInternal?.nextDueDate),
        external = calculateStatus(latestExternal?.nextDueDate)
    )
}
```

---

## Definition of Done

- [ ] Formulário com 3 tipos (Interno/Externo/Combo)
- [ ] Campo de medicamento obrigatório
- [ ] Separação visual por categoria (interno vs externo)
- [ ] Status calculado por categoria
- [ ] Tipo BOTH contabiliza para ambas categorias
- [ ] Histórico completo ordenado por data
- [ ] Indicadores visuais de status
- [ ] Edição e soft delete funcionando
- [ ] Testes unitários
