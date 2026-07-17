# US-201: Backup Manual

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Cloud Backup  
**Fase**: 3

---

## História

> Como usuário do app,  
> Eu quero fazer backup dos meus dados no Google Drive,  
> Para que eu possa recuperá-los em caso de perda do celular.

---

## Cenários de Aceite

### Cenário 1: Fazer backup com sucesso (usuário já logado)

```gherkin
DADO que estou logado com Google
E tenho conexão com internet
QUANDO acesso Configurações > "Backup Google Drive"
E toco em "Fazer backup agora"
ENTÃO vejo indicador de progresso
E o backup é enviado para o Google Drive (appDataFolder)
E vejo mensagem "Backup realizado com sucesso"
E vejo data/hora do último backup
```

### Cenário 2: Backup sem internet

```gherkin
DADO que estou sem conexão de internet
QUANDO tento fazer backup
ENTÃO vejo mensagem "Sem conexão. Conecte-se à internet para fazer backup."
E o backup não é iniciado
```

### Cenário 3: Backup sem estar logado (ativa login)

```gherkin
DADO que não estou logado
QUANDO tento fazer backup
ENTÃO vejo dialog explicando que é necessário login Google
E tenho opção "Entrar com Google"
QUANDO faço login com sucesso
ENTÃO o backup é iniciado automaticamente
```

### Cenário 4: Primeiro backup

```gherkin
DADO que nunca fiz backup antes
QUANDO faço meu primeiro backup
ENTÃO o arquivo é criado no appDataFolder do Google Drive
E o metadata é inicializado
E vejo "Backup realizado com sucesso"
```

### Cenário 5: Backup subsequente

```gherkin
DADO que já tenho backups anteriores
QUANDO faço novo backup
ENTÃO um novo arquivo é criado (não substitui o anterior)
E o metadata é atualizado
E backups antigos são mantidos (até o limite)
```

### Cenário 6: Erro durante backup

```gherkin
DADO que inicio um backup
QUANDO ocorre erro (rede cai, quota excedida, etc.)
ENTÃO vejo mensagem de erro específica
E o backup parcial é descartado
E posso tentar novamente
```

---

## UI/UX

### Tela: Backup na Nuvem

```
┌────────────────────────────────┐
│ ← Backup na Nuvem              │
├────────────────────────────────┤
│                                │
│ ☁️ GOOGLE DRIVE                 │
│                                │
│ Conectado como:                │
│ pessoa-a@example.com           │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 ÚLTIMO BACKUP               │
│ ┌────────────────────────────┐ │
│ │ 18/03/2026 às 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ 📂 BACKUPS SALVOS          ▶   │
│ 3 backups (45.2 KB total)      │
│                                │
├────────────────────────────────┤
│                                │
│ ℹ️ Os backups são armazenados  │
│ no appDataFolder do Google    │
│ Drive (oculto).               │
│                                │
└────────────────────────────────┘
```

### Tela: Backup na Nuvem (Fazendo Backup)

```
┌────────────────────────────────┐
│ ← Backup na Nuvem              │
├────────────────────────────────┤
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Fazendo backup...         │
│      Enviando dados            │
│                                │
│      Não feche o app           │
│                                │
│                                │
└────────────────────────────────┘
```

### Estado: Sucesso

```
┌────────────────────────────────┐
│                                │
│            ✅                  │
│                                │
│   Backup realizado com         │
│   sucesso!                     │
│                                │
│   18/03/2026 às 10:30          │
│   15.4 KB                      │
│                                │
│   ┌────────────────────────┐   │
│   │          OK            │   │
│   └────────────────────────┘   │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### BackupStorageRepository

```kotlin
interface BackupStorageRepository {
    suspend fun createBackup(data: ExportBundle): Result<BackupInfo>
    suspend fun listBackups(): Result<List<BackupInfo>>
    suspend fun downloadBackup(fileName: String): Result<ExportBundle>
    suspend fun deleteBackup(fileName: String): Result<Unit>
    suspend fun getBackupMetadata(): Result<BackupMetadata?>
}

data class BackupInfo(
    val fileId: String,
    val fileName: String,
    val createdAt: Instant,
    val sizeBytes: Long,
    val petCount: Int,
    val appVersion: String
)

data class BackupMetadata(
    val backups: List<BackupInfo>,
    val lastBackupAt: Instant?
)
```

### GoogleDriveBackupRepository

```kotlin
class GoogleDriveBackupRepository(
    private val driveService: Drive,
    private val authRepository: AuthRepository
) : BackupStorageRepository {
    
    override suspend fun createBackup(data: ExportBundle): Result<BackupInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(data)
                val timestamp = Instant.now().toString().replace(":", "-")
                val fileName = "petit_backup_$timestamp.json"
                
                // Upload para appDataFolder do Google Drive
                val fileMetadata = com.google.api.services.drive.model.File()
                    .setName(fileName)
                    .setParents(listOf("appDataFolder"))
                
                val mediaContent = ByteArrayContent("application/json", json.toByteArray())
                
                val file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, createdTime, size")
                    .execute()
                
                val backupInfo = BackupInfo(
                    fileId = file.id,
                    fileName = file.name,
                    createdAt = Instant.parse(file.createdTime.toString()),
                    sizeBytes = file.size,
                    petCount = data.pets.size,
                    appVersion = data.metadata.appVersion
                )
                
                Result.success(backupInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### BackupUseCase

```kotlin
class CreateBackupUseCase(
    private val authRepository: AuthRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository,
    private val connectivityManager: ConnectivityManager
) {
    suspend operator fun invoke(): Result<BackupInfo> {
        // Verificar login (se não logado, retorna erro que dispara fluxo de login)
        val currentUser = authRepository.getCurrentUser()
            ?: return Result.failure(LoginRequiredException("Login necessário para backup"))
        
        // Verificar conexão
        if (!connectivityManager.isConnected()) {
            return Result.failure(NoConnectionException("Sem conexão de internet"))
        }
        
        // Exportar dados
        val exportBundle = exportDataUseCase.exportAll()
        
        // Enviar para Google Drive
        return googleDriveBackupRepository.createBackup(exportBundle)
    }
}
```

### ViewModel

```kotlin
class BackupViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val backupStorageRepository: BackupStorageRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    init {
        loadBackupInfo()
    }
    
    private fun loadBackupInfo() {
        viewModelScope.launch {
            backupStorageRepository.getBackupMetadata()
                .onSuccess { metadata ->
                    _uiState.update { it.copy(
                        lastBackup = metadata?.backups?.firstOrNull(),
                        totalBackups = metadata?.backups?.size ?: 0,
                        isLoading = false
                    )}
                }
        }
    }
    
    fun createBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            
            createBackupUseCase()
                .onSuccess { backupInfo ->
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        lastBackup = backupInfo,
                        successMessage = "Backup realizado com sucesso!"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        errorMessage = error.message
                    )}
                }
        }
    }
}

data class BackupUiState(
    val isLoading: Boolean = true,
    val isBackingUp: Boolean = false,
    val lastBackup: BackupInfo? = null,
    val totalBackups: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

---

## Definition of Done

- [ ] Botão "Fazer backup agora" funciona
- [ ] Progresso exibido durante backup
- [ ] Arquivo criado no bucket privado do usuário
- [ ] Metadata atualizado após backup
- [ ] Mensagem de sucesso exibida
- [ ] Erro de rede tratado
- [ ] Verificação de premium implementada
- [ ] Data do último backup exibida
- [ ] Testes unitários
- [ ] Testes de integração com Firebase Storage (mock)
