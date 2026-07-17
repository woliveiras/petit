# Fase N - Firebase Auth ⏸️ ON HOLD

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por login Google e backup na nuvem.

## Motivo do Holding

Firebase Auth e serviços cloud foram adiados porque:
1. O app funciona 100% offline e atende às necessidades atuais
2. A demanda imediata é compartilhamento local entre dispositivos da casa
3. Firebase poderá ser reavaliado se houver demanda validada por backup na nuvem ou sync remoto

## Specs Preservadas

As specs abaixo foram migradas da Fase 2 original e serão adaptadas/atualizadas quando esta fase for retomada.

- [US-N01: Login com Google](./us-101-google-login.md)
- [US-N02: Gerenciamento de Conta](./us-102-account-management.md)
- [US-N03: Vinculação de Dados](./us-103-data-ownership.md)
- [US-N04: Gate Premium](./us-104-premium-gate.md)

---

## Pré-requisitos

- Fase 1 completa
- Google Cloud Console com OAuth configurado
- Firebase project configurado (google-services.json)

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-101](./us-101-google-login.md) | Login com Google | P0 |
| [US-102](./us-102-account-management.md) | Gerenciamento de Conta | P0 |
| [US-103](./us-103-data-ownership.md) | Vinculação de Dados | P1 |
| [US-104](./us-104-premium-gate.md) | Gate Premium | P1 |

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
