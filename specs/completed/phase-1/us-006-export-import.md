# US-006: Export/Import JSON

**Prioridade**: P1
**Épico**: Core Features
**Fase**: 1

---

## História

> Como tutor de pets,
> Eu quero exportar e importar meus dados em formato JSON,
> Para que eu possa fazer backup manual e restaurar em outro dispositivo ou após reinstalar.

---

## Cenários de Aceite

### Cenário 1: Exportar todos os dados

```gherkin
DADO que tenho pets cadastrados com histórico
QUANDO acesso Configurações > "Exportar dados"
E toco em "Exportar tudo"
ENTÃO um arquivo JSON é gerado
E posso escolher onde salvar (Downloads, compartilhar, etc.)
E o arquivo contém todos os pets, pesos, vacinas, vermífugos e lembretes
```

### Cenário 2: Formato do arquivo exportado

```gherkin
DADO que exporto meus dados
ENTÃO o arquivo tem nome "petit_backup_2026-03-15.json"
E contém estrutura organizada por domínio
E inclui metadados (versão do app, data de export)
```

### Cenário 3: Importar backup

```gherkin
DADO que tenho um arquivo de backup JSON válido
QUANDO acesso Configurações > "Importar dados"
E seleciono o arquivo
ENTÃO vejo resumo do que será importado
"2 pets, 15 pesagens, 8 vacinas, 6 vermífugos"
E posso confirmar ou cancelar
```

### Cenário 4: Conflito na importação

```gherkin
DADO que já tenho um pet "Luna" cadastrado
E o arquivo de backup também tem "Luna" (mesmo ID)
QUANDO importo o backup
ENTÃO vejo opções:
  - "Substituir dados locais" (usa dados do backup)
  - "Manter dados locais" (ignora duplicados)
  - "Mesclar" (last-write-wins por updatedAt)
```

### Cenário 5: Validação de arquivo

```gherkin
DADO que seleciono um arquivo inválido
QUANDO tento importar
ENTÃO vejo erro "Arquivo inválido ou corrompido"
E nenhum dado é alterado

DADO que seleciono arquivo de versão muito antiga
ENTÃO vejo aviso "Arquivo de versão antiga. Alguns dados podem não ser compatíveis."
```

### Cenário 6: Exportar pet específico

```gherkin
DADO que estou no perfil do pet "Luna"
QUANDO toco em opções (⋮) > "Exportar dados da Luna"
ENTÃO apenas os dados da Luna são exportados
(pet + pesos + vacinas + vermífugos)
```

> ⚠️ **Status verificado na migração:** o método `exportForPet(petId)` existe no
> `ExportImportUseCase`, mas esta spec não encontrou um ponto de entrada
> correspondente na `PetDetailScreen`. A exportação de um pet específico segue
> pendente na interface.

---

## Estrutura do JSON

> **Nota:** O Petit atual usa a chave `"tasks"`. Na data desta migração, arquivos
> históricos que contenham apenas `"reminders"` precisam de conversão explícita
> antes da importação.

```json
{
  "metadata": {
    "appVersion": "1.0.0",
    "exportDate": "2026-03-15T10:30:00Z",
    "schemaVersion": 1
  },
  "pets": [
    {
      "id": "uuid-1",
      "name": "Luna",
      "petType": "CAT",
      "birthDate": "2024-03-15",
      "sex": "FEMALE",
      "breed": null,
      "color": null,
      "microchipNumber": null,
      "passportNumber": null,
      "photoUri": null,
      "notes": null,
      "createdAt": 1710489600000,
      "updatedAt": 1710489600000
    }
  ],
  "weightEntries": [
    {
      "id": "uuid-2",
      "petId": "uuid-1",
      "date": "2026-03-15",
      "weightGrams": 3500,
      "note": null,
      "createdAt": 1710489600000,
      "updatedAt": 1710489600000
    }
  ],
  "vaccinationEntries": [
    {
      "id": "uuid-3",
      "petId": "uuid-1",
      "vaccineType": "V3",
      "customVaccineTypeName": null,
      "applicationDate": "2026-03-15",
      "nextDueDate": "2027-03-15",
      "veterinarian": "Dr. Silva",
      "clinic": "PetCare",
      "batchNumber": "ABC123",
      "note": null,
      "createdAt": 1710489600000,
      "updatedAt": 1710489600000
    }
  ],
  "dewormingEntries": [
    {
      "id": "uuid-4",
      "petId": "uuid-1",
      "type": "INTERNAL",
      "medication": "Milbemax",
      "applicationDate": "2026-03-15",
      "nextDueDate": "2026-06-15",
      "note": null,
      "createdAt": 1710489600000,
      "updatedAt": 1710489600000
    }
  ],
  "tasks": [
    {
      "id": "uuid-5",
      "petId": "uuid-1",
      "kind": "VACCINATION",
      "referenceEntityId": "uuid-3",
      "title": "Vacina V3 - Luna",
      "description": null,
      "scheduledFor": "2026-03-15T09:00:00",
      "status": "PENDING",
      "createdAt": 1710489600000,
      "updatedAt": 1710489600000
    }
  ]
}
```

---

## UI/UX

### Tela: Configurações

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 📦 DADOS                       │
│ ┌────────────────────────────┐ │
│ │ 📤 Exportar dados          │ │
│ │ Salvar backup em JSON      │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📥 Importar dados          │ │
│ │ Restaurar de backup JSON   │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Importação

```
┌────────────────────────────────┐
│       Importar Backup          │
├────────────────────────────────┤
│                                │
│ Serão importados:              │
│ • 2 pets                      │
│ • 15 pesagens                  │
│ • 8 vacinas                    │
│ • 6 vermífugos                 │
│ • 3 tarefas                    │
│                                │
│ ⚠️ 1 pet já existe (Luna)     │
│                                │
│ Conflitos:                     │
│ ○ Substituir dados locais      │
│ ○ Manter dados locais          │
│ ● Mesclar (mais recente vence) │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │   IMPORTAR   │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Modelo de exportação

```kotlin
data class ExportBundle(
    val metadata: ExportMetadata,
    val pets: List<Pet>,
    val weightEntries: List<WeightEntry>,
    val vaccinationEntries: List<VaccinationEntry>,
    val dewormingEntries: List<DewormingEntry>,
    val tasks: List<Task>
)

data class ExportMetadata(
    val appVersion: String,
    val exportDate: String,  // ISO 8601
    val schemaVersion: Int = 1
)
```

### Caso de uso de exportação

```kotlin
class ExportImportUseCase(...) {
    suspend fun exportAll(): ExportBundle
    suspend fun exportForPet(petId: String): ExportBundle
    suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri)
    suspend fun readImportFromUri(uri: Uri): ExportBundle
}
```

O código atual serializa com `org.json`, usa a chave `tasks` e gera arquivos no
formato `petit_backup_YYYY-MM-DD.json`.

### Import Use Case

```kotlin
enum class ConflictResolution {
    REPLACE,  // Dados do backup substituem locais
    KEEP,     // Dados locais são mantidos
    MERGE     // Last-write-wins por updatedAt
}

class ExportImportUseCase(...) {
    suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis {
        val existingPetIds = petRepository.getAllPets().first().map { it.id }.toSet()
        val conflictingPets = bundle.pets.filter { it.id in existingPetIds }

        return ImportAnalysis(
            totalPets = bundle.pets.size,
            totalWeightEntries = bundle.weightEntries.size,
            totalVaccinationEntries = bundle.vaccinationEntries.size,
            totalDewormingEntries = bundle.dewormingEntries.size,
            totalTasks = bundle.tasks.size,
            conflictingPetNames = conflictingPets.map { it.name },
            schemaVersion = bundle.metadata.schemaVersion,
            exportDate = bundle.metadata.exportDate
        )
    }

    suspend fun importData(
        bundle: ExportBundle,
        conflictResolution: ConflictResolution
    ): MergeResult
}
```

### File Operations

```kotlin
// APIs atuais; ambas validam o acesso por ContentResolver.
suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri)
suspend fun readImportFromUri(uri: Uri): ExportBundle
```

---

## Definition of Done

- [ ] Export de todos os dados para JSON
- [ ] Export de pet específico
- [ ] Nome do arquivo com data
- [ ] Metadados incluídos (versão, data, schema)
- [ ] Import com análise prévia
- [ ] Opções de resolução de conflito
- [ ] Validação de arquivo importado
- [ ] Transação atômica no import
- [ ] Integração com share sheet do Android
- [ ] Testes de serialização/deserialização
