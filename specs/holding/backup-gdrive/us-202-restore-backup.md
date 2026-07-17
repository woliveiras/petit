# US-202: Restaurar Backup

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Cloud Backup  
**Fase**: 3

---

## História

> Como usuário logado,  
> Eu quero restaurar meus dados de um backup no Google Drive,  
> Para que eu possa recuperar meus dados em um novo celular ou após reinstalar o app.

---

## Cenários de Aceite

### Cenário 1: Restaurar backup com sucesso

```gherkin
DADO que estou logado com Google
E tenho backups salvos no Google Drive
QUANDO acesso "Backups salvos"
E seleciono um backup para restaurar
E confirmo a restauração
ENTÃO vejo progresso de download
E os dados são restaurados no banco local
E vejo mensagem "Dados restaurados com sucesso"
```

### Cenário 2: Restaurar em dispositivo novo

```gherkin
DADO que instalei o app em um novo celular
E fiz login com minha conta Google
QUANDO acesso "Restaurar de backup"
ENTÃO vejo lista de backups disponíveis
E posso selecionar qual restaurar
```

### Cenário 3: Restaurar substitui dados locais

```gherkin
DADO que tenho dados locais
E restauro um backup
QUANDO confirmo "Substituir dados locais"
ENTÃO TODOS os dados locais são apagados
E os dados do backup são importados
E vejo os dados do backup na home
```

### Cenário 4: Restaurar com merge

```gherkin
DADO que tenho dados locais
E restauro um backup
QUANDO escolho "Mesclar com dados locais"
ENTÃO dados são mesclados (last-write-wins)
E dados únicos de ambas fontes são mantidos
```

### Cenário 5: Restaurar sem backups

```gherkin
DADO que não tenho backups no Google Drive
QUANDO acesso "Backups salvos"
ENTÃO vejo mensagem "Nenhum backup encontrado"
E vejo sugestão para fazer primeiro backup
```

### Cenário 6: Erro de download

```gherkin
DADO que seleciono um backup para restaurar
QUANDO a conexão falha durante download
ENTÃO vejo mensagem de erro
E os dados locais não são alterados
E posso tentar novamente
```

---

## UI/UX

### Tela: Lista de Backups

```
┌────────────────────────────────┐
│ ← Backups Salvos               │
├────────────────────────────────┤
│                                │
│ Selecione um backup para       │
│ restaurar:                     │
│                                │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ 📦 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 15/03/2026 14:20        │ │
│ │ 2 pets • 14.8 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Restauração

```
┌────────────────────────────────┐
│      Restaurar Backup          │
├────────────────────────────────┤
│                                │
│ Backup de 18/03/2026 10:30     │
│ 2 pets • 15.4 KB              │
│                                │
│ ⚠️ Você tem dados locais.      │
│ O que deseja fazer?            │
│                                │
│ ○ Substituir dados locais      │
│   (apaga tudo e restaura)      │
│                                │
│ ● Mesclar com dados locais     │
│   (mantém dados mais recentes) │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │  RESTAURAR   │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### Estado: Restaurando

```
┌────────────────────────────────┐
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Restaurando backup...     │
│      Baixando dados            │
│                                │
│      Não feche o app           │
│                                │
│                                │
└────────────────────────────────┘
```

### Estado: Sem Backups

```
┌────────────────────────────────┐
│ ← Backups Salvos               │
├────────────────────────────────┤
│                                │
│                                │
│         📭                     │
│                                │
│   Nenhum backup encontrado     │
│                                │
│   Faça seu primeiro backup     │
│   para proteger seus dados.    │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### RestoreBackupUseCase

```kotlin
class RestoreBackupUseCase(
    private val premiumRepository: PremiumRepository,
    private val backupStorageRepository: BackupStorageRepository,
    private val importDataUseCase: ImportDataUseCase,
    private val database: PetitDatabase
) {
    sealed class RestoreMode {
        object Replace : RestoreMode()
        object Merge : RestoreMode()
    }
    
    suspend operator fun invoke(
        fileId: String,
        mode: RestoreMode
    ): Result<RestoreResult> {
        // Verificar premium
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Restauração requer plano premium"))
        }
        
        // Baixar backup
        val exportBundle = backupStorageRepository.downloadBackup(fileName)
            .getOrElse { return Result.failure(it) }
        
        // Aplicar restauração
        return when (mode) {
            is RestoreMode.Replace -> replaceAllData(exportBundle)
            is RestoreMode.Merge -> mergeData(exportBundle)
        }
    }
    
    private suspend fun replaceAllData(bundle: ExportBundle): Result<RestoreResult> {
        return database.withTransaction {
            // Limpar todos os dados locais
            database.petDao().deleteAll()
            database.weightEntryDao().deleteAll()
            database.vaccinationDao().deleteAll()
            database.dewormingDao().deleteAll()
            database.reminderDao().deleteAll()
            
            // Importar dados do backup
            importDataUseCase.import(bundle, ConflictResolution.REPLACE)
            
            Result.success(RestoreResult(
                petsRestored = bundle.pets.size,
                totalEntries = bundle.weightEntries.size + 
                              bundle.vaccinationEntries.size + 
                              bundle.dewormingEntries.size
            ))
        }
    }
    
    private suspend fun mergeData(bundle: ExportBundle): Result<RestoreResult> {
        return importDataUseCase.import(bundle, ConflictResolution.MERGE)
            .map { 
                RestoreResult(
                    petsRestored = bundle.pets.size,
                    totalEntries = bundle.weightEntries.size + 
                                  bundle.vaccinationEntries.size + 
                                  bundle.dewormingEntries.size,
                    merged = true
                )
            }
    }
}

data class RestoreResult(
    val petsRestored: Int,
    val totalEntries: Int,
    val merged: Boolean = false
)
```

### ViewModel

```kotlin
class RestoreViewModel(
    private val backupStorageRepository: BackupStorageRepository,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()
    
    init {
        loadBackups()
    }
    
    private fun loadBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        backups = backups,
                        isEmpty = backups.isEmpty()
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )}
                }
        }
    }
    
    fun restoreBackup(fileId: String, mode: RestoreBackupUseCase.RestoreMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            
            restoreBackupUseCase(fileId, mode)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isRestoring = false,
                        restoreSuccess = true,
                        successMessage = "Restaurados ${result.petsRestored} pets e ${result.totalEntries} registros"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isRestoring = false,
                        errorMessage = error.message
                    )}
                }
        }
    }
}

data class RestoreUiState(
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val backups: List<BackupInfo> = emptyList(),
    val isEmpty: Boolean = false,
    val restoreSuccess: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

### Download do Backup

```kotlin
// Em BackupStorageRepositoryImpl
override suspend fun downloadBackup(fileName: String): Result<ExportBundle> {
    return withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("backups/$userId/$fileName")
            val MAX_SIZE = 10 * 1024 * 1024L
            val bytes = ref.getBytes(MAX_SIZE).await()
            
            val json = bytes.decodeToString()
            val exportBundle = Json.decodeFromString<ExportBundle>(json)
            
            Result.success(exportBundle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Fluxo de Onboarding (Device Novo)

```kotlin
class OnboardingViewModel(...) {
    
    fun checkForBackups() {
        viewModelScope.launch {
            // Verificar se usuário tem backups
            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    if (backups.isNotEmpty()) {
                        // Mostrar opção de restaurar
                        _showRestoreOption.value = true
                    }
                }
        }
    }
}
```

---

## Definition of Done

- [ ] Lista de backups carrega do Drive
- [ ] Seleção de backup para restaurar
- [ ] Dialog de confirmação com opções
- [ ] Modo "Replace" funciona
- [ ] Modo "Merge" funciona
- [ ] Progresso exibido durante download
- [ ] Erro de rede tratado
- [ ] Estado vazio quando sem backups
- [ ] Verificação de premium
- [ ] Testes unitários
