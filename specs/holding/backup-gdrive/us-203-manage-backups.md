# US-203: Gerenciar Backups

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Cloud Backup  
**Fase**: 3

---

## História

> Como usuário logado,  
> Eu quero gerenciar meus backups no Google Drive,  
> Para que eu possa ver o histórico e limpar backups antigos se necessário.

---

## Cenários de Aceite

### Cenário 1: Ver lista de backups

```gherkin
DADO que estou logado com Google
E tenho múltiplos backups salvos
QUANDO acesso "Backups salvos"
ENTÃO vejo lista com todos os backups
E cada item mostra:
  - Data e hora do backup
  - Quantidade de pets
  - Tamanho do arquivo
  - Versão do app
```

### Cenário 2: Ver detalhes do backup

```gherkin
DADO que estou na lista de backups
QUANDO toco em um backup
ENTÃO vejo detalhes completos:
  - Data e hora
  - Conteúdo (X pets, Y pesagens, Z vacinas)
  - Tamanho
  - Versão do app que criou
E vejo opções: Restaurar, Deletar
```

### Cenário 3: Deletar backup específico

```gherkin
DADO que estou nos detalhes de um backup
QUANDO toco em "Deletar"
E confirmo a exclusão
ENTÃO o backup é removido do Google Drive
E não aparece mais na lista
```

### Cenário 4: Deletar múltiplos backups

```gherkin
DADO que estou na lista de backups
QUANDO ativo modo de seleção (long press)
E seleciono múltiplos backups
E toco em "Deletar selecionados"
E confirmo
ENTÃO todos os backups selecionados são removidos
```

### Cenário 5: Limite de backups manuais

```gherkin
DADO que tenho 10 backups manuais salvos (limite)
QUANDO faço um novo backup manual
ENTÃO o backup manual mais antigo é removido automaticamente
E o novo backup é adicionado
E vejo notificação "Backup antigo removido para liberar espaço"
```

### Cenário 7: Backups após exclusão de conta

```gherkin
DADO que tenho backups salvos no Google Drive
QUANDO excluo minha conta do app
ENTÃO os backups são mantidos por 90 dias (período de grace)
E vejo aviso "Sua conta será permanentemente excluída em X dias."
E após 90 dias sem reativação, os backups são purgados automaticamente
```

### Cenário 8: Backups após exclusão de conta

```gherkin
DADO que tenho backups salvos
QUANDO deleto minha conta
ENTÃO os backups são agendados para purge em 30 dias
E após 30 dias, todos os arquivos no bucket do usuário são removidos permanentemente
```

### Cenário 6: Espaço total usado

```gherkin
DADO que estou na tela de backup
QUANDO vejo a seção "Backups salvos"
ENTÃO vejo o total de backups
E o espaço total usado (ex: "3 backups • 45.2 KB")
```

---

## UI/UX

### Tela: Detalhes do Backup

```
┌────────────────────────────────┐
│ ← Detalhes do Backup           │
├────────────────────────────────┤
│                                │
│ 📦 BACKUP                      │
│                                │
│ 18/03/2026 às 10:30            │
│ Versão do app: 1.0.0           │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 CONTEÚDO                    │
│ ┌────────────────────────────┐ │
│ │ 🐱 Pets           2       │ │
│ │ ⚖️ Pesagens       15       │ │
│ │ 💉 Vacinas         8       │ │
│ │ 🪱 Vermífugos      6       │ │
│ │ 🔔 Lembretes       3       │ │
│ └────────────────────────────┘ │
│                                │
│ 📁 Tamanho: 15.4 KB            │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │       RESTAURAR            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │        DELETAR             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Lista com Seleção Múltipla

```
┌────────────────────────────────┐
│ ← Backups Salvos    [🗑️] [✓]   │
├────────────────────────────────┤
│ 2 selecionados                 │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ ☑️ 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☐ 15/03/2026 14:20         │ │
│ │ 2 pets • 14.8 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☑️ 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Exclusão

```
┌────────────────────────────────┐
│     Deletar Backup?            │
├────────────────────────────────┤
│                                │
│ ⚠️ Esta ação não pode ser      │
│ desfeita.                      │
│                                │
│ O backup será removido         │
│ permanentemente do Firebase    │
│ Storage.                       │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │   DELETAR    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Listar Backups

```kotlin
// Em BackupStorageRepositoryImpl
override suspend fun listBackups(): Result<List<BackupInfo>> {
    return withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val listResult = storage.reference.child("backups/$userId").listAll().await()
            
            val backups = listResult.items
                .filter { it.name.startsWith("petit_backup_") && it.name.endsWith(".json") }
                .map { ref ->
                    val metadata = ref.metadata.await()
                    BackupInfo(
                        fileName = ref.name,
                        path = ref.path,
                        createdAt = Instant.ofEpochMilli(metadata.creationTimeMillis),
                        sizeBytes = metadata.sizeBytes,
                        petCount = 0,  // Carregar do metadata
                        appVersion = metadata.getCustomMetadata("appVersion") ?: "unknown"
                    )
                }
            
            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Deletar Backup

```kotlin
override suspend fun deleteBackup(fileId: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            driveService.files().delete(fileId).execute()
            
            // Atualizar metadata
            removeFromMetadata(fileId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

suspend fun deleteMultipleBackups(fileIds: List<String>): Result<Int> {
    var deletedCount = 0
    
    fileIds.forEach { fileId ->
        deleteBackup(fileId).onSuccess { deletedCount++ }
    }
    
    return Result.success(deletedCount)
}
```

### Auto-cleanup de Backups Antigos

```kotlin
class BackupCleanupUseCase(
    private val backupStorageRepository: BackupStorageRepository
) {
    companion object {
        const val MAX_BACKUPS = 10
    }
    
    suspend fun cleanupOldBackups(): Result<Int> {
        val backups = backupStorageRepository.listBackups()
            .getOrElse { return Result.failure(it) }
        
        if (backups.size <= MAX_BACKUPS) {
            return Result.success(0)
        }
        
        // Ordenar por data (mais antigo primeiro para deletar)
        val toDelete = backups
            .sortedBy { it.createdAt }
            .take(backups.size - MAX_BACKUPS)
        
        var deletedCount = 0
        toDelete.forEach { backup ->
            backupStorageRepository.deleteBackup(backup.fileId)
                .onSuccess { deletedCount++ }
        }
        
        return Result.success(deletedCount)
    }
}
```

### ViewModel

```kotlin
class ManageBackupsViewModel(
    private val backupStorageRepository: BackupStorageRepository,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ManageBackupsUiState())
    val uiState: StateFlow<ManageBackupsUiState> = _uiState.asStateFlow()
    
    fun loadBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        backups = backups,
                        totalSize = backups.sumOf { it.sizeBytes }
                    )}
                }
        }
    }
    
    fun toggleSelection(fileId: String) {
        _uiState.update { state ->
            val newSelection = if (fileId in state.selectedIds) {
                state.selectedIds - fileId
            } else {
                state.selectedIds + fileId
            }
            state.copy(
                selectedIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _uiState.value.selectedIds.toList()
            _uiState.update { it.copy(isDeleting = true) }
            
            backupStorageRepository.deleteMultipleBackups(toDelete)
                .onSuccess { count ->
                    _uiState.update { it.copy(
                        isDeleting = false,
                        selectedIds = emptySet(),
                        isSelectionMode = false
                    )}
                    loadBackups()  // Recarregar lista
                }
        }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )}
    }
}

data class ManageBackupsUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val backups: List<BackupInfo> = emptyList(),
    val totalSize: Long = 0,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)
```

---

## Definition of Done

- [ ] Lista de backups com detalhes
- [ ] Tela de detalhes do backup
- [ ] Deletar backup individual
- [ ] Seleção múltipla para deletar
- [ ] Confirmação antes de deletar
- [ ] Auto-cleanup quando excede limite
- [ ] Total de espaço usado exibido
- [ ] Tratamento de erros
- [ ] Testes unitários
