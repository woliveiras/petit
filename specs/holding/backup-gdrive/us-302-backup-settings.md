# US-302: Configurações de Backup

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Auto Sync  
**Fase**: 4

---

## História

> Como usuário logado,  
> Eu quero configurar como o backup automático funciona,  
> Para que eu possa otimizar consumo de dados e bateria.

---

## Cenários de Aceite

### Cenário 1: Ativar/desativar backup automático

```gherkin
DADO que estou logado com Google
QUANDO acesso Configurações > Backup Automático
E ativo o toggle "Backup automático"
ENTÃO o backup diário às 2h é agendado
E vejo "Próximo backup: hoje/amanhã às 2h"

QUANDO desativo o toggle
ENTÃO o agendamento é cancelado
E vejo "Backup automático desativado"
```

### Cenário 2: Configurar Wi-Fi only

```gherkin
DADO que backup automático está ativado
E "Apenas em Wi-Fi" está desativado
QUANDO ativo "Apenas em Wi-Fi"
ENTÃO backups futuros só executam em Wi-Fi
E o agendamento atual é ajustado

DADO que estou em rede móvel às 2h
E "Apenas em Wi-Fi" está ativado
QUANDO o backup deveria executar
ENTÃO é adiado até conectar em Wi-Fi
```

### Cenário 3: Ver histórico de backups

```gherkin
DADO que tenho backups automáticos realizados
QUANDO acesso "Ver histórico"
ENTÃO vejo lista dos últimos backups
E cada item mostra:
  - Data/hora
  - Se foi automático ou manual
  - Status (sucesso/falha)
```

### Cenário 5: Notificação de backup

```gherkin
DADO que "Notificar após backup" está ativado
QUANDO um backup automático é realizado com sucesso
ENTÃO recebo uma notificação silenciosa
"Backup realizado: 2 pets, 15 KB"

DADO que "Notificar após backup" está desativado
QUANDO um backup é realizado
ENTÃO NÃO recebo notificação
```

### Cenário 6: Forçar backup agora

```gherkin
DADO que estou na tela de configurações de backup
QUANDO toco em "Fazer backup agora"
ENTÃO um backup é executado imediatamente
E o timer do próximo backup automático é resetado
```

---

## UI/UX

### Tela: Configurações de Backup Automático

```
┌────────────────────────────────┐
│ ← Backup Automático            │
├────────────────────────────────┤
│                                │
│ ☁️ BACKUP AUTOMÁTICO           │
│ ┌────────────────────────────┐ │
│ │ Ativar                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são salvos       │
│ automaticamente no Firebase   │
│ Storage, mesmo com o app      │
│ fechado.                      │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Último: Hoje 10:30      │ │
│ │ ⏰ Próximo: Amanhã 10:30   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ CONFIGURAÇÕES               │
│                                │
│ ┌────────────────────────────┐ │
│ │ Frequência                 │ │
│ │ A cada 24 horas          ▶ │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Apenas em Wi-Fi      [ON]  │ │
│ │ Economiza dados móveis     │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Notificar sucesso   [OFF]  │ │
│ │ Mostra notificação após    │ │
│ │ cada backup                │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
│ Ver histórico de backups    ▶  │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Frequência

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│ Frequência do backup           │
│                                │
│ ○ A cada 6 horas               │
│   Mais proteção, mais dados    │
│                                │
│ ● A cada 24 horas              │
│   Recomendado                  │
│                                │
│ ○ Uma vez por semana           │
│   Menor consumo                │
│                                │
└────────────────────────────────┘
```

### Tela: Histórico de Backups

```
┌────────────────────────────────┐
│ ← Histórico de Backups         │
├────────────────────────────────┤
│                                │
│ Março 2026                     │
│ ┌────────────────────────────┐ │
│ │ ✅ 18/03 10:30  Automático │ │
│ │    2 pets • 15.4 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 17/03 10:30  Automático │ │
│ │    2 pets • 15.2 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 16/03 14:00  Manual     │ │
│ │    2 pets • 15.1 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ❌ 15/03 10:30  Automático │ │
│ │    Falhou: Sem conexão     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### ViewModel

```kotlin
class BackupSettingsViewModel(
    private val backupPreferences: BackupPreferencesRepository,
    private val backupScheduler: BackupScheduler,
    private val createBackupUseCase: CreateBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update { it.copy(
            isAutoBackupEnabled = backupPreferences.isAutoBackupEnabled(),
            intervalHours = backupPreferences.getBackupIntervalHours(),
            isWifiOnly = backupPreferences.isWifiOnly(),
            shouldNotifyOnSuccess = backupPreferences.shouldNotifyOnSuccess(),
            lastBackupTimestamp = backupPreferences.getLastBackupTimestamp(),
            lastBackupError = backupPreferences.getLastBackupError(),
            nextBackupTimestamp = backupScheduler.getNextBackupTime()
        )}
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        backupPreferences.setAutoBackupEnabled(enabled)
        
        if (enabled) {
            backupScheduler.scheduleAutoBackup()
        } else {
            backupScheduler.cancelAutoBackup()
        }
        
        loadSettings()
    }

    fun setIntervalHours(hours: Int) {
        backupPreferences.setBackupIntervalHours(hours)
        
        if (backupPreferences.isAutoBackupEnabled()) {
            backupScheduler.scheduleAutoBackup()  // Re-schedule com nova frequência
        }
        
        loadSettings()
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        backupPreferences.setWifiOnly(wifiOnly)
        
        if (backupPreferences.isAutoBackupEnabled()) {
            backupScheduler.scheduleAutoBackup()  // Re-schedule com nova constraint
        }
        
        loadSettings()
    }

    fun setNotifyOnSuccess(notify: Boolean) {
        backupPreferences.setNotifyOnSuccess(notify)
        loadSettings()
    }

    fun backupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            
            createBackupUseCase()
                .onSuccess {
                    _uiState.update { it.copy(
                        isBackingUp = false,
                        successMessage = "Backup realizado!"
                    )}
                    
                    // Re-schedule para resetar timer
                    if (backupPreferences.isAutoBackupEnabled()) {
                        backupScheduler.scheduleAutoBackup()
                    }
                    
                    loadSettings()
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

data class BackupSettingsUiState(
    val isAutoBackupEnabled: Boolean = false,
    val intervalHours: Int = 24,
    val isWifiOnly: Boolean = true,
    val shouldNotifyOnSuccess: Boolean = false,
    val lastBackupTimestamp: Long? = null,
    val lastBackupError: String? = null,
    val nextBackupTimestamp: Long? = null,
    val isBackingUp: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
```

### Composable

```kotlin
@Composable
fun BackupSettingsScreen(
    viewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle principal
        SwitchPreference(
            title = "Backup automático",
            subtitle = "Salva seus dados automaticamente",
            checked = uiState.isAutoBackupEnabled,
            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
        )

        if (uiState.isAutoBackupEnabled) {
            // Status
            BackupStatusCard(
                lastBackup = uiState.lastBackupTimestamp,
                nextBackup = uiState.nextBackupTimestamp,
                lastError = uiState.lastBackupError
            )

            // Frequência
            ListPreference(
                title = "Frequência",
                value = uiState.intervalHours,
                options = listOf(
                    6 to "A cada 6 horas",
                    24 to "A cada 24 horas",
                    168 to "Uma vez por semana"
                ),
                onValueChange = { viewModel.setIntervalHours(it) }
            )

            // Wi-Fi only
            SwitchPreference(
                title = "Apenas em Wi-Fi",
                subtitle = "Economiza dados móveis",
                checked = uiState.isWifiOnly,
                onCheckedChange = { viewModel.setWifiOnly(it) }
            )

            // Notificação
            SwitchPreference(
                title = "Notificar após backup",
                subtitle = "Mostra notificação de sucesso",
                checked = uiState.shouldNotifyOnSuccess,
                onCheckedChange = { viewModel.setNotifyOnSuccess(it) }
            )
        }

        // Botão backup agora
        Button(
            onClick = { viewModel.backupNow() },
            enabled = !uiState.isBackingUp
        ) {
            if (uiState.isBackingUp) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Fazer backup agora")
            }
        }
    }
}
```

---

## Definition of Done

- [ ] Toggle ativa/desativa backup automático
- [ ] Seletor de frequência funciona
- [ ] Toggle Wi-Fi only funciona
- [ ] Toggle de notificação funciona
- [ ] Status de último backup exibido
- [ ] Próximo backup agendado exibido
- [ ] Botão "Backup agora" funciona
- [ ] Histórico de backups acessível
- [ ] Persiste configurações no DataStore
- [ ] Testes unitários
