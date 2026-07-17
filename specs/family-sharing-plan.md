# Plano de Implementação — Compartilhamento Familiar (Fase 2)

> **Documento migrado em 2026-07-17.** Origem:
> `getmiw/specs-miw@09b4497`. Este arquivo é uma cópia histórica integral e
> duplicada de [`household-sharing-plan.md`](./household-sharing-plan.md).
> Ambos foram preservados para manter todo o material original. Use
> [`development-plan-phase2.md`](./development-plan-phase2.md) e
> [`phase-2/README.md`](./phase-2/README.md) como referências atuais.

## Contexto

O Petit precisa de compartilhamento de dados entre dois dispositivos na mesma casa (Device A + Device B) sem depender de internet ou servidores remotos.

## Estratégia

A implementação é dividida em **3 etapas incrementais**, cada uma entregando valor funcional:

---

## Etapa 1: Pareamento + Transferência One-Shot

**Objetivo**: Dois devices podem se parear e transferir dados manualmente.

### Escopo

- Tela "Compartilhar com Família" em Configurações
- Pareamento via Nearby Connections (código 4 dígitos)
- Envio/recebimento de dados (ExportBundle JSON)
- Opções "Mesclar" e "Substituir" no receptor
- Persistência da family group key no DataStore

### Pacotes e classes a criar

```
com.woliveiras.petit/
├── data/
│   ├── local/
│   │   ├── entity/FamilyGroupMemberEntity.kt
│   │   ├── entity/SyncLogEntity.kt
│   │   └── dao/FamilyGroupMemberDao.kt
│   ├── mapper/FamilyGroupMapper.kt
│   └── repository/
│       ├── FamilyGroupRepository.kt
│       ├── FamilyGroupRepositoryImpl.kt
│       ├── NearbyTransferRepository.kt
│       └── NearbyTransferRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── FamilyGroupMember.kt
│   │   ├── FamilyGroupInfo.kt
│   │   ├── TransferState.kt
│   │   ├── PairingState.kt
│   │   ├── MergeResult.kt
│   │   └── SyncLog.kt
│   └── usecase/
│       ├── CreateFamilyGroupUseCase.kt
│       ├── JoinFamilyGroupUseCase.kt
│       ├── SendDataUseCase.kt
│       ├── ReceiveDataUseCase.kt
│       └── MergeDataUseCase.kt
├── presentation/
│   └── feature/
│       └── familygroup/
│           ├── FamilyGroupScreen.kt
│           ├── FamilyGroupViewModel.kt
│           ├── FamilyGroupUiState.kt
│           ├── PairingScreen.kt
│           ├── PairingViewModel.kt
│           ├── TransferScreen.kt
│           └── TransferViewModel.kt
└── di/
    └── FamilyGroupModule.kt
```

### Dependências a adicionar

```toml
# gradle/libs.versions.toml
[versions]
playServicesNearby = "19.3.0"

[libraries]
play-services-nearby = { group = "com.google.android.gms", name = "play-services-nearby", version.ref = "playServicesNearby" }
```

### Permissões a adicionar no AndroidManifest

```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
                 android:maxSdkVersion="32" />
```

### Room Migration

- Criar migration para adicionar tabelas `family_group_members` e `sync_logs`
- Incrementar versão do database

### Navegação

- Adicionar rota `familygroup` na Navigation Compose
- Adicionar item de menu em Configurações: "Compartilhar com Família"

### Ordem de implementação

1. Modelos de domínio (`FamilyGroupMember`, `PairingState`, `TransferState`)
2. Room entities + DAO + migration
3. `FamilyGroupRepository` + `NearbyTransferRepository`
4. Use cases
5. ViewModels
6. Telas Compose (FamilyGroup, Pairing, Transfer)
7. Hilt module
8. Navegação
9. Permissões runtime

### Resultado entregável

Usuário pode parear dois devices e enviar/receber dados manualmente. Funciona sem internet.

---

## Etapa 2: Grupo Familiar + Conflict Resolution

**Objetivo**: Gerenciar membros do grupo familiar e resolver conflitos automaticamente.

### Escopo

- Tela de gerenciamento do grupo familiar
- Renomear dispositivo
- Remover membro
- Sair do grupo
- ConflictResolver (last-write-wins por updatedAt)
- Log de sync

### Classes a criar

```
├── domain/
│   └── usecase/
│       ├── LeaveFamilyGroupUseCase.kt
│       └── RemoveMemberUseCase.kt
├── presentation/
│   └── feature/
│       └── familygroup/
│           ├── FamilyGroupScreen.kt
│           ├── FamilyGroupViewModel.kt
│           └── SyncLogScreen.kt
└── data/
    └── repository/
        └── ConflictResolver.kt
```

### Ordem de implementação

1. ConflictResolver (regras de merge)
2. SyncLog entity + writing
3. Use cases de leave/remove
4. Telas de gerenciamento
5. Testes unitários do ConflictResolver

### Resultado entregável

Membros do grupo são visíveis. Conflitos são resolvidos automaticamente. Histórico de sync é consultável.

---

## Etapa 3: Sync Contínuo na Rede Local (NSD + TCP)

**Objetivo**: Sync automático em background quando ambos devices estão na mesma Wi-Fi.

### Escopo

- NSD service registration ("\_petit.\_tcp")
- NSD discovery
- TCP server/client para troca de changesets
- Protocolo de handshake com validação de family group key
- WorkManager para sync em background
- Indicador de sync na toolbar
- Configuração on/off do sync automático

### Classes a criar

```
├── data/
│   └── repository/
│       ├── LanSyncRepository.kt
│       ├── LanSyncRepositoryImpl.kt
│       ├── NsdServiceManager.kt
│       └── TcpSyncServer.kt
├── worker/
│   └── LanSyncWorker.kt
├── presentation/
│   ├── components/
│   │   └── SyncIndicator.kt
│   └── feature/
│       └── familygroup/
│           └── SyncSettingsScreen.kt
└── di/
    └── LanSyncModule.kt
```

### Protocolo TCP

```
1. Client → Server: HELLO {familyGroupKey} {deviceId} {lastSyncTimestamp}
2. Server valida familyGroupKey → se inválida, retorna ERROR e desconecta
3. Server → Client: HELLO_ACK {deviceId} {lastSyncTimestamp}
4. Server → Client: CHANGESET {json das entidades com updatedAt > client.lastSyncTimestamp}
5. Client → Server: CHANGESET {json das entidades com updatedAt > server.lastSyncTimestamp}
6. Ambos: ACK {newSyncTimestamp}
7. Ambos: CLOSE
```

### WorkManager Config

```kotlin
// Sync periódica em background (a cada 15 minutos, mínimo do WorkManager)
val syncRequest = PeriodicWorkRequestBuilder<LanSyncWorker>(
    15, TimeUnit.MINUTES,
    5, TimeUnit.MINUTES  // flex interval
)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
```

### Lifecycle-aware NSD

- Registrar NSD service quando app está em foreground
- Manter WorkManager para tentativas em background
- Desregistrar NSD ao sair do app (para não consumir bateria)

### Ordem de implementação

1. NsdServiceManager (register + discover)
2. TcpSyncServer (accept + handshake + changeset exchange)
3. LanSyncRepository (orquestrar NSD + TCP)
4. LanSyncWorker (background sync)
5. SyncIndicator (Compose component)
6. SyncSettingsScreen
7. Integração com lifecycle do app
8. Testes de integração

### Resultado entregável

Sync automática e contínua na rede local. Ambos devices sempre sincronizados quando na mesma Wi-Fi.

---

## Cronograma Sugerido

| Etapa | Escopo                     | Dependência     |
| ----- | -------------------------- | --------------- |
| 1     | Pareamento + One-Shot      | Fase 1 completa |
| 2     | Grupo Familiar + Conflitos | Etapa 1         |
| 3     | Sync LAN Contínuo          | Etapa 2         |

---

## Riscos e Mitigações

| Risco                                          | Impacto                         | Mitigação                                                    |
| ---------------------------------------------- | ------------------------------- | ------------------------------------------------------------ |
| Nearby Connections requer Google Play Services | Não funciona em devices sem GMS | Ambos os dispositivos têm GMS. NSD+TCP funciona sem GMS.             |
| NSD discovery pode ser lento em algumas redes  | Delay na detecção do parceiro   | Retry com backoff; UI mostra quando parceiro não encontrado  |
| Conflitos de merge em edições simultâneas      | Perda de dados                  | Last-write-wins é determinístico; log de sync para auditoria |
| Consumo de bateria com NSD ativo               | Impacto em uso diário           | NSD ativo apenas em foreground; WorkManager para background  |
| Room migration pode falhar                     | App crash                       | Testes de migration; fallback para recreate                  |
| Wi-Fi Direct consumo excessivo                 | Drenagem de bateria em aparelhos fracos | Wi-Fi Direct APENAS para one-shot, NUNCA para sync contínuo |

---

## Estratégia de Teste

### Dispositivos de teste

| Dispositivo | Papel | Play Services | Observação |
|---|---|---|---|
| Device A | Device A — desenvolvimento + debug | Sim Sim | Device principal do desenvolvedor |
| Device B | Device B — teste de aceitação | Sim Sim | Teste real de uso familiar |

### Fluxo de teste

1. **Desenvolvimento** no Device A com `./gradlew installDebug`
2. **Gerar APK debug** para o Device B (`./gradlew assembleDebug`)
3. **Instalar** no Device B via `adb install` (conectar por USB ou Wi-Fi ADB)
4. **Testar pareamento** com ambos devices próximos (Nearby Connections)
5. **Testar sync LAN** com ambos devices na mesma rede Wi-Fi de casa

### O que testar por etapa

| Etapa | Cenário de teste |
|---|---|
| 1 | Parear devices → enviar dados → mesclar → verificar dados iguais |
| 2 | Editar em ambos → sincronizar → verificar conflitos resolvidos |
| 3 | Ambos na Wi-Fi → verificar sync automática → desligar Wi-Fi → verificar fallback |

### Dica: ADB over Wi-Fi

Para não precisar de cabo no Device B:

```bash
# No Device B (conectado por USB inicialmente)
adb tcpip 5555
adb connect <ip-do-device-b>:5555
# Agora pode desconectar o USB
adb -s <ip-do-device-b>:5555 install app/build/outputs/apk/debug/app-debug.apk
```

---

## Referências

- [Nearby Connections Overview](https://developers.google.com/nearby/connections/overview)
- [Wi-Fi Direct (P2P)](https://developer.android.com/develop/connectivity/wifi/wifip2p)
- [Network Service Discovery](https://developer.android.com/develop/connectivity/wifi/use-nsd)
- [Documento técnico de protocolos](./local-sharing-protocols.md)
