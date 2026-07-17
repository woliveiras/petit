# US-204: Transferência Device-to-Device

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Data Transfer  
**Fase**: 3

---

## História

> Como usuário do app,  
> Eu quero transferir meus dados para outro celular próximo,  
> Para que eu possa compartilhar dados com outro device sem usar a nuvem.

---

## Cenários de Aceite

### Cenário 1: Enviar dados para outro device

```gherkin
DADO que tenho dados locais (pets, pesos, etc.)
E outro device com Petit está próximo
QUANDO acesso Configurações > "Compartilhar dados"
E toco em "Enviar dados"
ENTÃO vejo código de 4 dígitos para compartilhar
E aguardo conexão do receptor
QUANDO receptor insere o código
ENTÃO dados são enviados via Nearby Connections
E vejo "Dados enviados com sucesso"
```

### Cenário 2: Receber dados de outro device

```gherkin
DADO que estou no app
QUANDO acesso Configurações > "Receber dados"
E toco em "Receber de outro celular"
ENTÃO vejo campo para inserir código
QUANDO insiro código de 4 dígitos do transmissor
ENTÃO conexão é estabelecida
E vejo progresso de transferência
QUANDO transferência completa
ENTÃO vejo opção "Substituir" ou "Mesclar" dados
```

### Cenário 3: Transferência sem internet

```gherkin
DADO que ambos devices estão sem internet
MAS estão na mesma rede Wi-Fi OU com Bluetooth ativo
QUANDO inicio transferência
ENTÃO funciona normalmente
(Nearby Connections usa Wi-Fi Direct ou Bluetooth)
```

### Cenário 4: Mesclar dados recebidos

```gherkin
DADO que recebi dados de outro device
E tenho dados locais
QUANDO escolho "Mesclar"
ENTÃO dados são combinados
E duplicatas são resolvidas por ID (UUIDs únicos)
E vejo resumo: "2 pets adicionados, 10 pesagens mescladas"
```

### Cenário 5: Substituir dados locais

```gherkin
DADO que recebi dados de outro device
QUANDO escolho "Substituir"
ENTÃO vejo confirmação "Seus dados locais serão apagados. Continuar?"
QUANDO confirmo
ENTÃO todos dados locais são deletados
E dados recebidos são importados
E vejo "Dados restaurados com sucesso"
```

### Cenário 6: Cancelar transferência

```gherkin
DADO que transferência está em andamento
QUANDO toco em "Cancelar"
ENTÃO transferência é interrompida
E dados parciais são descartados
E ambos devices voltam ao estado inicial
```

### Cenário 7: Erro de conexão

```gherkin
DADO que devices estão muito distantes
OU Bluetooth/Wi-Fi estão desativados
QUANDO tento iniciar transferência
ENTÃO vejo mensagem "Não foi possível conectar. Aproxime os devices e ative Wi-Fi ou Bluetooth."
```

---

## UI/UX

### Tela: Compartilhar Dados (Transmissor)

```
┌────────────────────────────────┐
│ ← Compartilhar Dados           │
├────────────────────────────────┤
│                                │
│        📱 ➡️ 📱                │
│                                │
│  Compartilhe seus dados com    │
│  outro celular próximo         │
│                                │
│ ┌────────────────────────────┐ │
│ │    ENVIAR DADOS            │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Funciona sem internet,      │
│ usando Wi-Fi Direct ou         │
│ Bluetooth.                     │
│                                │
└────────────────────────────────┘
```

### Tela: Aguardando Conexão (Transmissor)

```
┌────────────────────────────────┐
│ ← Aguardando conexão...        │
├────────────────────────────────┤
│                                │
│         🔒                     │
│                                │
│    Código de segurança:        │
│                                │
│        ┌──────────┐            │
│        │   4729   │            │
│        └──────────┘            │
│                                │
│  Peça para o outro celular     │
│  inserir este código.          │
│                                │
│ ┌────────────────────────────┐ │
│ │       CANCELAR             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Tela: Receber Dados (Receptor)

```
┌────────────────────────────────┐
│ ← Receber Dados                │
├────────────────────────────────┤
│                                │
│        📱 ⬅️ 📱                │
│                                │
│  Insira o código mostrado no   │
│  outro celular:                │
│                                │
│  ┌────┬────┬────┬────┐         │
│  │  4 │  7 │  2 │  9 │         │
│  └────┴────┴────┴────┘         │
│                                │
│ ┌────────────────────────────┐ │
│ │      CONECTAR              │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Certifique-se de que Wi-Fi  │
│ ou Bluetooth estão ativos.     │
│                                │
└────────────────────────────────┘
```

### Tela: Transferindo

```
┌────────────────────────────────┐
│ Transferindo...                │
├────────────────────────────────┤
│                                │
│         ████████░░             │
│            80%                 │
│                                │
│  Enviando dados...             │
│  2 pets • 25 registros        │
│                                │
│  Não desligue o app            │
│                                │
└────────────────────────────────┘
```

### Dialog: Escolher Ação (Receptor)

```
┌────────────────────────────────┐
│                                │
│         ✅                     │
│                                │
│   Dados recebidos!             │
│                                │
│   2 pets                      │
│   15 pesagens                  │
│   8 vacinas                    │
│                                │
│ ┌────────────────────────────┐ │
│ │    MESCLAR COM LOCAIS      │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    SUBSTITUIR LOCAIS       │ │
│ └────────────────────────────┘ │
│                                │
│       Cancelar                 │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Nearby Connections API

```kotlin
dependencies {
    // Google Nearby Connections
    implementation("com.google.android.gms:play-services-nearby:VERSION")
}
```

### DeviceTransferRepository

```kotlin
interface DeviceTransferRepository {
    // Transmissor
    suspend fun startAdvertising(): Flow<TransferState>
    suspend fun sendData(endpointId: String, data: ExportBundle): Result<Unit>
    fun stopAdvertising()
    
    // Receptor
    suspend fun startDiscovery(code: String): Flow<TransferState>
    suspend fun receiveData(endpointId: String): Result<ExportBundle>
    fun stopDiscovery()
}

sealed class TransferState {
    object AdvertisingStarted : TransferState()
    data class ConnectionRequested(val endpointId: String, val deviceName: String) : TransferState()
    data class Connected(val endpointId: String) : TransferState()
    data class Transferring(val bytesTransferred: Long, val totalBytes: Long) : TransferState()
    data class TransferComplete(val data: ExportBundle) : TransferState()
    data class Error(val message: String) : TransferState()
}
```

### NearbyTransferRepository (Implementação)

```kotlin
class NearbyTransferRepository(
    private val context: Context,
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
) : DeviceTransferRepository {
    
    override suspend fun startAdvertising(): Flow<TransferState> = callbackFlow {
        val code = generateSecurityCode() // 4 dígitos
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()
        
        connectionsClient.startAdvertising(
            context.packageName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        )
        
        trySend(TransferState.AdvertisingStarted)
        
        awaitClose {
            connectionsClient.stopAdvertising()
        }
    }
    
    private fun generateSecurityCode(): String {
        return (1000..9999).random().toString()
    }
    
    companion object {
        private const val SERVICE_ID = "com.woliveiras.petit.DEVICE_TRANSFER"
    }
}
```

### TransferDataUseCase

```kotlin
class TransferDataUseCase(
    private val exportDataUseCase: ExportDataUseCase,
    private val deviceTransferRepository: DeviceTransferRepository
) {
    suspend fun sendData(endpointId: String): Result<Unit> {
        // Exportar dados locais
        val exportBundle = exportDataUseCase.exportAll()
        
        // Enviar via Nearby Connections
        return deviceTransferRepository.sendData(endpointId, exportBundle)
    }
}
```

### ReceiveDataUseCase

```kotlin
class ReceiveDataUseCase(
    private val deviceTransferRepository: DeviceTransferRepository,
    private val importDataUseCase: ImportDataUseCase
) {
    suspend fun receiveAndMerge(endpointId: String): Result<ImportResult> {
        // Receber dados
        val result = deviceTransferRepository.receiveData(endpointId)
        
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }
        
        val exportBundle = result.getOrNull()!!
        
        // Importar com merge
        return importDataUseCase.importWithMerge(exportBundle)
    }
    
    suspend fun receiveAndReplace(endpointId: String): Result<ImportResult> {
        val result = deviceTransferRepository.receiveData(endpointId)
        
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }
        
        val exportBundle = result.getOrNull()!!
        
        // Importar com substituição total
        return importDataUseCase.importWithReplace(exportBundle)
    }
}
```

---

## Permissões

### AndroidManifest.xml

```xml
<!-- Nearby Connections -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## Segurança

- **Código de 4 dígitos**: Previne conexões não autorizadas
- **Proximity-based**: Funciona apenas com devices próximos (< 10 metros)
- **One-shot transfer**: Conexão é encerrada após transferência
- **No cloud storage**: Dados trafegam diretamente entre devices
- **Encryption**: Nearby Connections usa criptografia automática

---

## Critérios de Aceite

- [ ] Transmissor gera código de 4 dígitos
- [ ] Receptor insere código e conecta com sucesso
- [ ] Transferência funciona sem internet (Wi-Fi Direct / Bluetooth)
- [ ] Opções "Mesclar" e "Substituir" funcionam corretamente
- [ ] Cancelamento interrompe transferência
- [ ] Erros são tratados com mensagens claras
- [ ] Permissões de Bluetooth/Wi-Fi/Localização são solicitadas
- [ ] UI mostra progresso da transferência
- [ ] Funciona com devices Android 6.0+ (API 23+)

---

## Notas de Implementação

- **Strategy**: Usar `Strategy.P2P_POINT_TO_POINT` (1 transmissor, 1 receptor)
- **Service ID**: Deve ser único por app (`com.woliveiras.petit.DEVICE_TRANSFER`)
- **Payload**: Serializar ExportBundle para JSON, enviar como ByteArray
- **Timeout**: 30 segundos sem atividade cancela conexão
- **Battery**: Nearby Connections é otimizado para bateria
- **Reutilização**: Fluxo reutiliza ExportBundle/ImportDataUseCase da Fase 1

---

## Referências

- [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview)
- [Android Strategy.P2P_POINT_TO_POINT](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_POINT_TO_POINT)
