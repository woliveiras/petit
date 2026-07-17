# US-002: Acompanhamento de Peso

**Prioridade**: P0
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero registrar o peso dos meus pets ao longo do tempo,
> Para que eu possa acompanhar a evolução e identificar variações anormais.

---

## Cenários de Aceite

### Cenário 1: Registrar pesagem

```gherkin
DADO que estou no perfil do pet "Luna"
QUANDO toco em "Peso"
E toco em "Nova pesagem"
E informo o peso "3.5" kg
E a data é preenchida automaticamente com hoje
QUANDO toco em "Salvar"
ENTÃO a pesagem é registrada
E vejo a pesagem na lista de histórico
```

### Cenário 2: Peso em gramas ou quilos

```gherkin
DADO que estou registrando uma pesagem
QUANDO informo "3.5" com unidade "kg"
ENTÃO o valor salvo no banco é 3500 (gramas)

DADO que estou registrando uma pesagem
QUANDO informo "350" com unidade "g"
ENTÃO o valor salvo no banco é 350 (gramas)
```

### Cenário 3: Apenas uma pesagem por dia

```gherkin
DADO que já existe uma pesagem para Luna em 15/03/2026
QUANDO tento registrar outra pesagem para Luna em 15/03/2026
ENTÃO a pesagem anterior é substituída (upsert)
E apenas uma pesagem aparece para esse dia
```

### Cenário 4: Ver gráfico de evolução

```gherkin
DADO que Luna tem pesagens registradas em múltiplas datas
QUANDO acesso a tela de peso da Luna
ENTÃO vejo um gráfico de barras mostrando a evolução
E o eixo X mostra as datas
E o eixo Y mostra o peso em kg
```

> **Nota:** O gráfico implementado usa `Vico` com `rememberColumnCartesianLayer` (barras verticais), não linha.

### Cenário 5: Validação de peso

```gherkin
DADO que estou registrando uma pesagem
QUANDO informo peso "0" ou negativo
ENTÃO vejo erro "Peso deve ser maior que zero"
E a pesagem NÃO é salva

QUANDO informo peso maior que 50 kg
ENTÃO vejo mensagem de erro (rejeição hard)
E a pesagem NÃO é salva
```

> **Nota:** O peso máximo implementado é **50 kg** com rejeição direta. Não há diálogo de confirmação.

### Cenário 6: Editar pesagem existente

```gherkin
DADO que Luna tem uma pesagem de 3.5kg em 15/03/2026
QUANDO toco na pesagem
E altero para 3.6kg
E salvo
ENTÃO o peso é atualizado
E o updatedAt é atualizado
```

### Cenário 7: Excluir pesagem

```gherkin
DADO que Luna tem uma pesagem registrada
QUANDO toco na pesagem
E toco em "Excluir"
E confirmo
ENTÃO a pesagem é removida da lista (soft delete)
E o gráfico é atualizado
```

---

## Campos do Formulário

| Campo      | Tipo        | Obrigatório | Validação                     |
| ---------- | ----------- | ----------- | ----------------------------- |
| Data       | DatePicker  | ✅          | Default: hoje, não futura     |
| Peso       | NumberField | ✅          | > 0, máx 50kg (rejeição hard) |
| Unidade    | Toggle      | ✅          | kg (default) ou g             |
| Observação | TextField   | ❌          | Máx 200 caracteres            |

---

## UI/UX

### Tela: Histórico de Peso

```
┌────────────────────────────────┐
│ ← Peso do Luna            [+]  │
├────────────────────────────────┤
│                                │
│     📈 Gráfico de evolução     │
│     ┌─────────────────────┐    │
│  kg │    ╱╲                │    │
│ 3.5 │   ╱  ╲   ╱          │    │
│ 3.0 │  ╱    ╲─╱           │    │
│     └─────────────────────┘    │
│       Jan  Fev  Mar            │
│                                │
├────────────────────────────────┤
│ Histórico                      │
├────────────────────────────────┤
│ 15/03/2026           3.5 kg    │
│ 01/03/2026           3.4 kg    │
│ 15/02/2026           3.6 kg    │
└────────────────────────────────┘
```

### Tela: Nova Pesagem

```
┌────────────────────────────────┐
│ ← Nova Pesagem                 │
├────────────────────────────────┤
│                                │
│ Data                           │
│ ┌────────────────────────────┐ │
│ │ 15/03/2026             📅  │ │
│ └────────────────────────────┘ │
│                                │
│ Peso                           │
│ ┌──────────────────┐ ┌──────┐ │
│ │ 3.5              │ │ kg ▼│ │
│ └──────────────────┘ └──────┘ │
│                                │
│ Observação (opcional)          │
│ ┌────────────────────────────┐ │
│ │                            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │         SALVAR             │ │
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Entity

```kotlin
@Entity(
    tableName = "weight_entries",
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
data class WeightEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val date: Long,  // Epoch day
    val weightGrams: Int,
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
interface WeightEntryDao {
    @Query("""
        SELECT * FROM weight_entries
        WHERE petId = :petId AND deletedAt IS NULL
        ORDER BY date DESC
    """)
    fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntryEntity>>

    @Query("""
        SELECT * FROM weight_entries
        WHERE petId = :petId AND date = :date AND deletedAt IS NULL
    """)
    suspend fun getWeightEntryByDate(petId: String, date: Long): WeightEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Query("""
        SELECT * FROM weight_entries
        WHERE petId = :petId AND deletedAt IS NULL
        ORDER BY date DESC LIMIT 1
    """)
    suspend fun getLatestWeightEntry(petId: String): WeightEntryEntity?
}
```

### Gráfico

- Usar biblioteca: **Vico** (Android Charting) ou **MPAndroidChart**
- Exibir últimos 6 meses por padrão
- Permitir zoom/pan para ver mais dados

---

## Definition of Done

- [ ] Formulário de pesagem implementado
- [ ] Toggle kg/g funcionando
- [ ] Conversão para gramas no salvamento
- [ ] Upsert por data funcionando
- [ ] Lista de histórico ordenada por data desc
- [ ] Gráfico de evolução funcionando
- [ ] Validação de peso implementada
- [ ] Edição de pesagem funcionando
- [ ] Soft delete funcionando
- [ ] Testes unitários
