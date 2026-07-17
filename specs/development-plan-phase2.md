# Plano de Desenvolvimento — Fase 2: Local Sharing

> **Documento migrado e reconciliado em 2026-07-17.** Origem:
> `getmiw/specs-miw@09b4497`. Este é o plano histórico, não uma declaração de
> conclusão. A Etapa 1 está parcialmente implementada no Petit; as Etapas 2 e
> 3 ainda contêm trabalho planejado. O estado verificável por história está em
> [`phase-2/README.md`](./phase-2/README.md).

## Contexto

Compartilhamento de dados de pets entre dois dispositivos na mesma casa, sem depender de internet ou servidores remotos.

**Dispositivos de teste sugeridos:**
- Device A — desenvolvimento e debug
- Device B — teste de aceitação

**Estado verificado no Petit em 2026-07-17:**
- `PetitDatabase` v1, com sete entidades, incluindo
  `FamilyGroupMemberEntity` e `SyncLogEntity`
- `ExportBundle` usa pets, export/import JSON e merge por `updatedAt`
- Hilt DI e WorkManager têm inicialização customizada
- Nearby Connections, permissões e rotas de grupo familiar já existem
- NSD + TCP, worker de LAN e indicador global de sync ainda não existem

---

## Resumo de Decisões Técnicas

| Decisão | Escolha | Motivo |
|---|---|---|
| Pareamento + one-shot | Nearby Connections API | UX dentro do app, criptografia built-in, abstrai BLE/BT/Wi-Fi Direct |
| Sync contínuo | NSD + TCP sobre Wi-Fi infraestrutura | Bateria desprezível (~5-15mW), usa rádio já ativo |
| Wi-Fi Direct para sync contínuo | **PROIBIDO** | Drena 10-15%/hora em aparelhos fracos |
| Resolução de conflitos | Last-write-wins por updatedAt | Determinístico, idempotente, já usado no ExportImportUseCase |
| Background sync | WorkManager periódico (15min) | Respeita constraints do Android, battery-friendly |
| i18n | pt-BR, en, es | Todos os textos visíveis ao usuário via `strings.xml` |

---

## Internacionalização (i18n)

O app suporta **3 idiomas**: pt-BR, English e Español.

Todos os textos visíveis ao usuário **devem** estar em `strings.xml`, nunca hardcoded no Compose:

```
res/
├── values/strings.xml          (English — padrão)
├── values-pt-rBR/strings.xml   (pt-BR)
└── values-es/strings.xml       (Español)
```

### Strings da Fase 2

| Key | pt-BR | en | es |
|---|---|---|---|
| `nav_profile` | Perfil | Profile | Perfil |
| `profile_section_family_group` | Grupo Familiar | Family Group | Grupo Familiar |
| `profile_section_settings` | Configurações | Settings | Configuración |
| `profile_section_data` | Dados | Data | Datos |
| `profile_section_about` | Sobre | About | Acerca de |
| `family_group_title` | Grupo Familiar | Family Group | Grupo Familiar |
| `family_group_onboarding_description` | Compartilhe os dados dos seus pets com sua família. | Share your pets' data with your family. | Comparte los datos de tus mascotas con tu familia. |
| `family_group_pair_device` | Parear Dispositivo | Pair Device | Vincular Dispositivo |
| `family_group_join` | Entrar em Grupo | Join Group | Unirse al Grupo |
| `family_group_manage` | Gerenciar Grupo | Manage Group | Gestionar Grupo |
| `family_group_send_data` | Enviar Dados | Send Data | Enviar Datos |
| `family_group_receive_data` | Receber Dados | Receive Data | Recibir Datos |
| `family_group_merge` | Mesclar com Locais | Merge with Local | Combinar con Locales |
| `family_group_replace` | Substituir Dados Locais | Replace Local Data | Reemplazar Datos Locales |
| `family_group_pairing_code` | Código de pareamento | Pairing code | Código de vinculación |
| `family_group_waiting_connection` | Aguardando conexão... | Waiting for connection... | Esperando conexión... |
| `family_group_paired_success` | Pareamento concluído! | Pairing complete! | ¡Vinculación completada! |
| `family_group_sync_auto` | Sync automático | Auto sync | Sincronización automática |
| `family_group_sync_now` | Forçar Sync Agora | Sync Now | Sincronizar Ahora |
| `family_group_leave` | Sair do Grupo | Leave Group | Salir del Grupo |
| `family_group_remove_device` | Remover dispositivo | Remove device | Eliminar dispositivo |
| `family_group_last_sync` | Última sync: %s | Last sync: %s | Última sincronización: %s |
| `family_group_no_internet_hint` | Funciona sem internet! | Works without internet! | ¡Funciona sin internet! |
| `family_group_sync_history` | Histórico de Sync | Sync History | Historial de Sincronización |
| `family_group_sync_settings` | Config. de Sync | Sync Settings | Config. de Sincronización |
| `sync_indicator_synced` | Sincronizado | Synced | Sincronizado |
| `sync_indicator_syncing` | Sincronizando... | Syncing... | Sincronizando... |
| `sync_indicator_offline` | Parceiro não encontrado | Partner not found | Compañero no encontrado |

> **Regra:** Nenhum texto visível ao usuário pode ser hardcoded em Kotlin/Compose. Usar `stringResource(R.string.key)`.

---

## Etapa 1: Pareamento + Transferência One-Shot

> **Resultado:** Dois devices podem parear e transferir dados manualmente. Funciona sem internet.

### 1.1 — Dependências e Permissões

**Adicionar em `gradle/libs.versions.toml`:**

```toml
[versions]
playServicesNearby = "19.3.0"

[libraries]
play-services-nearby = { group = "com.google.android.gms", name = "play-services-nearby", version.ref = "playServicesNearby" }
```

**Adicionar em `build.gradle.kts` (app):**

```kotlin
implementation(libs.play.services.nearby)
```

**Adicionar em `AndroidManifest.xml`:**

```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:usesPermissionFlags="neverForLocation" />
<!-- ACCESS_FINE_LOCATION necessário apenas para API < 33 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="32" />
```

### 1.2 — Persistência Room

> **Estado atual:** a migração v4 → v5 abaixo pertence ao app anterior e
> não deve ser aplicada ao Petit. O `PetitDatabase` está na versão 1 e já
> declara `family_group_members` e `sync_logs`. O SQL foi preservado apenas
> como referência histórica; qualquer mudança futura exige uma nova migration
> baseada no schema exportado atual.

**Novas tabelas:**

```sql
CREATE TABLE family_group_members (
    id TEXT PRIMARY KEY NOT NULL,
    deviceName TEXT NOT NULL,
    familyGroupKey TEXT NOT NULL,
    isLocalDevice INTEGER NOT NULL DEFAULT 0,
    lastSyncAt INTEGER,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

CREATE TABLE sync_logs (
    id TEXT PRIMARY KEY NOT NULL,
    peerId TEXT NOT NULL,
    peerName TEXT NOT NULL,
    syncTimestamp INTEGER NOT NULL,
    entitiesSent INTEGER NOT NULL,
    entitiesReceived INTEGER NOT NULL,
    conflictsResolved INTEGER NOT NULL,
    syncType TEXT NOT NULL
);
```

**Passos históricos no banco anterior:**
- Adicionar `FamilyGroupMemberEntity` e `SyncLogEntity` nas entities
- Adicionar `FamilyGroupMemberDao` e `SyncLogDao`
- Criar `MIGRATION_4_5`
- Version → 5

### 1.3 — Modelos de Domínio

Criar na ordem (sem dependências externas):

| # | Arquivo | Pacote | Descrição |
|---|---|---|---|
| 1 | `FamilyGroupMember.kt` | domain/model | Membro do grupo familiar |
| 2 | `FamilyGroupInfo.kt` | domain/model | Info do grupo familiar (key, membros, criação) |
| 3 | `PairingState.kt` | domain/model | Sealed class: Idle, Waiting, Requested, Paired, Error |
| 4 | `TransferState.kt` | domain/model | Sealed class: Idle, Sending, Receiving, Complete, Error |
| 5 | `MergeResult.kt` | domain/model | Resultado do merge (contadores por tipo) |
| 6 | `SyncLog.kt` | domain/model | Domain model do log de sync |

### 1.4 — Data Layer

| # | Arquivo | Descrição |
|---|---|---|
| 1 | `FamilyGroupMemberEntity.kt` | Room entity |
| 2 | `SyncLogEntity.kt` | Room entity |
| 3 | `FamilyGroupMemberDao.kt` | DAO com queries CRUD + flow |
| 4 | `SyncLogDao.kt` | DAO com insert + query por data |
| 5 | `FamilyGroupMapper.kt` | Entity ↔ Domain |
| 6 | `FamilyGroupRepository.kt` | Interface: create/join/leave/getMembers |
| 7 | `FamilyGroupRepositoryImpl.kt` | Impl com Room + DataStore |
| 8 | `NearbyTransferRepository.kt` | Interface: start/stop advertising/discovery, send/receive |
| 9 | `NearbyTransferRepositoryImpl.kt` | Impl com Nearby Connections API |

**DataStore keys** (em `FamilyGroupPreferences`):
- `FAMILY_GROUP_KEY: stringPreferencesKey`
- `LOCAL_DEVICE_ID: stringPreferencesKey`
- `LOCAL_DEVICE_NAME: stringPreferencesKey`
- `SYNC_ENABLED: booleanPreferencesKey`

### 1.5 — Use Cases

| # | Arquivo | Responsabilidade |
|---|---|---|
| 1 | `CreateFamilyGroupUseCase.kt` | Gera family group key + registra device local como primeiro membro |
| 2 | `JoinFamilyGroupUseCase.kt` | Valida código + registra device como membro |
| 3 | `SendDataUseCase.kt` | Serializa ExportBundle + envia via NearbyTransferRepository |
| 4 | `ReceiveDataUseCase.kt` | Recebe bundle + oferece merge/replace |
| 5 | `MergeDataUseCase.kt` | Reutiliza lógica do ExportImportUseCase (merge por updatedAt) |

> **Nota:** `MergeDataUseCase` deve reaproveitar o `ExportImportUseCase.importData()` que já implementa KEEP/REPLACE/MERGE.

### 1.6 — Presentation Layer: Profile Refactor + Family Group

> **Decisão original não adotada:** o Petit atual mantém `SettingsScreen` e
> `SettingsViewModel` na rota `settings`. A seção de grupo familiar foi
> integrada ali; os passos de renomeação abaixo permanecem como histórico e
> não devem ser executados sem uma nova decisão de produto.

A tab "Profile" do bottom nav (já existente com ícone `Icons.Filled.Person`) hoje
redireciona para `SettingsScreen`. Vamos:

1. Renomear `SettingsScreen` → `ProfileScreen` (mesma rota `settings`)
2. Reorganizar em seções: Grupo Familiar → Configurações → Dados → Sobre
3. A seção Grupo Familiar mostra status inline ou card de onboarding
4. Telas específicas do grupo familiar navegam a partir do Profile

**Refactor de arquivos existentes:**

| Arquivo atual | Ação |
|---|---|
| `SettingsScreen.kt` | Renomear → `ProfileScreen.kt`, reorganizar em seções |
| `SettingsViewModel.kt` | Renomear → `ProfileViewModel.kt`, adicionar estado do grupo familiar |

**Novos arquivos:**

| # | Arquivo | Pacote | Descrição |
|---|---|---|---|
| 1 | `ProfileScreen.kt` | presentation/feature/profile | Hub: grupo familiar + configurações + dados + sobre |
| 2 | `ProfileViewModel.kt` | presentation/feature/profile | Estado do grupo + tema + idioma |
| 3 | `FamilyGroupSection.kt` | presentation/feature/profile | Seção inline no Profile (status ou onboarding) |
| 4 | `FamilyGroupScreen.kt` | presentation/feature/familygroup | Gerenciamento do grupo (dispositivos, status) |
| 5 | `FamilyGroupViewModel.kt` | presentation/feature/familygroup | VM do gerenciamento |
| 6 | `PairingScreen.kt` | presentation/feature/familygroup | Código 4 dígitos (transmissor/receptor) |
| 7 | `PairingViewModel.kt` | presentation/feature/familygroup | VM do pareamento |
| 8 | `TransferScreen.kt` | presentation/feature/familygroup | Envio/recebimento com progresso |
| 9 | `TransferViewModel.kt` | presentation/feature/familygroup | VM da transferência |

**Navegação a partir do Profile:**

```
ProfileScreen (route: "settings")
  ├─→ FamilyGroupScreen (route: "familygroup")
  │     ├─→ PairingScreen (route: "familygroup/pairing")
  │     ├─→ TransferScreen (route: "familygroup/transfer")
  │     └─→ SyncSettingsScreen (route: "familygroup/sync-settings") ← Etapa 3
  └─→ DeleteAllDataScreen (route: "settings/delete-all-data") ← já existe
```

**Wireframe do ProfileScreen:**

```
┌────────────────────────────────────────┐
│  Perfil                                │
├────────────────────────────────────────┤
│                                        │
│  GRUPO FAMILIAR                        │
│  ┌──────────────────────────────────┐  │
│  │ [Pareado]                          │  │
│  │ Device B              │  │
│  │ Sincronizado • há 2 min           │  │
│  │                                  │  │
│  │ GERENCIAR GRUPO             →   │  │
│  └──────────────────────────────────┘  │
│                                        │
│  --- ou, se não pareado: ---           │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │ Compartilhe os dados dos seus   │  │
│  │ pets com sua família.         │  │
│  │ Funciona sem internet!         │  │
│  │                                │  │
│  │ [PAREAR DISPOSITIVO]           │  │
│  │ [ENTRAR EM GRUPO]              │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─   │
│                                        │
│  CONFIGURAÇÕES                          │
│  ┌──────────────────────────────────┐  │
│  │ Tema                  Sistema → │  │
│  │ Idioma              Português → │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─   │
│                                        │
│  DADOS                                  │
│  ┌──────────────────────────────────┐  │
│  │ Exportar dados                →  │  │
│  │ Importar dados                →  │  │
│  │ Apagar todos os dados         →  │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─   │
│                                        │
│  SOBRE                                  │
│  ┌──────────────────────────────────┐  │
│  │ Versão                      1.0.0 │  │
│  └──────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

**Regras de UI (seguindo skill ui-ux-design):**
- Sem emojis: usar Material Icons (`Icons.Default.*`)
- Tema e idioma continuam como `ModalBottomSheet` (inline, sem navegar)
- Usar `MaterialTheme.colorScheme` tokens, nunca cores hardcoded
- Seções separadas por `HorizontalDivider` com `outlineVariant`
- Touch targets ≥ 48dp

### 1.7 — DI e Navegação

| # | Ação |
|---|---|
| 1 | Criar `FamilyGroupModule.kt` — binds do FamilyGroupRepository e NearbyTransferRepository |
| 2 | Atualizar `DatabaseModule.kt` — adicionar `FamilyGroupMemberDao` e `SyncLogDao` |
| 3 | Adicionar routes `familygroup`, `familygroup/pairing`, `familygroup/transfer` em `Screen.kt` |
| 4 | Adicionar composables na NavGraph |
| 5 | Renomear `SettingsScreen` → `ProfileScreen`, mover para `feature/profile/` |
| 6 | Renomear `SettingsViewModel` → `ProfileViewModel`, adicionar estado do grupo |
| 7 | Reorganizar ProfileScreen em seções: Grupo Familiar, Configurações, Dados, Sobre |

### 1.8 — Permissões Runtime

- Solicitar `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `NEARBY_WIFI_DEVICES` antes de iniciar pairing
- Para API < 33: solicitar `ACCESS_FINE_LOCATION`
- Usar `rememberMultiplePermissionsState` (Accompanist) ou implementação manual

### Ordem de Implementação — Etapa 1

```
 1. libs.versions.toml + build.gradle.kts (dependência Nearby)
 2. AndroidManifest.xml (permissões)
 3. Modelos de domínio (FamilyGroupMember, PairingState, etc.)
 4. Room entities + DAOs + MIGRATION_4_5
 5. FamilyGroupMapper
 6. FamilyGroupRepository + impl
 7. NearbyTransferRepository + impl
 8. Use cases (Create, Join, Send, Receive, Merge)
 9. FamilyGroupModule.kt (Hilt)
10. DatabaseModule.kt (update DAOs)
11. Renomear SettingsScreen → ProfileScreen, reorganizar em seções
12. ProfileViewModel (absorver SettingsViewModel + estado do grupo)
13. FamilyGroupSection.kt (seção inline no Profile)
14. ViewModels (FamilyGroup, Pairing, Transfer)
15. Telas Compose (FamilyGroup, Pairing, Transfer)
16. Screen.kt + NavGraph (novas rotas)
17. Permissões runtime
18. Testes: Room migration, ConflictResolver, Use Cases
```

### Testar — Etapa 1

```bash
# Build e instalar no Device A (conectado por USB)
./gradlew assembleDebug && ./gradlew installDebug

# Gerar APK para Device B
./gradlew assembleDebug

# Instalar no Device B (via ADB over Wi-Fi ou USB)
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Cenários para validar:**
1. Parear: Device A gera código → Device B insere → conexão OK
2. Enviar: Device A envia dados → Device B recebe → escolhe Mesclar → dados iguais
3. Substituir: Device B escolhe Substituir → dados locais apagados → dados de A importados
4. Sem internet: Desligar Wi-Fi e dados móveis → pareamento funciona via Bluetooth
5. Cancelar: Parear e cancelar no meio → nenhum dado perdido

---

## Etapa 2: Grupo Familiar + Conflict Resolution

> **Resultado:** Membros do grupo são visíveis e gerenciáveis. Conflitos resolvidos automaticamente.

### Novas Classes

| # | Arquivo | Descrição |
|---|---|---|
| 1 | `ConflictResolver.kt` | Regras de merge (data/repository) |
| 2 | `LeaveFamilyGroupUseCase.kt` | Sair do grupo, manter dados locais |
| 3 | `RemoveMemberUseCase.kt` | Remover membro do grupo |
| 4 | `FamilyGroupScreen.kt` | Tela de gerenciamento (presentation/feature/familygroup) |
| 5 | `FamilyGroupViewModel.kt` | VM do grupo |
| 6 | `SyncLogScreen.kt` | Histórico de syncs (presentation/feature/familygroup) |

**Navegação:**
- `FamilyGroupScreen` acessada via ProfileScreen > "Gerenciar Grupo"
- `SyncLogScreen` acessada via FamilyGroupScreen > "Histórico de Sync"
- Novas rotas: `familygroup/sync-log`

### ConflictResolver — Regras

```
Para cada entidade remota:
  1. Não existe local       → INSERT
  2. remote.deletedAt != null:
     a. local.updatedAt > remote.deletedAt → KEEP (edição desfaz delete)
     b. senão → APPLY_DELETE
  3. remote.updatedAt > local.updatedAt → UPDATE
  4. senão → KEEP_LOCAL
```

### Ordem de Implementação — Etapa 2

```
1. ConflictResolver (extrair lógica do ExportImportUseCase.merge)
2. Integrar ConflictResolver no MergeDataUseCase
3. SyncLog writing (gravar em sync_logs após cada operação)
4. LeaveFamilyGroupUseCase + RemoveMemberUseCase
5. FamilyGroupScreen + ViewModel
6. SyncLogScreen
7. Testes unitários: ConflictResolver (todos os cenários do US-105)
```

### Testar — Etapa 2

1. Editar peso do Pet A no Device A (10:00) e Device B (10:05) → sync → 10:05 prevalece
2. Deletar registro no A, editar no B (mais recente) → edição prevalece
3. Ambos deletam → permanece deletado
4. Ver histórico de sync → mostra contadores corretos
5. Remover membro → device removido mantém dados, perde referência de sync

---

## Etapa 3: Sync Contínuo na Rede Local (NSD + TCP)

> **Resultado:** Sync automática e contínua quando ambos devices estão na mesma Wi-Fi.

### Novas Classes

| # | Arquivo | Descrição |
|---|---|---|
| 1 | `NsdServiceManager.kt` | Register/discover `_petit._tcp` (data/repository) |
| 2 | `TcpSyncServer.kt` | Accept connections + handshake + changeset exchange (data/repository) |
| 3 | `LanSyncRepository.kt` | Interface: start/stop, syncWithPeer, getChangesSince |
| 4 | `LanSyncRepositoryImpl.kt` | Orquestra NSD + TCP |
| 5 | `LanSyncWorker.kt` | WorkManager worker (worker/) |
| 6 | `LanSyncModule.kt` | Hilt module (di/) |
| 7 | `SyncIndicator.kt` | Composable no slot `actions` do PetitTopAppBar (presentation/components) |
| 8 | `SyncSettingsScreen.kt` | On/off + status (presentation/feature/familygroup, route: `familygroup/sync-settings`) |

**Navegação:**
- `SyncSettingsScreen` acessada via FamilyGroupScreen > "Config. de Sync"
- `SyncIndicator` exibido globalmente no PetitTopAppBar (só se parte de um grupo)
- Nova rota: `familygroup/sync-settings`

### Protocolo TCP

```
Client → Server:  HELLO {familyGroupKey} {deviceId} {lastSyncTimestamp}
Server valida familyGroupKey → se inválida: ERROR + desconecta
Server → Client:  HELLO_ACK {deviceId} {lastSyncTimestamp}
Server → Client:  CHANGESET {json das entidades com updatedAt > client.lastSyncTimestamp}
Client → Server:  CHANGESET {json das entidades com updatedAt > server.lastSyncTimestamp}
Ambos:            ACK {newSyncTimestamp}
Ambos:            CLOSE
```

### Regras de Bateria (obrigatórias)

| Contexto | Comportamento |
|---|---|
| App em foreground | NSD ativo, TCP on-demand |
| App em background | WorkManager periódico (15min mínimo) |
| App fechado | NSD desregistrado, sem consumo |
| Sem parceiro na rede | Discovery para após timeout, retry via WorkManager |

> **PROIBIDO:** Wi-Fi Direct para sync contínuo. Usar EXCLUSIVAMENTE Wi-Fi de infraestrutura.

### WorkManager Config

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<LanSyncWorker>(
    15, TimeUnit.MINUTES,
    5, TimeUnit.MINUTES  // flex interval
).setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "lan_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

### Lifecycle-aware NSD

- `ON_START` → registrar serviço NSD + iniciar discovery
- `ON_STOP` → desregistrar NSD (economia de bateria)
- WorkManager mantém sync periódica mesmo com app em background

### Ordem de Implementação — Etapa 3

```
 1. NsdServiceManager (register + discover + resolve)
 2. TcpSyncServer (ServerSocket + handshake + changeset exchange)
 3. LanSyncRepository + impl (orquestra NSD + TCP + ConflictResolver)
 4. LanSyncWorker
 5. LanSyncModule (Hilt)
 6. SyncIndicator (composable)
 7. Integrar SyncIndicator no PetitTopAppBar
 8. SyncSettingsScreen (on/off + status do parceiro)
 9. Lifecycle observer para NSD registration
10. Testes de integração: dois processos simulando TCP exchange
```

### Testar — Etapa 3

1. Ambos os dispositivos na mesma Wi-Fi → indicador Sim aparece na toolbar
2. Editar dado no A → aparece no B automaticamente (dentro de ~15s em foreground)
3. Desligar Wi-Fi no B → indicador muda para [dispositivo] no A
4. Religar Wi-Fi no B → sync automática das mudanças acumuladas
5. Desabilitar sync nas configurações → NSD para, indicador some
6. Background: minimizar app em ambos → WorkManager sincroniza a cada 15min

---

## Dependências entre Etapas

```
Etapa 1 ─────► Etapa 2 ─────► Etapa 3
(Parear +       (Grupo +        (NSD +
 One-Shot)       Conflitos)      TCP Auto)
```

Cada etapa entrega valor funcional independente:
- **Após Etapa 1**: Já é possível compartilhar dados manualmente
- **Após Etapa 2**: Conflitos são tratados corretamente
- **Após Etapa 3**: Sync é automática, sem intervenção do usuário

---

## Checklist de Qualidade por Etapa

### Para cada etapa, validar:

- [ ] `./gradlew spotlessCheck` passa
- [ ] `./gradlew test` passa
- [ ] Room migration testada (criar teste de migration v4→v5)
- [ ] Acessibilidade: contentDescription em ícones, touch targets ≥ 48dp
- [ ] Dark theme: todas as telas novas funcionam em dark mode
- [ ] Permissões: solicitadas antes do uso, fallback se negadas
- [ ] Lifecycle: NSD registra/desregistra corretamente
- [ ] Bateria: Wi-Fi Direct nunca usado para sync contínuo
