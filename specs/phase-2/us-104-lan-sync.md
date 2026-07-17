# US-104: Sync Contínuo na Rede Local

> **Estado no Petit (2026-07-17): planejado.** Não foram encontrados NSD,
> servidor TCP, `LanSyncWorker`, `LanSyncRepository` ou `SyncIndicator` no
> código atual. Este documento define o comportamento desejado e não deve ser
> lido como funcionalidade entregue. Origem: `getmiw/specs-miw@09b4497`.

**Prioridade**: P0
**Épico**: Compartilhamento Familiar
**Fase**: 2

---

## História

> Como usuário do app,
> Eu quero que meus dados sincronizem automaticamente com o dispositivo de outra pessoa cuidadora quando estamos na mesma rede Wi-Fi,
> Para que ambos tenhamos sempre os dados atualizados sem precisar fazer transferências manuais.

---

## Cenários de Aceite

### Cenário 1: Auto-discovery na mesma Wi-Fi

```gherkin
DADO que estou pareado com outro dispositivo
E ambos estamos na mesma rede Wi-Fi (ex: Wi-Fi de casa)
QUANDO abro o app
ENTÃO o app registra um serviço NSD "_petit._tcp"
E inicia discovery para encontrar outros devices Petit na rede
E ao encontrar, inicia sync automática em background
```

### Cenário 2: Sync incremental automático

```gherkin
DADO que estou conectado ao dispositivo parceiro via NSD
E fiz alterações locais (novo peso, nova vacina)
QUANDO as alterações são salvas no Room
ENTÃO o app detecta mudanças (updatedAt > lastSyncTimestamp)
E envia apenas as entidades modificadas (changeset) via TCP
E o dispositivo parceiro recebe e faz merge
```

### Cenário 3: Sync bidirecional

```gherkin
DADO que duas pessoas cuidadoras fizeram alterações diferentes
QUANDO ambos estamos na mesma Wi-Fi
ENTÃO o Device A envia suas mudanças para Device B
E Device B envia suas mudanças para Device A
E ambos fazem merge por updatedAt (last-write-wins)
E ambos ficam com os mesmos dados
```

### Cenário 4: Indicador de sync

```gherkin
DADO que estou no app
QUANDO estou sincronizando com outro device
ENTÃO vejo indicador "[sincronizando]" na toolbar
QUANDO sync completa
ENTÃO indicador muda para "sincronizado"
QUANDO não detecto o outro device na rede
ENTÃO indicador muda para "[local]" (apenas local)
```

### Cenário 5: Sync ao reconectar na Wi-Fi

```gherkin
DADO que estava fora de casa (sem rede local)
E fiz alterações offline
QUANDO volto para casa e conecto na Wi-Fi
ENTÃO o app detecta o parceiro via NSD
E sincroniza automaticamente as mudanças acumuladas
```

### Cenário 6: Desabilitar sync automático

```gherkin
DADO que estou nas configurações de sync
QUANDO desabilito "Sync automático na rede local"
ENTÃO o app para de anunciar via NSD
E para de descobrir outros devices
E a sync só acontece manualmente via "Enviar Dados"
```

---

## UI/UX

### Indicador na Toolbar (PetitTopAppBar)

O SyncIndicator aparece no slot `actions` do PetitTopAppBar em **todas as telas**,
não apenas no Perfil. Usa Material Icons, sem emojis.

```
┌────────────────────────────────┐
│ Petit                    [sync] │  ← Icons.Default.CloudDone (synced)
├────────────────────────────────┤

┌────────────────────────────────┐
│ Petit                    [sync] │  ← Icons.Default.Sync (syncing, animated)
├────────────────────────────────┤

┌────────────────────────────────┐
│ Petit                    [sync] │  ← Icons.Default.PhoneAndroid (parceiro offline)
├────────────────────────────────┤
```

O indicador só aparece quando o usuário faz parte de um grupo familiar.
Sem grupo = sem indicador.

### Configuração de Sync

Acessada via Perfil > Grupo Familiar > "Gerenciar Grupo" > "Config. de Sync".
Usa Material Icons, sem emojis. Segue Material 3.

```
┌────────────────────────────────┐
│ ← Configurações de Sync        │
├────────────────────────────────┤
│                                │
│  SYNC NA REDE LOCAL            │
│  ┌────────────────────────────┐ │
│  │ Sync automático       [ON] │ │
│  └────────────────────────────┘ │
│                                │
│  Quando ambos dispositivos     │
│  estão na mesma rede Wi-Fi,    │
│  os dados sincronizam          │
│  automaticamente.              │
│                                │
├────────────────────────────────┤
│                                │
│  STATUS                        │
│  ┌────────────────────────────┐ │
│  │ Device B        │ │
│  │ Última sync: há 2 min      │ │
│  │                            │ │
│  │ 2 pets • 15 pesagens      │ │
│  │ 8 vacinas • 6 vermífugos   │ │
│  └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FORÇAR SYNC AGORA       │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### NSD Service Registration

```kotlin
class LanSyncService(
    private val context: Context,
    private val nsdManager: NsdManager
) {
    private val serviceInfo = NsdServiceInfo().apply {
        serviceName = "Petit-${getLocalDeviceId()}"
        serviceType = "_petit._tcp"
        setPort(0) // porta dinâmica
    }

    fun startAdvertising(port: Int) {
        serviceInfo.setPort(port)
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )
    }

    fun startDiscovery() {
        nsdManager.discoverServices(
            "_petit._tcp",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }
}
```

### LanSyncRepository

```kotlin
interface LanSyncRepository {
    val syncState: StateFlow<LanSyncState>

    suspend fun startService(): Result<Unit>
    suspend fun stopService()
    suspend fun syncWithPeer(peerAddress: InetAddress, port: Int): Result<SyncResult>
    fun getChangesSince(timestamp: Long): ExportBundle
}

sealed class LanSyncState {
    object Idle : LanSyncState()
    object Advertising : LanSyncState()
    object Discovering : LanSyncState()
    data class PeerFound(val deviceName: String, val address: InetAddress) : LanSyncState()
    object Syncing : LanSyncState()
    data class Synced(val lastSyncAt: Long) : LanSyncState()
    data class Error(val message: String) : LanSyncState()
}

data class SyncResult(
    val entitiesSent: Int,
    val entitiesReceived: Int,
    val conflictsResolved: Int,
    val syncTimestamp: Long
)
```

### TCP Sync Protocol

```
Fluxo de mensagens TCP:

1. Client → Server: HELLO {familyGroupKey} {deviceId} {lastSyncTimestamp}
2. Server → Client: HELLO_ACK {deviceId} {lastSyncTimestamp}
3. Server → Client: CHANGESET {json_bundle_com_mudanças_desde_lastSync}
4. Client → Server: CHANGESET {json_bundle_com_mudanças_desde_lastSync}
5. Client → Server: ACK {newSyncTimestamp}
6. Server → Client: ACK {newSyncTimestamp}
7. Ambos: CLOSE
```

### WorkManager Integration

```kotlin
// Verificação periódica de sync (quando app em background)
class LanSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val lanSyncRepository: LanSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            lanSyncRepository.startService()
            // NSD discovery roda por alguns segundos
            // Se encontrar peer, sincroniza
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## Bateria e Performance

### Regra fundamental

> **Sync contínuo usa EXCLUSIVAMENTE Wi-Fi de infraestrutura (roteador da casa), NUNCA Wi-Fi Direct.**

Wi-Fi Direct cria um grupo P2P onde um device age como soft AP — o rádio nunca dorme, consumindo ~150-300mW extras. Em aparelhos fracos (3000mAh), isso drena 10-15% por hora.

NSD + TCP sobre Wi-Fi normal usam o rádio que já está ligado para internet. Custo incremental: ~5-15mW.

### Estratégias de economia

| Contexto | Estratégia |
|---|---|
| App em foreground | NSD ativo, TCP on-demand |
| App em background | WorkManager periódico (15min mínimo) |
| App fechado | NSD desregistrado, sem consumo |
| Sem parceiro na rede | Discovery para após timeout, retry via WorkManager |

### Batching de changesets

- Não sincronizar a cada escrita individual no Room
- Acumular mudanças e sincronizar em batch (a cada sync cycle)
- WorkManager flex interval de 5 minutos permite ao Android otimizar o momento

---

## Segurança

- **Family Group Key validation**: Toda conexão TCP valida a family group key no handshake
- **Rede local only**: NSD funciona apenas na mesma rede Wi-Fi (não roteável pela internet)
- **Dados em trânsito**: Considerar TLS sobre TCP para proteção extra
- **Autenticação**: Apenas devices com a mesma family group key podem sincronizar

---

## Critérios de Aceite

- [ ] NSD registra serviço "_petit._tcp" quando app está aberto
- [ ] NSD descobre outros Petit na mesma rede
- [ ] Sync incremental envia apenas mudanças (changeset)
- [ ] Merge bidirecional por updatedAt (last-write-wins)
- [ ] Indicador de sync visível na toolbar
- [ ] Sync automática acontece quando ambos na mesma Wi-Fi
- [ ] WorkManager tenta sync periódica em background
- [ ] Family group key é validada em toda conexão
- [ ] É possível desabilitar sync automático
- [ ] Funciona ao reconectar na Wi-Fi após período offline
