# Fase 2 - Compartilhamento Familiar (Local Sharing)

> **Documento migrado e reconciliado em 2026-07-17.** Origem:
> `getmiw/specs-miw@09b4497`. Os marcadores no escopo abaixo expressam a visão
> original, não o estado entregue. A seção "Estado verificado no Petit" é a
> referência para contribuidores.

## Estado verificado no Petit

| História | Estado | Evidência e lacunas principais |
|---|---|---|
| [US-101](./us-101-device-pairing.md) | Parcial | Nearby, permissões, telas e persistência existem. A entrada/validação do código de quatro dígitos descrita na história não existe na UI atual e faltam testes em dois dispositivos. |
| [US-102](./us-102-one-shot-transfer.md) | Parcial | Envio/recebimento de `ExportBundle`, merge/replace e UI existem. `REPLACE` ainda não remove dados locais ausentes no bundle; o fluxo completo também precisa de validação entre dispositivos. |
| [US-103](./us-103-family-group.md) | Parcial | Lista, remoção local, saída do grupo e DataStore existem. Renomear dispositivo e propagar remoção/saída não têm implementação comprovada. |
| [US-104](./us-104-lan-sync.md) | Planejado | Não foram encontrados NSD, servidor TCP, worker de LAN ou indicador global de sincronização. |
| [US-105](./us-105-conflict-resolution.md) | Parcial | O import existente compara `updatedAt` e grava `SyncLog`, mas não há `ConflictResolver` dedicado, tela de histórico, desempate para timestamps iguais ou testes das regras de soft delete. |

Os arquivos [`us-103-family-group.md`](./us-103-family-group.md) e
[`us-103-household-group.md`](./us-103-household-group.md) descrevem a mesma
história. O segundo é uma variante legada preservada e marcada como duplicada.

## Objetivo

Permitir que membros de uma casa **compartilhem dados dos pets entre seus dispositivos** usando protocolos locais (Nearby Connections + NSD), **sem depender de servidor remoto ou internet**.

Cenário principal: dois dispositivos Android na mesma casa, compartilhando dados sobre os mesmos pets, com sync automático quando ambos estão na mesma rede Wi-Fi.

## Escopo

- [ ] Pareamento inicial entre dispositivos (Nearby Connections)
- [ ] Transferência one-shot de dados (enviar/receber/mesclar)
- [ ] Grupo familiar local (gerenciado no device, sem cloud)
- [ ] Sync contínuo na rede local (NSD + TCP, quando ambos na mesma Wi-Fi)
- [ ] Resolução de conflitos local (last-write-wins por updatedAt)
- Não inclui login Google
- Não inclui Firebase ou serviços cloud
- Não requer internet para as operações locais

---

## Pré-requisitos

- Fase 1 completa
- Dispositivos com Google Play Services (para Nearby Connections)
- Wi-Fi ou Bluetooth habilitado

> **Nota sobre compatibilidade**: Dispositivos sem Google Play Services (ex: LineageOS sem GApps) podem usar o Mode 2 (NSD + TCP) normalmente. Apenas o Mode 1 (Nearby Connections) requer Play Services.

---

## Decisões de Bateria e Protocolo

### Por que NSD + TCP sobre Wi-Fi normal (e não Wi-Fi Direct) para sync contínuo?

| Estratégia | Consumo extra | Aparelho fraco (3000mAh) |
|---|---|---|
| Wi-Fi Direct contínuo | ~150-300mW | Drena rápido, **inviável** |
| NSD + TCP sobre Wi-Fi normal | ~5-15mW | **Desprezível** |
| WorkManager periódico (15min) | ~2-5mW (burst) | **Imperceptível** |
| Nearby Connections (one-shot) | ~200mW por poucos segundos | **OK** |

Wi-Fi Direct cria um grupo P2P onde um dispositivo age como soft AP — o rádio **nunca dorme**. Para transferência pontual (segundos), OK. Para sync contínuo em background, inviável.

NSD + TCP usam o **Wi-Fi de infraestrutura** (roteador da casa) — o rádio que **já está ligado** para internet. O custo incremental é quase zero.

### Regras obrigatórias

1. **Wi-Fi Direct NUNCA deve ser usado para sync contínuo** — apenas para one-shot (Mode 1)
2. Sync contínuo (Mode 2) usa **exclusivamente Wi-Fi de infraestrutura** (NSD + TCP)
3. NSD deve ser **lifecycle-aware**: registrar em foreground, desregistrar ao sair do app
4. WorkManager com constraints `NetworkType.CONNECTED` para sync em background
5. Changesets devem usar **batching** (acumular mudanças, não sync a cada escrita)

### Por que Nearby Connections ao invés de Wi-Fi Direct puro para one-shot?

- Nearby Connections abstrai Bluetooth/BLE/Wi-Fi Direct automaticamente
- Criptografia end-to-end built-in
- UX 100% dentro do app (sem popups do SO, sem pairing Bluetooth)
- Fallback automático entre transportes
- Wi-Fi Direct puro exigiria gerenciar grupos P2P manualmente

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-101](./us-101-device-pairing.md) | Pareamento de Dispositivos | P0 |
| [US-102](./us-102-one-shot-transfer.md) | Transferência One-Shot | P0 |
| [US-103](./us-103-family-group.md) | Grupo Familiar Local | P1 |
| [US-104](./us-104-lan-sync.md) | Sync Contínuo na Rede Local | P0 |
| [US-105](./us-105-conflict-resolution.md) | Resolução de Conflitos Local | P1 |

---

## Arquitetura

### Dois Modos de Compartilhamento

```
┌─────────────────────────────────────────────────────────────┐
│                    MODO 1: PAREAMENTO + ONE-SHOT             │
│                    (Nearby Connections API)                   │
│                                                              │
│  ┌─────────────┐   Bluetooth/    ┌─────────────┐            │
│  │  Device A   │   Wi-Fi Direct  │  Device B   │            │
│  │  (Device A)  │ ◀────────────▶ │  (Device B) │            │
│  └─────────────┘   P2P_POINT    └─────────────┘            │
│                    _TO_POINT                                 │
│                                                              │
│  Usado para: pareamento inicial, transferência               │
│  completa, quando devices não estão na mesma Wi-Fi           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    MODO 2: SYNC CONTÍNUO                     │
│                    (NSD + TCP Sockets)                        │
│                                                              │
│  ┌─────────────┐      Wi-Fi      ┌─────────────┐            │
│  │  Device A   │   (mesma rede)  │  Device B   │            │
│  │  (Device A)  │ ◀────────────▶ │  (Device B) │            │
│  └─────────────┘   NSD + TCP    └─────────────┘            │
│                                                              │
│  Usado para: sync automático em background quando            │
│  ambos devices estão na rede Wi-Fi de casa                   │
└─────────────────────────────────────────────────────────────┘
```

### Fluxo de Pareamento (primeira vez)

```
Device A (Transmissor)                Device B (Receptor)
─────────────────────                 ────────────────────
1. Perfil > Grupo Familiar            1. Perfil > Grupo Familiar
2. Toca "Parear Dispositivo"          2. Toca "Entrar em Grupo"
3. Gera código 4 dígitos             3. Insere código
3. Inicia advertising                 3. Inicia discovery
   (Nearby Connections)                  (Nearby Connections)
4. Aceita conexão                     4. Conecta
5. Troca chave do grupo familiar      5. Salva chave
6. Envia ExportBundle JSON            6. Recebe dados
7. Conexão encerrada                  7. Merge ou Replace
                                      8. Ambos agora "pareados"
```

### Fluxo de Sync Contínuo (após pareamento)

```
┌──────────┐   NSD Discovery   ┌──────────┐
│ Device A │ ◀───────────────▶ │ Device B │
│  Room    │    "_petit._tcp"    │  Room    │
└────┬─────┘                   └────┬─────┘
     │                              │
     │  TCP: enviar changesets      │
     │  (entidades com updatedAt    │
     │   > lastSyncTimestamp)       │
     │ ────────────────────────▶    │
     │                              │
     │  TCP: receber changesets     │
     │ ◀────────────────────────    │
     │                              │
     ▼                              ▼
  Merge por                      Merge por
  updatedAt                      updatedAt
  (last-write-wins)              (last-write-wins)
```

---

## Modelo de Dados Adicional

### FamilyGroupMember (novo)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador do membro |
| `deviceName` | String | Nome do dispositivo (ex: "Device A") |
| `familyGroupKey` | String | Chave compartilhada do grupo familiar |
| `lastSyncAt` | Long | Timestamp da última sincronização |
| `createdAt` | Long | Timestamp de criação |

### SyncLog (implementado como `SyncLogEntity`)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador |
| `peerId` | UUID | ID do membro com quem sincronizou |
| `lastSyncTimestamp` | Long | Timestamp da última sync |
| `entitiesSent` | Int | Número de entidades enviadas |
| `entitiesReceived` | Int | Número de entidades recebidas |

---

## Tecnologias

### Nearby Connections API
- **Uso**: Pareamento inicial + transferência one-shot
- **Strategy atual**: `P2P_STAR`
- **Transporte**: Bluetooth + Wi-Fi Direct (automático, gerenciado pelo Google Play Services)
- **Criptografia**: Automática (built-in)
- **Alcance**: ~100m

### NSD (Network Service Discovery)
- **Uso**: Discovery de devices na mesma rede Wi-Fi
- **Protocolo**: DNS-SD (mDNS, Bonjour-compatible)
- **Service Type**: `_petit._tcp`
- **Transporte**: TCP sockets sobre Wi-Fi local

### Referência técnica
- [Documento de protocolos](../local-sharing-protocols.md)

---

## Permissões

```xml
<!-- Nearby Connections (pareamento + one-shot) -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
                 android:maxSdkVersion="32" />

<!-- NSD (sync contínuo na rede local) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

---

## Segurança

- **Pareamento com código**: Código de 4 dígitos para validação na primeira conexão
- **Family Group Key**: Chave UUID gerada no pareamento, validada em toda sync
- **Proximity-based**: Nearby Connections funciona apenas com devices próximos
- **Rede local**: NSD sync funciona apenas na mesma rede Wi-Fi
- **Sem cloud**: Nenhum dado trafega pela internet
- **Criptografia**: Nearby Connections fornece o transporte protegido. TLS com
  chave compartilhada para o TCP local é requisito planejado e ainda não está
  implementado.

---

## Material legado misturado na fonte

> **Fora do escopo desta fase e superseded.** O repositório de origem continha
> o bloco abaixo sobre autenticação, premium e Crashlytics dentro do README de
> compartilhamento local. Ele foi mantido integralmente para que nada se perca,
> mas não representa o plano atual do Petit. As histórias pertenciam a outro
> conjunto e agora estão preservadas em [`holding/firebase-auth`](../holding/firebase-auth/README.md).

## Pré-requisitos legados

- Fase 1 completa
- Google Cloud Console com OAuth configurado
- Firebase project configurado (google-services.json)

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [us-101-google-login.md](../holding/firebase-auth/us-101-google-login.md) (holding) | Login com Google | P0 |
| [us-102-account-management.md](../holding/firebase-auth/us-102-account-management.md) (holding) | Gerenciamento de Conta | P0 |
| [us-103-data-ownership.md](../holding/firebase-auth/us-103-data-ownership.md) (holding) | Vinculação de Dados | P1 |
| [us-104-premium-gate.md](../holding/firebase-auth/us-104-premium-gate.md) (holding) | Gate Premium | P1 |

---

## Arquitetura

### Fluxo de Autenticação

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Login     │────▶│  Credential │────▶│  Firebase   │
│   Button    │     │   Manager   │     │    Auth     │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ Firebase    │
                                        │ UserInfo    │
                                        │ id, email   │
                                        └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │  DataStore  │
                                        │  (local)    │
                                        └─────────────┘
```

### Estados de Autenticação

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    object Anonymous : AuthState()  // Usando sem login (free)
    data class Authenticated(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val isPremium: Boolean
    ) : AuthState()
}
```

---

## Configuração Firebase

### 1. Firebase Console

1. Criar projeto no Firebase Console
2. Habilitar Authentication > Google como provider
3. Baixar e adicionar `google-services.json` ao módulo app
4. Configurar Google Client ID para Credential Manager

### 2. Dependências

```kotlin
// build.gradle.kts (app)
dependencies {
    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:VERSION"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Credential Manager (continua sendo usado para obter ID Token do Google)
    implementation("androidx.credentials:credentials:1.2.x")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.x")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.x.x")
}
```

---

## Critérios de Aceite Globais

- [ ] Login com Google funciona via Credential Manager + Firebase Auth
- [ ] Usuário pode usar o app sem login (modo anônimo)
- [ ] Logout limpa estado de autenticação mas mantém dados locais
- [ ] Token Firebase Auth é renovado automaticamente
- [ ] UI reflete estado de autenticação corretamente
- [ ] Preparação para verificação de premium status

---

## Firebase Crashlytics (Complementar)

Junto com a implementação do Firebase Auth, adicionar Firebase Crashlytics para monitoramento de crashes (serviço Firebase complementar gratuito).

### Motivação

- Detectar crashes em produção
- Entender padrões de erros
- Priorizar correções com base em impacto real
- Ter visibilidade antes que usuários reportem

### Dependências Adicionais

```kotlin
// build.gradle.kts (project)
plugins {
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// build.gradle.kts (app)
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

### Configuração ProGuard/R8

Para que os stack traces sejam legíveis, configurar mapeamento no build:

```kotlin
// build.gradle.kts (app)
android {
    buildTypes {
        release {
            // Já existente
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Adicionar para Crashlytics
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
}
```

### Inicialização

Crashlytics inicia automaticamente. Para desabilitar coleta em debug:

```kotlin
// Application.kt
class PetitApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Desabilitar Crashlytics em debug
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
```

### Critérios de Aceite

- [ ] Crashlytics configurado e recebendo eventos
- [ ] Stack traces são legíveis (não ofuscados)
- [ ] Coleta desabilitada em build de debug
- [ ] Dashboard Firebase mostra crashes corretamente
