# US-101: Pareamento de Dispositivos

> **Estado no Petit (2026-07-17): parcialmente implementado.** Nearby,
> permissões, tela, repositórios e persistência existem. A UI atual descobre
> dispositivos sem permitir que o receptor digite o código descrito abaixo;
> por isso, os critérios permanecem pendentes até alinhamento e teste em dois
> dispositivos. Origem: `getmiw/specs-miw@09b4497`.

**Prioridade**: P0
**Épico**: Compartilhamento Familiar
**Fase**: 2

---

## História

> Como usuário do app,
> Eu quero parear meu dispositivo com o dispositivo de outra pessoa cuidadora,
> Para que possamos compartilhar os dados dos nossos pets entre nossos dispositivos.

---

## Cenários de Aceite

### Cenário 1: Iniciar pareamento (transmissor)

```gherkin
DADO que tenho o app aberto
QUANDO acesso Perfil > "Grupo Familiar"
E toco em "Parear Dispositivo"
ENTÃO vejo um código de 4 dígitos
E o app inicia advertising via Nearby Connections
E vejo "Aguardando conexão do outro dispositivo..."
```

### Cenário 2: Conectar ao transmissor (receptor)

```gherkin
DADO que tenho o app aberto em outro dispositivo
QUANDO acesso Perfil > "Grupo Familiar"
E toco em "Entrar em Grupo Familiar"
E insiro o código de 4 dígitos do transmissor
ENTÃO o app inicia discovery via Nearby Connections
E a conexão é estabelecida
E ambos devices trocam informações do grupo familiar
E vejo "Pareamento concluído! Vocês agora compartilham dados."
```

### Cenário 3: Pareamento sem internet

```gherkin
DADO que ambos dispositivos estão sem internet
MAS têm Bluetooth ou Wi-Fi habilitado
QUANDO inicio o processo de pareamento
ENTÃO o pareamento funciona normalmente
(Nearby Connections usa Bluetooth/Wi-Fi Direct automaticamente)
```

### Cenário 4: Cancelar pareamento

```gherkin
DADO que estou aguardando conexão
QUANDO toco em "Cancelar"
ENTÃO o advertising/discovery é interrompido
E volto à tela anterior
```

### Cenário 5: Pareamento com código inválido

```gherkin
DADO que o receptor insere um código errado
QUANDO tenta conectar
ENTÃO recebe "Código inválido. Verifique o código no outro dispositivo."
E pode tentar novamente
```

### Cenário 6: Desfazer pareamento

```gherkin
DADO que estou pareado com outro dispositivo
QUANDO acesso "Gerenciar Família"
E toco em "Remover dispositivo"
E confirmo a ação
ENTÃO o dispositivo é removido do grupo familiar local
E a chave do grupo familiar é invalidada neste device
E o outro device mantém seus dados mas perde a referência de sync
```

---

## UI/UX

### Tela: Seção Grupo Familiar no Perfil (sem pareamento)

A seção Grupo Familiar aparece como primeira seção na tela de Perfil.
Usa Material Icons, sem emojis. Segue Material 3.

```
┌────────────────────────────────┐
│  Perfil                         │
├────────────────────────────────┤
│                                │
│  GRUPO FAMILIAR                │
│  ┌────────────────────────────┐│
│  │                            ││
│  │  Compartilhe os dados dos  ││
│  │  seus pets com sua        ││
│  │  família.                  ││
│  │                            ││
│  │  Funciona sem internet!    ││
│  │                            ││
│  │ ┌──────────────────────┐   ││
│  │ │  PAREAR DISPOSITIVO  │   ││
│  │ └──────────────────────┘   ││
│  │ ┌──────────────────────┐   ││
│  │ │  ENTRAR EM GRUPO     │   ││
│  │ └──────────────────────┘   ││
│  │                            ││
│  └────────────────────────────┘│
│                                │
│  CONFIGURAÇÕES                 │
│  ...                           │
└────────────────────────────────┘
```

### Tela: Aguardando Pareamento (transmissor)

```
┌────────────────────────────────┐
│ ← Aguardando conexão...        │
├────────────────────────────────┤
│                                │
│         [conexão]              │
│                                │
│    Código de pareamento:       │
│                                │
│        ┌──────────┐            │
│        │   4729   │            │
│        └──────────┘            │
│                                │
│  Peça para o outro dispositivo │
│  inserir este código.          │
│                                │
│ ┌────────────────────────────┐ │
│ │       CANCELAR             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### Nearby Connections Config

```kotlin
// Strategy para pareamento 1:1
val strategy = Strategy.P2P_STAR

// Service ID único do app
const val SERVICE_ID = "com.woliveiras.petit.familygroup"

// Advertising options
val advertisingOptions = AdvertisingOptions.Builder()
    .setStrategy(strategy)
    .build()

// Discovery options
val discoveryOptions = DiscoveryOptions.Builder()
    .setStrategy(strategy)
    .build()
```

### FamilyGroupRepository

```kotlin
interface FamilyGroupRepository {
    val familyGroupInfo: Flow<FamilyGroupInfo?>
    suspend fun createFamilyGroup(deviceName: String): String
    suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String)
    suspend fun addRemoteMember(member: FamilyGroupMember)
    suspend fun removeMember(memberId: String)
    suspend fun leaveFamilyGroup()
    suspend fun getFamilyGroupKey(): String?
}
```

### NearbyTransferRepository

```kotlin
interface NearbyTransferRepository {
    val pairingState: Flow<PairingState>
    suspend fun startAdvertising(deviceName: String, familyGroupKey: String)
    suspend fun startDiscovery(familyGroupKey: String)
    fun stopAdvertising()
    fun stopDiscovery()
    fun disconnect()
}

sealed interface PairingState {
    data object Idle : PairingState
    data class WaitingForConnection(val code: String) : PairingState
    data class ConnectionRequested(val deviceName: String, val endpointId: String) : PairingState
    data class Paired(val familyGroupKey: String, val deviceName: String) : PairingState
    data class Error(val message: String) : PairingState
}
```

---

## Critérios de Aceite

- [ ] Transmissor gera código de 4 dígitos
- [ ] Receptor insere código e conecta com sucesso
- [ ] Pareamento funciona sem internet (Nearby Connections)
- [ ] Ambos devices recebem a mesma family group key
- [ ] Family group key é persistida no DataStore
- [ ] UI mostra estado do pareamento
- [ ] É possível desfazer o pareamento
- [ ] Permissões de Bluetooth/Wi-Fi são solicitadas antes
