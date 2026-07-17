# US-003: Registro de Vacinação

**Prioridade**: P0
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero registrar as vacinas dos meus pets,
> Para que eu possa manter o calendário de vacinação em dia e não perder prazos.

---

## Cenários de Aceite

### Cenário 1: Registrar vacinação

```gherkin
DADO que estou no perfil do pet "Luna"
QUANDO toco em "Vacinas"
E toco em "Nova vacina"
E seleciono tipo "V3 - Tríplice Felina"
E informo data de aplicação "15/03/2026"
E informo próxima dose "15/03/2027"
QUANDO toco em "Salvar"
ENTÃO a vacinação é registrada
E o status é calculado como "OK" (próxima dose > 30 dias)
```

### Cenário 2: Cálculo automático de status

```gherkin
DADO que Luna tem vacina V3 com próxima dose em 20/03/2026
E hoje é 15/03/2026 (5 dias para próxima)
ENTÃO o status é "SCHEDULED" (próximos 30 dias)
E exibe "5 dias para próxima dose"

DADO que Luna tem vacina V3 com próxima dose em 01/03/2026
E hoje é 15/03/2026 (14 dias atrasada)
ENTÃO o status é "OVERDUE"
E exibe "Atrasada há 14 dias" em vermelho
```

### Cenário 3: Vacina sem próxima dose

```gherkin
DADO que estou registrando uma vacina de raiva
E não informo próxima dose
QUANDO salvo
ENTÃO o status é "OK"
E não exibe informação de próxima dose
```

### Cenário 4: Ver histórico de vacinas por tipo

```gherkin
DADO que Luna tem múltiplas doses de V3 registradas
QUANDO acesso "Vacinas" da Luna
ENTÃO vejo as vacinas agrupadas por tipo
E vejo o histórico de cada tipo
E vejo indicador visual do status de cada tipo
```

### Cenário 5: Campos opcionais de rastreabilidade

```gherkin
DADO que estou registrando uma vacina
QUANDO preencho veterinário, clínica e lote
E salvo
ENTÃO essas informações são armazenadas
E podem ser visualizadas no detalhe da vacina
```

---

## Tipos de Vacina

As opções são filtradas por `PetType`. Raiva e “Outra” são gerais; as demais
são específicas por espécie.

| Código | Nome             | Descrição                                  |
| ------ | ---------------- | ------------------------------------------ |
| V3     | Tríplice Felina  | Panleucopenia, Rinotraqueíte, Calicivirose |
| V4     | Quádrupla Felina | V3 + Clamidiose                            |
| V5     | Quíntupla Felina | V4 + Leucemia (FeLV)                       |
| RABIES | Antirrábica      | Raiva                                      |
| FELV   | Leucemia Felina  | FeLV separada                              |
| FIV    | Imunodeficiência Felina | FIV                                |
| DHPP   | DHPP             | Cães                                       |
| BORDETELLA | Bordetella   | Cães                                       |
| LEPTOSPIROSIS | Leptospirose | Cães                                    |
| LEISHMANIA | Leishmaniose | Cães                                       |
| GRIPE_CANINA | Gripe canina | Cães                                     |
| RHDV   | Calicivirose     | Coelhos                                    |
| MYXOMATOSIS | Mixomatose  | Coelhos                                    |
| POLYOMAVIRUS | Poliomavírus aviário | Aves                           |
| OTHER  | Outra            | Personalizada                              |

---

## Campos do Formulário

| Campo             | Tipo       | Obrigatório | Validação                       |
| ----------------- | ---------- | ----------- | ------------------------------- |
| Tipo de vacina    | Dropdown   | ✅          | Lista fixa                      |
| Nome customizado  | TextField  | Se OTHER    | 1-100 chars                     |
| Data de aplicação | DatePicker | ✅          | Não futura                      |
| Próxima dose      | DatePicker | ❌          | Se preenchida, > data aplicação |
| Veterinário       | TextField  | ❌          | Máx 100 chars                   |
| Clínica           | TextField  | ❌          | Máx 100 chars                   |
| Lote              | TextField  | ❌          | Máx 50 chars                    |
| Observação        | TextField  | ❌          | Máx 500 chars                   |

---

## UI/UX

### Tela: Lista de Vacinas

```
┌────────────────────────────────┐
│ ← Vacinas do Luna         [+]  │
├────────────────────────────────┤
│                                │
│ V3 - Tríplice Felina           │
│ ┌────────────────────────────┐ │
│ │ ✅ Em dia                  │ │
│ │ Última: 15/03/2026         │ │
│ │ Próxima: 15/03/2027        │ │
│ │ 365 dias restantes         │ │
│ └────────────────────────────┘ │
│                                │
│ Raiva                          │
│ ┌────────────────────────────┐ │
│ │ ⚠️ Próxima em 15 dias      │ │
│ │ Última: 15/03/2025         │ │
│ │ Próxima: 30/03/2026        │ │
│ └────────────────────────────┘ │
│                                │
│ V4 - Quádrupla Felina          │
│ ┌────────────────────────────┐ │
│ │ 🔴 Atrasada há 30 dias     │ │
│ │ Última: 15/01/2025         │ │
│ │ Próxima: 15/02/2026        │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Status Visual

| Status    | Cor      | Ícone |
| --------- | -------- | ----- |
| OK        | Verde    | ✅    |
| SCHEDULED | Amarelo  | ⚠️    |
| OVERDUE   | Vermelho | 🔴    |

---

## Requisitos Técnicos

### Entity

```kotlin
@Entity(
    tableName = "vaccination_entries",
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
data class VaccinationEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val vaccineType: String,  // Enum name
    val customVaccineTypeName: String? = null,  // Se OTHER
    val applicationDate: Long,
    val nextDueDate: Long? = null,
    val veterinarian: String? = null,
    val clinic: String? = null,
    val batchNumber: String? = null,
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
interface VaccinationEntryDao {
    @Query("""
        SELECT * FROM vaccination_entries
        WHERE petId = :petId AND deletedAt IS NULL
        ORDER BY applicationDate DESC
    """)
    fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntryEntity>>

    fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationEntry(entry: VaccinationEntryEntity)
}
```

### Cálculo de Status

```kotlin
fun calculateVaccinationStatus(nextDueDate: Long?, today: Long): HealthStatus {
    if (nextDueDate == null) return HealthStatus.OK

    val daysUntil = (nextDueDate - today) / (24 * 60 * 60 * 1000)

    return when {
        daysUntil < 0 -> HealthStatus.OVERDUE
        daysUntil <= 30 -> HealthStatus.SCHEDULED
        else -> HealthStatus.OK
    }
}
```

---

## Definition of Done

- [ ] Formulário de vacinação implementado
- [ ] Dropdown de tipos de vacina funcionando
- [ ] Campo customizado para "Outra" vacina
- [ ] Cálculo automático de status funcionando
- [ ] Lista agrupada por tipo de vacina
- [ ] Indicadores visuais de status (cores/ícones)
- [ ] Histórico por tipo de vacina
- [ ] Campos de rastreabilidade (vet/clínica/lote)
- [ ] Edição e soft delete funcionando
- [ ] Testes unitários do cálculo de status
