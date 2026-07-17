# US-101: Login com Google

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Autenticação  
**Fase**: 2

---

## História

> Como usuário do app,  
> Eu quero fazer login com minha conta Google,  
> Para que eu possa fazer backup dos meus dados no Google Drive e acessar recursos premium.

---

## Cenários de Aceite

### Cenário 1: Login bem-sucedido

```gherkin
DADO que estou usando o app sem login
QUANDO toco em "Entrar com Google"
ENTÃO vejo o seletor de contas Google do sistema
QUANDO seleciono minha conta
E autorizo o app
ENTÃO sou autenticado com sucesso
E vejo meu nome/foto na tela de configurações
E o estado muda para "Authenticated"
```

### Cenário 2: Primeiro login associa dados existentes

```gherkin
DADO que tenho dados locais (pets, pesos, etc.)
E nunca fiz login antes
QUANDO faço login com Google pela primeira vez
ENTÃO meus dados locais são associados ao meu userId
E posso continuar usando normalmente
```

### Cenário 3: Login cancelado

```gherkin
DADO que inicio o processo de login
QUANDO cancelo o seletor de contas
OU fecho o dialog
ENTÃO volto ao estado anterior (anônimo)
E não vejo mensagem de erro
E posso tentar novamente
```

### Cenário 4: Erro de rede no login

```gherkin
DADO que estou sem conexão de internet
QUANDO tento fazer login
ENTÃO vejo mensagem "Sem conexão. Tente novamente."
E continuo no modo anônimo
E o app continua funcionando normalmente offline
```

### Cenário 5: Login ativado ao tentar backup

```gherkin
DADO que estou usando o app sem login
E tenho dados locais (pets, pesos, etc.)
QUANDO tento fazer "Backup para Google Drive"
ENTÃO vejo dialog explicando que é necessário login
E tenho opção "Entrar com Google"
QUANDO faço login com sucesso
ENTÃO o backup é iniciado automaticamente
```

---

## UI/UX

### Tela de Configurações (Não Logado)

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 👤 CONTA                       │
│ ┌────────────────────────────┐ │
│ │        🔒                  │ │
│ │  Você não está logado      │ │
│ │                            │ │
│ │  Faça login para proteger  │ │
│ │  seus dados e acessar      │ │
│ │  recursos premium.         │ │
│ │                            │ │
│ │ ┌────────────────────────┐ │ │
│ │ │ 🔵 Entrar com Google   │ │ │
│ │ └────────────────────────┘ │ │
│ └────────────────────────────┘ │
│                                │
│ 📦 DADOS                       │
│ ...                            │
└────────────────────────────────┘
```

### Tela de Configurações (Logado)

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 👤 CONTA                       │
│ ┌────────────────────────────┐ │
│ │ ┌────┐                     │ │
│ │ │ 📷 │ Pessoa A            │ │
│ │ └────┘ pessoa-a@example.com │ │
│ │        Plano: Gratuito     │ │
│ │                            │ │
│ │ [Gerenciar conta]  [Sair]  │ │
│ └────────────────────────────┘ │
│                                │
│ ⭐ PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ Desbloqueie sync na nuvem, │ │
│ │ backup automático e mais!  │ │
│ │ [Ver planos]               │ │
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

### Fluxo de Login

```
┌──────────────────────────────────────────────────┐
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │                                            │  │
│  │  Escolha uma conta                         │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-a@example.com              │  │
│  │  │ 📷 │  Pessoa A                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-b@example.com              │  │
│  │  │ 📷 │  Pessoa B                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────────────────────────────────────┐   │  │
│  │  │ + Usar outra conta                 │   │  │
│  │  └────────────────────────────────────┘   │  │
│  │                                            │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## Requisitos Técnicos

### AuthRepository

```kotlin
interface AuthRepository {
    val authState: StateFlow<AuthState>
    
    suspend fun signInWithGoogle(idToken: String): Result<UserInfo>
    suspend fun signOut()
    fun getCurrentUser(): UserInfo?
    fun isLoggedIn(): Boolean
}
```

### Implementação com Credential Manager + Firebase Auth

```kotlin
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val context: Context
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = if (auth.currentUser != null) {
                auth.currentUser!!.toAuthState()
            } else {
                AuthState.Anonymous
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserInfo> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("User not found after sign in"))
            Result.success(user.toUserInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoogleIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential as? CustomCredential
            ?: throw Exception("Invalid credential")

        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
        return googleIdToken.idToken
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignorar erro de clear
        }
    }
}
```

### ViewModel

```kotlin
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState = authRepository.authState

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult = _loginResult.asSharedFlow()

    fun signInWithGoogle() {
        viewModelScope.launch {
            _loginResult.emit(LoginResult.Loading)
            
            try {
                val idToken = authRepository.getGoogleIdToken()
                authRepository.signInWithGoogle(idToken)
                    .onSuccess { user ->
                        _loginResult.emit(LoginResult.Success(user.userMetadata?.get("full_name")?.toString() ?: ""))
                    }
                    .onFailure { error ->
                        _loginResult.emit(LoginResult.Error(error.message ?: "Erro desconhecido"))
                    }
            } catch (e: Exception) {
                _loginResult.emit(LoginResult.Error(e.message ?: "Erro desconhecido"))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

sealed class LoginResult {
    object Loading : LoginResult()
    data class Success(val userName: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Cancelled : LoginResult()
}
```

### DataStore para Persistir Info Local

```kotlin
data class UserPreferences(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val lastLoginAt: Long? = null
)
```

---

## Configuração do Projeto

### Firebase

Adicionar o arquivo `google-services.json` (baixado do Firebase Console) ao módulo app.

### strings.xml

```xml
<resources>
    <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
</resources>
```

### BuildConfig

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${properties["GOOGLE_WEB_CLIENT_ID"]}\"")
    }
}
```

---

## Definition of Done

- [ ] Botão "Entrar com Google" implementado
- [ ] Credential Manager integrado
- [ ] Firebase Auth configurado
- [ ] Login bem-sucedido salva dados no DataStore
- [ ] Estado de autenticação refletido na UI
- [ ] Tratamento de erro de rede
- [ ] Tratamento de cancelamento
- [ ] Testes unitários do AuthRepository
- [ ] Testes de integração do fluxo de login
