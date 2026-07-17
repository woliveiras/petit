# US-301: Backup Automático

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Auto Sync  
**Fase**: 4

---

## História

> Como usuário logado,  
> Eu quero que meus dados sejam salvos automaticamente no Google Drive todos os dias às 2h da madrugada,  
> Para que eu não precise me preocupar em fazer backup manualmente.

---

## Cenários de Aceite

### Cenário 1: Backup automático ativado por padrão (usuário logado)

```gherkin
DADO que estou logado com Google
QUANDO habilito backup automático nas configurações
ENTÃO o WorkManager agenda backup diário às 2h da madrugada
E vejo "Backup automático ativado — próximo às 2h"
```

### Cenário 2: Backup diário executa em background

```gherkin
DADO que backup automático está ativado
QUANDO chega 2h da madrugada
ENTÃO o backup é executado automaticamente
MESMO que o app esteja fechado
E não preciso abrir o app
E o backup é salvo no Google Drive
```

### Cenário 3: Backup apenas em Wi-Fi

```gherkin
DADO que "Backup apenas em Wi-Fi" está ativado
E estou conectado em rede móvel (4G/5G)
QUANDO o backup automático deveria executar
ENTÃO o backup é adiado
E executa quando conectar em Wi-Fi
```

### Cenário 4: Backup apenas se logado

```gherkin
DADO que backup automático está agendado
E não estou mais logado (logout)
QUANDO chega 2h da madrugada
ENTÃO o backup NÃO é executado
E vejo notificação "Faça login para continuar backups automáticos"
```

### Cenário 5: Configuração Wi-Fi only respeitada

```gherkin
DADO que "Backup apenas em Wi-Fi" está ativado
E estou conectado em rede móvel (4G/5G) às 2h
QUANDO o backup automático deveria executar
ENTÃO o backup é adiado
E executa quando conectar em Wi-Fi
E vejo notificação "Aguardando Wi-Fi para backup"
```

### Cenário 6: Sem internet

```gherkin
DADO que não tenho conexão de internet
QUANDO o backup automático deveria executar
ENTÃO o backup falha silenciosamente
E será tentado novamente na próxima vez
E posso ver "Último backup: há 2 dias (falhou)" nas configurações
```

---

## UI/UX

### Configurações de Backup

```
┌────────────────────────────────┐
│ ← Backup Automático            │
├────────────────────────────────┤
│                                │
│ ☁️ BACKUP AUTOMÁTICO           │
│ ┌────────────────────────────┐ │
│ │ Ativado               [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Último backup:             │ │
│ │ Hoje às 10:30 ✅           │ │
│ │                            │ │
│ │ Próximo backup:            │ │
│ │ Amanhã às 10:30            │ │
│ └────────────────────────────┘ │
│                                │
│ ⚙️ CONFIGURAÇÕES               │
│ ┌────────────────────────────┐ │
│ │ Frequência          24h  ▶ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Apenas em Wi-Fi      [ON]  │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Notificar sucesso    [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Notificação de Backup

```
┌────────────────────────────────┐
│ 🐱 Petit                         │
│ Backup realizado com sucesso   │
│ 2 pets salvos • 15.4 KB       │
│                                │
│                      [Ignorar] │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### AutoBackupWorker

```kotlin
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Verificar premium
        if (!premiumRepository.isPremium()) {
            // Cancelar trabalho periódico
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(WORK_NAME)
            return Result.failure()
        }
        
        // Verificar Wi-Fi se necessário
        val wifiOnly = backupPreferences.isWifiOnly()
        if (wifiOnly && !isOnWifi()) {
            return Result.retry()  // Tentar depois
        }
        
        // Executar backup
        return backupUseCase()
            .map { backupInfo ->
                // Atualizar timestamp
                backupPreferences.setLastBackupTimestamp(System.currentTimeMillis())
                
                // Notificar se configurado
                if (backupPreferences.shouldNotifyOnSuccess()) {
                    notificationHelper.showBackupSuccessNotification(backupInfo)
                }
                
                Result.success()
            }
            .getOrElse { error ->
                backupPreferences.setLastBackupError(error.message)
                Result.retry()
            }
    }
    
    private fun isOnWifi(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    companion object {
        const val WORK_NAME = "auto_backup_work"
    }
}
```

### Scheduling do Backup Periódico

```kotlin
class BackupScheduler(
    private val workManager: WorkManager,
    private val backupPreferences: BackupPreferencesRepository
) {
    fun scheduleAutoBackup() {
        val intervalHours = backupPreferences.getBackupIntervalHours()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (backupPreferences.isWifiOnly()) 
                    NetworkType.UNMETERED 
                else 
                    NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .addTag("auto_backup")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    fun cancelAutoBackup() {
        workManager.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }
    
    fun getNextBackupTime(): Long? {
        val workInfo = workManager.getWorkInfosForUniqueWork(AutoBackupWorker.WORK_NAME)
            .get()
            .firstOrNull()
        
        return workInfo?.nextScheduleTimeMillis
    }
}
```

### WorkerFactory para Injeção de Dependência

```kotlin
class PetitWorkerFactory(
    private val premiumRepository: PremiumRepository,
    private val backupUseCase: CreateBackupUseCase,
    private val backupPreferences: BackupPreferencesRepository,
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            AutoBackupWorker::class.java.name -> AutoBackupWorker(
                appContext,
                workerParameters,
                premiumRepository,
                backupUseCase,
                backupPreferences,
                notificationHelper
            )
            else -> null
        }
    }
}
```

### BackupPreferencesRepository

```kotlin
interface BackupPreferencesRepository {
    fun isAutoBackupEnabled(): Boolean
    fun setAutoBackupEnabled(enabled: Boolean)
    
    fun getBackupIntervalHours(): Int
    fun setBackupIntervalHours(hours: Int)
    
    fun isWifiOnly(): Boolean
    fun setWifiOnly(wifiOnly: Boolean)
    
    fun shouldNotifyOnSuccess(): Boolean
    fun setNotifyOnSuccess(notify: Boolean)
    
    fun getLastBackupTimestamp(): Long?
    fun setLastBackupTimestamp(timestamp: Long)
    
    fun getLastBackupError(): String?
    fun setLastBackupError(error: String?)
}

class BackupPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : BackupPreferencesRepository {
    
    companion object {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        val WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        val NOTIFY_ON_SUCCESS = booleanPreferencesKey("backup_notify_success")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }
    
    // Implementações...
}
```

---

## Frequências Disponíveis

| Opção | Horas | Descrição |
|-------|-------|-----------|
| Frequente | 6 | A cada 6 horas |
| Diário | 24 | Uma vez por dia |
| Semanal | 168 | Uma vez por semana |

---

## Definition of Done

- [ ] Worker de backup automático implementado
- [ ] Scheduling periódico funciona
- [ ] Cancelamento ao perder premium
- [ ] Respeita configuração Wi-Fi only
- [ ] Retry com backoff exponencial
- [ ] Status de último backup visível
- [ ] Próximo backup agendado visível
- [ ] Notificação opcional de sucesso
- [ ] Testes unitários do Worker
- [ ] Testes de integração do scheduling
