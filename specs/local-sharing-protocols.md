# Protocolos de Compartilhamento Local entre Dispositivos Android

> **Pesquisa técnica migrada em 2026-07-17.** Origem:
> `getmiw/specs-miw@09b4497`. As decisões foram generalizadas para o Petit e
> para dispositivos Android neutros. Exemplos de código são ilustrativos; a
> implementação atual usa Nearby Connections, enquanto NSD + TCP permanece
> planejado em [`phase-2/us-104-lan-sync.md`](./phase-2/us-104-lan-sync.md).

## Introdução

Este documento descreve os protocolos e tecnologias disponíveis no Android para compartilhamento de dados entre dispositivos **sem depender de internet ou servidores remotos**. O foco é o cenário de dois dispositivos na mesma casa (Device A + Device B) que precisam sincronizar dados de um app.

---

## Visão Geral das Tecnologias

| Tecnologia                     | Alcance    | Throughput          | Requer Internet | Requer Pareamento | Complexidade | API Level        |
| ------------------------------ | ---------- | ------------------- | --------------- | ----------------- | ------------ | ---------------- |
| **Nearby Connections**         | ~100m      | Alto (Wi-Fi Direct) | Não              | Não (código)       | Baixa        | 16+              |
| **Wi-Fi Direct (P2P)**         | ~200m      | Muito alto          | Não              | Sim (WPS)         | Média        | 14+              |
| **Wi-Fi Aware (NAN)**          | ~50m       | Alto                | Não              | Não                | Alta         | 26+ (Android 8)  |
| **NSD (mDNS/DNS-SD)**          | Rede local | Alto (TCP)          | Não\*            | Não                | Média        | 16+              |
| **Bluetooth Classic**          | ~10m       | Médio (~3 Mbps)     | Não              | Sim               | Média        | 5+               |
| **BLE (Bluetooth Low Energy)** | ~50m       | Baixo (~1 Mbps)     | Não              | Não                | Alta         | 18+              |
| **NFC**                        | ~4cm       | Muito baixo         | Não              | Não                | Baixa        | 14+              |
| **UWB (Ultra-Wideband)**       | ~200m      | Baixo               | Não              | Sim               | Alta         | 31+ (Android 12) |

> \* NSD requer que ambos dispositivos estejam na mesma rede Wi-Fi local, mas não requer acesso à internet.

---

## 1. Nearby Connections API

### O que é

API do Google Play Services que abstrai Bluetooth, BLE e Wi-Fi Direct em uma interface unificada. O desenvolvedor não precisa se preocupar com qual tecnologia de transporte está sendo usada — o sistema escolhe automaticamente a melhor opção.

### Como funciona

```
┌──────────────┐                      ┌──────────────┐
│  Advertiser  │                      │  Discoverer  │
│  (Device A)  │                      │  (Device B)  │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       │  1. startAdvertising()              │
       │◀────────────────────────────────────│  2. startDiscovery()
       │                                     │
       │  3. onEndpointFound()               │
       │────────────────────────────────────▶│
       │                                     │
       │  4. requestConnection()             │
       │◀────────────────────────────────────│
       │                                     │
       │  5. acceptConnection() ←───────────▶  5. acceptConnection()
       │                                     │
       │  6. onConnectionResult(SUCCESS)     │
       │◀───────────────────────────────────▶│
       │                                     │
       │  7. sendPayload() ◀───────────────▶  7. sendPayload()
       │                                     │
```

### Estratégias (topologias)

| Estratégia             | Topologia | Conexões       | Throughput | Uso ideal                              |
| ---------------------- | --------- | -------------- | ---------- | -------------------------------------- |
| **P2P_POINT_TO_POINT** | 1:1       | Máximo 1       | Máximo     | Transferência de dados entre 2 devices |
| **P2P_STAR**           | 1:N       | Hub ↔ N spokes | Alto       | Um device central + vários periféricos |
| **P2P_CLUSTER**        | M:N       | Mesh           | Médio      | Gaming multiplayer, mesh networks      |

**Decisão original da pesquisa**: `P2P_POINT_TO_POINT` — máximo throughput
para transferir dados entre dois dispositivos. A implementação atual do Petit
usa `P2P_STAR`; qualquer alteração de estratégia deve ser validada em dois
dispositivos antes de atualizar a implementação ou esta decisão.

### Tipos de Payload

| Tipo       | Tamanho   | Uso                                         |
| ---------- | --------- | ------------------------------------------- |
| **BYTES**  | Até 32 KB | Metadados, mensagens de controle, handshake |
| **FILE**   | Ilimitado | Arquivos grandes (fotos, backups)           |
| **STREAM** | Ilimitado | Dados gerados em tempo real (áudio, vídeo)  |

**Recomendação para Petit**: `BYTES` para dados JSON pequenos (< 32 KB). Para dados maiores, `FILE` com ExportBundle serializado.

### Transporte subjacente

O Nearby Connections usa automaticamente:

1. **Bluetooth** para descoberta inicial
2. **Wi-Fi Direct** para transferência de dados (quando disponível)
3. **BLE** como fallback para descoberta

O desenvolvedor **não controla** qual transporte é usado. A API otimiza automaticamente.

### Segurança

- **Criptografia automática**: toda comunicação é criptografada end-to-end
- **Autenticação**: ambos os lados podem verificar um token de autenticação (o código de 4 dígitos)
- **Proximidade**: funciona apenas com devices fisicamente próximos

### Código Kotlin (exemplo simplificado)

```kotlin
// Advertiser (Device A — quem cria o código)
val advertisingOptions = AdvertisingOptions.Builder()
    .setStrategy(Strategy.P2P_POINT_TO_POINT)
    .build()

connectionsClient.startAdvertising(
    localDeviceName,      // nome visível
    SERVICE_ID,           // "com.woliveiras.petit.familygroup"
    connectionLifecycleCallback,
    advertisingOptions
)

// Discoverer (Device B — quem insere o código)
val discoveryOptions = DiscoveryOptions.Builder()
    .setStrategy(Strategy.P2P_POINT_TO_POINT)
    .build()

connectionsClient.startDiscovery(
    SERVICE_ID,
    endpointDiscoveryCallback,
    discoveryOptions
)

// Enviar dados após conexão
val jsonBytes = exportBundle.toJson().toByteArray()
val payload = Payload.fromBytes(jsonBytes)
connectionsClient.sendPayload(endpointId, payload)
```

### Permissões necessárias

```xml
<!-- Android 12 (API 31) e superior -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- Android 13 (API 33) e superior -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />

<!-- Android 12 e inferior -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
                 android:maxSdkVersion="32" />

<!-- Sempre necessário -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

### Prós e Contras

| Sim Prós                                 | Não Contras                               |
| --------------------------------------- | ---------------------------------------- |
| API simples e bem documentada           | Requer Google Play Services              |
| Abstrai Bluetooth/Wi-Fi automaticamente | Analytics coletados pelo Google          |
| Criptografia built-in                   | Não funciona em devices sem GMS (Huawei) |
| Alto throughput com P2P_POINT_TO_POINT  | Controle limitado sobre transporte       |
| Funciona 100% offline                   |                                          |

---

## 2. Wi-Fi Direct (P2P)

### O que é

Protocolo Wi-Fi que permite dois dispositivos se conectarem diretamente, **sem access point intermediário**. Um device atua como "Group Owner" (mini access point) e o outro se conecta a ele.

### Como funciona

```
┌──────────────┐                      ┌──────────────┐
│   Device A   │  Wi-Fi Direct P2P   │   Device B   │
│ (Group Owner)│◀────────────────────▶│   (Client)   │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       │  1. discoverPeers()                 │
       │────────────────────────────────────▶│
       │                                     │
       │  2. WIFI_P2P_PEERS_CHANGED          │
       │◀────────────────────────────────────│
       │                                     │
       │  3. connect(WifiP2pConfig)          │
       │────────────────────────────────────▶│
       │                                     │
       │  4. WIFI_P2P_CONNECTION_CHANGED     │
       │◀───────────────────────────────────▶│
       │                                     │
       │  5. ServerSocket ←──── Socket       │
       │     (transferência via TCP)         │
       │                                     │
```

### Diferença vs Nearby Connections

| Aspecto     | Nearby Connections   | Wi-Fi Direct        |
| ----------- | -------------------- | ------------------- |
| Nível       | Alto (API Google)    | Baixo (Android SDK) |
| Controle    | Pouco (automático)   | Total (manual)      |
| Transporte  | Auto (BT + Wi-Fi)    | Apenas Wi-Fi        |
| Setup       | Simples              | Complexo            |
| Dependência | Google Play Services | Nenhuma             |

### Quando usar Wi-Fi Direct diretamente

- Quando precisa de controle total sobre o transporte
- Quando o app precisa funcionar sem Google Play Services
- Quando precisa de conexão persistente de longa duração

### Código Kotlin

```kotlin
// Inicializar
val manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
val channel = manager.initialize(this, mainLooper, null)

// Descobrir peers
manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
    override fun onSuccess() { /* discovery iniciada */ }
    override fun onFailure(reason: Int) { /* falhou */ }
})

// Conectar
val config = WifiP2pConfig().apply {
    deviceAddress = selectedDevice.deviceAddress
}
manager.connect(channel, config, actionListener)

// Transferir dados (após conexão)
// Group Owner: ServerSocket(8888).accept()
// Client: Socket(groupOwnerAddress, 8888)
```

### Prós e Contras

| Sim Prós                      | Não Contras                            |
| ---------------------------- | ------------------------------------- |
| Altíssimo throughput         | API mais complexa                     |
| Sem dependência de GMS       | Gerenciamento manual de sockets       |
| Alcance de ~200m             | Apenas Wi-Fi (sem fallback BT)        |
| Conexão persistente possível | Precisa gerenciar broadcast receivers |
| Suporta WPA2                 | Requer permission handling complexo   |

---

## 3. Network Service Discovery (NSD / mDNS / DNS-SD)

### O que é

Mecanismo de descoberta de serviços na rede local baseado em **DNS-SD (DNS Service Discovery)** e **mDNS (Multicast DNS)**. É o mesmo protocolo usado pelo **Bonjour** da Apple. Permite que um app anuncie um serviço na rede local e outros apps descubram e se conectem a ele.

### Como funciona

```
┌──────────────┐    mDNS broadcast    ┌──────────────┐
│   Device A   │   "_petit._tcp"       │   Device B   │
│  (Servidor)  │────────────────────▶│   (Cliente)  │
└──────┬───────┘    na rede Wi-Fi    └──────┬───────┘
       │                                     │
       │  1. registerService()               │
       │  (anuncia "_petit._tcp"               │
       │   com IP e porta)                   │
       │                                     │
       │                  2. discoverServices()
       │◀────────────────────────────────────│
       │                                     │
       │  3. resolveService()                │
       │  (obtém IP + porta)                │
       │────────────────────────────────────▶│
       │                                     │
       │  4. TCP Socket connection           │
       │◀───────────────────────────────────▶│
       │                                     │
       │  5. Troca de dados via TCP          │
       │◀───────────────────────────────────▶│
       │                                     │
```

### Diferença vs Nearby Connections

| Aspecto     | NSD                    | Nearby Connections    |
| ----------- | ---------------------- | --------------------- |
| Requisito   | Mesma rede Wi-Fi       | Proximidade física    |
| Sempre on   | Sim (enquanto na rede) | Não (precisa iniciar) |
| Background  | Sim (com WorkManager)  | Limitado              |
| Uso ideal   | Sync contínuo em casa  | Pareamento e one-shot |
| Dependência | Android SDK (sem GMS)  | Google Play Services  |

### Service Type Format

```
_<protocolo>._<transporte>
_petit._tcp           ← serviço Petit sobre TCP
_http._tcp           ← servidor HTTP
_ipp._tcp            ← impressora
```

### Código Kotlin

```kotlin
// Registrar serviço
val serviceInfo = NsdServiceInfo().apply {
    serviceName = "Petit-Device-A"
    serviceType = "_petit._tcp"
    setPort(serverSocket.localPort)
}

val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

// Descobrir serviços
nsdManager.discoverServices("_petit._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

// Resolver serviço (obter IP + porta)
nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
    override fun onServiceResolved(service: NsdServiceInfo) {
        val host: InetAddress = service.host
        val port: Int = service.port
        // Conectar via TCP socket
    }
    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { }
})
```

### Prós e Contras

| Sim Prós                                  | Não Contras                          |
| ---------------------------------------- | ----------------------------------- |
| Padrão da indústria (Bonjour-compatible) | Requer mesma rede Wi-Fi             |
| Sem dependência de GMS                   | Discovery pode ser lento (~5-15s)   |
| Funciona em background                   | Consome bateria se sempre ativo     |
| Perfeito para sync contínuo em casa      | Não funciona entre redes diferentes |
| Baixo overhead                           | Precisa gerenciar TCP manualmente   |

---

## 4. Bluetooth Classic

### O que é

Protocolo de comunicação sem fio de curto alcance (~10m). Usa RFCOMM (serial port emulation) para transferência de dados stream-based entre dispositivos pareados.

### Throughput

- **Bluetooth 2.0+EDR**: ~3 Mbps
- **Bluetooth 3.0+HS**: ~24 Mbps (via Wi-Fi)
- **Bluetooth 4.0+**: ~3 Mbps (Classic) ou ~1 Mbps (BLE)
- **Bluetooth 5.0+**: ~2 Mbps (BLE) com maior alcance

### Quando usar

- Para transferências menores (< 1 MB)
- Quando Wi-Fi não está disponível
- Quando precisa de conexão persistente e confiável

### Prós e Contras

| Sim Prós                       | Não Contras                 |
| ----------------------------- | -------------------------- |
| Universalmente disponível     | Baixo throughput vs Wi-Fi  |
| Baixo consumo de energia      | Alcance limitado (~10m)    |
| Conexão estável               | Requer pareamento manual   |
| Sem dependência de rede Wi-Fi | API mais antiga e complexa |

---

## 5. Bluetooth Low Energy (BLE)

### O que é

Versão de baixo consumo do Bluetooth, otimizada para troca de pequenas quantidades de dados. Usa o modelo GATT (Generic Attribute Profile) com características e serviços.

### Quando usar

- Para troca de metadados pequenos (< 512 bytes por characterística)
- Para presença/discovery contínuo com baixo consumo
- Para wearables e sensores

### Para Petit: NÃO recomendado como transporte principal

BLE não é adequado para transferir ExportBundles de dados (que podem ter centenas de KB ou mais). É útil apenas como mecanismo de discovery complementar.

---

## 6. Wi-Fi Aware (NAN — Neighbor Awareness Networking)

### O que é

Protocolo Wi-Fi para descoberta e comunicação direta entre dispositivos **sem access point**, disponível a partir do Android 8.0 (API 26). Diferente do Wi-Fi Direct, não requer formação de grupo — devices comunicam diretamente.

### Quando usar

- Para discovery e comunicação sem rede Wi-Fi
- Quando precisa de discovery contínuo com baixo consumo
- Para cenários IoT

### Para Petit: alternativa futura

Wi-Fi Aware é mais moderno que Wi-Fi Direct, mas tem menor suporte de dispositivos. Para o cenário de casa (mesma rede Wi-Fi), NSD é mais simples e confiável.

---

## 7. NFC (Near Field Communication)

### O que é

Comunicação de curtíssimo alcance (~4 cm). Usado para troca rápida de informações pequenas por aproximação.

### Para Petit: útil apenas para bootstrapping

NFC poderia ser usado para iniciar o pareamento (encostar os phones), mas não para transferir dados significativos.

---

## Comparativo para o caso de uso Petit

### Cenário: Família com Device A + Device B, mesma casa, mesma Wi-Fi

| Necessidade                        | Melhor tecnologia  | Justificativa                                            |
| ---------------------------------- | ------------------ | -------------------------------------------------------- |
| **Pareamento inicial**             | Nearby Connections | Simples, criptografado, funciona sem Wi-Fi               |
| **Transferência única (one-shot)** | Nearby Connections | Alto throughput, API simples                             |
| **Sync contínuo em casa**          | NSD + TCP          | Funciona em background, auto-discovery, sem GMS overhead |
| **Sync sem Wi-Fi (emergência)**    | Nearby Connections | Usa Bluetooth/Wi-Fi Direct automaticamente               |

### Arquitetura híbrida recomendada

```
┌─────────────────────────────────────────────────────┐
│                  CAMADA DE APLICAÇÃO                  │
│                                                      │
│   FamilyGroupRepository ──── SyncEngine                │
│          │                      │                    │
│          ▼                      ▼                    │
│  ┌───────────────┐    ┌──────────────────┐           │
│  │    Nearby      │    │   NSD + TCP       │          │
│  │  Connections   │    │   (LAN Sync)      │          │
│  │               │    │                  │           │
│  │  • Pareamento  │    │  • Discovery     │           │
│  │  • One-shot    │    │  • Auto-sync     │           │
│  │  • Fallback    │    │  • Background    │           │
│  └───────┬───────┘    └────────┬─────────┘           │
│          │                      │                    │
└──────────┼──────────────────────┼────────────────────┘
           │                      │
    ┌──────▼──────┐       ┌──────▼──────┐
    │  Bluetooth  │       │   TCP over  │
    │  Wi-Fi Dir  │       │   Wi-Fi LAN │
    │  (GMS auto) │       │   (Sockets) │
    └─────────────┘       └─────────────┘
```

---

## Protocolo de Sync para Petit

### Handshake (TCP sobre NSD)

```
┌─────────────────────────────────────────────────────────┐
│                    PROTOCOLO Petit SYNC v1                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. HELLO                                                │
│     Client → Server                                      │
│     {                                                    │
│       "type": "HELLO",                                   │
│       "familyGroupKey": "uuid-do-grupo-familiar",               │
│       "deviceId": "uuid-do-device",                      │
│       "deviceName": "Device A",            │
│       "lastSyncTimestamp": 1712345678000,                │
│       "protocolVersion": 1                               │
│     }                                                    │
│                                                          │
│  2. HELLO_ACK (se familyGroupKey válida)                   │
│     Server → Client                                      │
│     {                                                    │
│       "type": "HELLO_ACK",                               │
│       "deviceId": "uuid-do-server",                      │
│       "deviceName": "Device B",               │
│       "lastSyncTimestamp": 1712345600000                  │
│     }                                                    │
│                                                          │
│  2b. ERROR (se familyGroupKey inválida)                    │
│     Server → Client                                      │
│     {                                                    │
│       "type": "ERROR",                                   │
│       "code": "INVALID_FAMILY_GROUP_KEY"                    │
│     }                                                    │
│     → Server desconecta                                  │
│                                                          │
│  3. CHANGESET (Server → Client)                          │
│     {                                                    │
│       "type": "CHANGESET",                               │
│       "since": 1712345678000,                            │
│       "pets": [...entidades modificadas...],             │
│       "weightEntries": [...],                            │
│       "vaccinationEntries": [...],                       │
│       "dewormingEntries": [...],                         │
│       "tasks": [...]                                     │
│     }                                                    │
│                                                          │
│  4. CHANGESET (Client → Server)                          │
│     (mesma estrutura, com mudanças do client)            │
│                                                          │
│  5. ACK (ambos)                                          │
│     {                                                    │
│       "type": "ACK",                                     │
│       "syncTimestamp": 1712346000000                      │
│     }                                                    │
│                                                          │
│  6. CLOSE (ambos fecham a conexão TCP)                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Formato do Changeset

O changeset usa o mesmo formato do ExportBundle (JSON), mas contém apenas entidades com `updatedAt > lastSyncTimestamp`:

```json
{
  "type": "CHANGESET",
  "since": 1712345678000,
  "pets": [
    {
      "id": "uuid-pet-a",
      "name": "Pet A",
      "updatedAt": 1712345900000,
      "deletedAt": null,
      "syncStatus": "SYNCED"
    }
  ],
  "weightEntries": [
    {
      "id": "uuid-peso-1",
      "petId": "uuid-pet-a",
      "date": "2026-04-12",
      "weightGrams": 3520,
      "updatedAt": 1712345800000,
      "deletedAt": null
    }
  ]
}
```

### Regras de Merge (Last-Write-Wins)

```
Para cada entidade no changeset remoto:

  SE entidade.id NÃO existe localmente:
    → INSERT (nova entidade)

  SE entidade.id existe localmente:
    SE remote.deletedAt != null:
      SE local.updatedAt > remote.deletedAt:
        → KEEP_LOCAL (edição mais recente que delete)
      SENÃO:
        → APPLY_DELETE (propagar soft delete)

    SE remote.updatedAt > local.updatedAt:
      → UPDATE (dados remotos são mais recentes)

    SENÃO:
      → KEEP_LOCAL (dados locais são mais recentes)
```

---

## Referências

- [Nearby Connections Overview](https://developers.google.com/nearby/connections/overview)
- [Nearby Connections Strategies](https://developers.google.com/nearby/connections/strategies)
- [Wi-Fi Direct (P2P)](https://developer.android.com/develop/connectivity/wifi/wifip2p)
- [Create P2P connections with Wi-Fi Direct](https://developer.android.com/develop/connectivity/wifi/wifi-direct)
- [Network Service Discovery (NSD)](https://developer.android.com/develop/connectivity/wifi/use-nsd)
- [Bluetooth data transfer](https://developer.android.com/develop/connectivity/bluetooth/transfer-data)
- [Wi-Fi Aware](https://developer.android.com/develop/connectivity/wifi/wifi-aware)
- [Android Connectivity overview](https://developer.android.com/develop/connectivity)
