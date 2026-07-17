# US-001: Gerenciamento de Pets

**Prioridade**: P0
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero cadastrar e gerenciar meus pets no app,
> Para que eu possa acompanhar a saúde de cada um individualmente.

---

## Cenários de Aceite

### Cenário 1: Cadastrar novo pet

```gherkin
DADO que estou na tela de lista de pets
QUANDO toco no botão de adicionar (+)
ENTÃO vejo o formulário de cadastro de pet

DADO que estou no formulário de cadastro
E preencho o nome "Luna"
QUANDO toco em "Salvar"
ENTÃO o pet é salvo no banco local
E sou redirecionado para a lista de pets
E vejo "Luna" na lista
```

### Cenário 2: Campos obrigatórios

```gherkin
DADO que estou no formulário de cadastro
E o campo nome está vazio
QUANDO toco em "Salvar"
ENTÃO vejo mensagem de erro "Nome é obrigatório"
E o pet NÃO é salvo
```

### Cenário 3: Editar pet existente

```gherkin
DADO que tenho um pet "Luna" cadastrado
QUANDO toco no pet na lista
E toco em "Editar"
E altero o nome para "Luninha"
E toco em "Salvar"
ENTÃO o nome é atualizado para "Luninha"
E o campo updatedAt é atualizado
```

### Cenário 4: Excluir pet (soft delete)

```gherkin
DADO que tenho um pet "Luna" cadastrado
QUANDO toco no pet na lista
E toco em "Excluir"
E confirmo a exclusão
ENTÃO o pet não aparece mais na lista
E o pet permanece no banco com deletedAt preenchido
```

### Cenário 5: Adicionar foto do pet

```gherkin
DADO que estou no formulário de cadastro/edição
QUANDO toco no placeholder de foto
ENTÃO posso escolher uma foto da galeria
OU tirar uma nova foto

DADO que selecionei uma foto
QUANDO salvo o pet
ENTÃO a foto é armazenada localmente
E exibida no perfil do pet
```

---

## Campos do Formulário

| Campo                | Tipo                | Obrigatório | Validação              |
| -------------------- | ------------------- | ----------- | ---------------------- |
| Foto                 | Image picker        | ❌          | Máx 5MB, JPG/PNG       |
| Nome                 | TextField           | ✅          | 1-50 caracteres        |
| Tipo de pet          | Dropdown            | ✅          | Gato/Cão/Coelho/Ave/Hamster/Outro |
| Data de nascimento   | DatePicker          | ❌          | Não pode ser futura    |
| Sexo                 | Dropdown            | ❌          | Macho/Fêmea/Indefinido |
| Raça                 | TextField           | ❌          | Máx 50 caracteres      |
| Cor/Pelagem          | TextField           | ❌          | Máx 50 caracteres      |
| Número do microchip  | TextField           | ❌          | Alfanumérico, máx 50   |
| Número do passaporte | TextField           | ❌          | Alfanumérico, máx 50   |
| Observações          | TextField multiline | ❌          | Máx 500 caracteres     |

---

## UI/UX

### Tela: Lista de Pets

```
┌────────────────────────────────┐
│ Meus Pets            [+]   │
├────────────────────────────────┤
│ ┌──────┐                       │
│ │ 📷   │  Luna                 │
│ │      │  2 anos • Fêmea       │
│ └──────┘                       │
├────────────────────────────────┤
│ ┌──────┐                       │
│ │ 📷   │  Simba                │
│ │      │  1 ano • Macho        │
│ └──────┘                       │
└────────────────────────────────┘
```

### Tela: Perfil do Pet

O perfil exibe um card de cabeçalho com foto, nome, idade e chips de informação (sexo, cor, microchip), seguido de um grid 2x2 de ações de gestão:

```
┌────────────────────────────────┐
│ ← Luna                    ⋮    │
├────────────────────────────────┤
│  ┌──────────────────────────┐  │
│  │  [foto]  Luna            │  │
│  │          2 anos • ♀      │  │
│  │          Cinza • 🔵      │  │
│  └──────────────────────────┘  │
│                                │
│ GESTÃO                         │
│ ┌─────────────┐ ┌───────────┐  │
│ │ 💉 Vacinas  │ │ ⚖️ Peso   │  │
│ │ 3 registros │ │ 3.5 kg   │  │
│ │             │ │Faixa ideal│  │
│ └─────────────┘ └───────────┘  │
│ ┌─────────────┐ ┌───────────┐  │
│ │ 🪱 Verms    │ │ 🔗 Compart│  │
│ │ 2 registros │ │  En breve │  │
│ └─────────────┘ └───────────┘  │
└────────────────────────────────┘
```

**Grid de Gestão (4 cards):**

- **Vacinas**: abre VaccinationRecordsScreen; exibe contagem de registros
- **Peso**: abre WeightEntryScreen; exibe último peso + subtítulo "Faixa ideal"
- **Vermífugos**: abre DewormingRecordsScreen; exibe contagem de registros
- **Compartilhar ficha**: desabilitado (acessibilidade: `enabled = false`), exibe badge "Em breve"

---

## Requisitos Técnicos

### Entity

```kotlin
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val petType: String = "OTHER",
    val birthDate: Long? = null,
    val sex: String = "UNKNOWN",
    val breed: String? = null,
    val color: String? = null,
    val microchipNumber: String? = null,
    val passportNumber: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY"
)
```

### Telas Implementadas

| Tela                        | Rota                      | Descrição                              |
| --------------------------- | ------------------------- | -------------------------------------- |
| PetListScreen               | `pets`                    | Lista de pets ativos                  |
| PetDetailScreen             | `pets/{petId}`            | Perfil + grid de gestão                |
| PetFormScreen               | `pets/form?petId={petId}` | Criar/editar pet                      |
| PetDeleteConfirmationScreen | `pets/{petId}/delete`     | Confirmação de exclusão (tela própria) |
| PetSelectionScreen          | `select-pet/{action}`     | Seleção de pet para ações do QuickAdd |

### DAO

```kotlin
@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE deletedAt IS NULL ORDER BY name")
    fun getAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :id AND deletedAt IS NULL")
    suspend fun getPetById(id: String): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Query("UPDATE pets SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePet(id: String, timestamp: Long = System.currentTimeMillis())
}
```

---

## Definition of Done

- [ ] Formulário de cadastro implementado
- [ ] Validação de campos funcionando
- [ ] Lista de pets exibindo corretamente
- [ ] Edição de pet funcionando
- [ ] Soft delete implementado
- [ ] Foto do pet funcionando
- [ ] Dados persistem após fechar o app
- [ ] Testes unitários do DAO
- [ ] Testes de UI básicos
