# US-102: Gerenciamento de Conta

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P0  
**Épico**: Autenticação  
**Fase**: 2

---

## História

> Como usuário logado,  
> Eu quero gerenciar minha conta (ver dados, fazer logout, deletar conta),  
> Para que eu tenha controle sobre minha identidade no app.

---

## Cenários de Aceite

### Cenário 1: Ver informações da conta

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO acesso Configurações > Minha Conta
ENTÃO vejo:
  - Minha foto de perfil do Google
  - Meu nome "Pessoa A"
  - Meu email "pessoa-a@example.com"
  - Meu status de plano (Gratuito/Premium)
  - Data do último login
```

### Cenário 2: Fazer logout

```gherkin
DADO que estou logado
QUANDO toco em "Sair"
E confirmo a ação
ENTÃO sou deslogado do Firebase Auth
E volto ao estado "Anônimo"
E meus dados locais permanecem no dispositivo
E posso continuar usando o app sem login
```

### Cenário 3: Logout mantém dados locais

```gherkin
DADO que tenho 2 pets cadastrados
E estou logado
QUANDO faço logout
ENTÃO continuo vendo meus 2 pets
E posso adicionar novos dados
E os dados não são deletados
```

### Cenário 4: Trocar de conta

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO faço logout
E faço login com "pessoa-b@example.com"
ENTÃO estou autenticado como Pessoa B
E os dados locais permanecem (da Pessoa A)
(associação de dados por conta é feita no sync - fases futuras)
```

### Cenário 5: Deletar conta

```gherkin
DADO que estou logado
QUANDO toco em "Deletar minha conta"
ENTÃO vejo aviso explicando consequências:
  - "Sua conta será removida do Firebase"
  - "Dados locais permanecerão no dispositivo"
  - "Dados na nuvem serão removidos em até 30 dias"
QUANDO confirmo digitando "DELETAR"
ENTÃO minha conta é removida do Firebase
E os dados na nuvem são agendados para purge em 30 dias
E sou deslogado
E volto ao modo anônimo
```

---

## UI/UX

### Tela: Minha Conta

```
┌────────────────────────────────┐
│ ← Minha Conta                  │
├────────────────────────────────┤
│                                │
│         ┌──────────┐           │
│         │          │           │
│         │   📷     │           │
│         │          │           │
│         └──────────┘           │
│         Pessoa A               │
│   pessoa-a@example.com         │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Plano: Gratuito            │ │
│ │ [Fazer upgrade ⭐]         │ │
│ └────────────────────────────┘ │
│                                │
│ 📅 ATIVIDADE                   │
│ ┌────────────────────────────┐ │
│ │ Último login: 18/03/2026   │ │
│ │ Membro desde: 01/01/2026   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │         SAIR               │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    DELETAR MINHA CONTA     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Logout

```
┌────────────────────────────────┐
│           Sair                 │
├────────────────────────────────┤
│                                │
│ Você será desconectado da sua  │
│ conta Google.                  │
│                                │
│ Seus dados locais serão        │
│ mantidos no dispositivo.       │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │     SAIR     │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### Dialog: Deletar Conta

```
┌────────────────────────────────┐
│     ⚠️ Deletar Conta           │
├────────────────────────────────┤
│                                │
│ Esta ação é irreversível!      │
│                                │
│ • Sua conta será removida      │
│ • Dados na nuvem serão removidos│
│   em até 30 dias               │
│ • Dados locais serão mantidos  │
│                                │
│ Digite DELETAR para confirmar: │
│ ┌────────────────────────────┐ │
│ │                            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │   DELETAR    │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### AccountViewModel

```kotlin
class AccountViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val accountInfo: StateFlow<AccountInfo?> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.Authenticated -> AccountInfo(
                    displayName = state.displayName,
                    email = state.email,
                    photoUrl = state.photoUrl,
                    isPremium = state.isPremium,
                    memberSince = userPreferencesRepository.getMemberSince(),
                    lastLoginAt = userPreferencesRepository.getLastLoginAt()
                )
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // 1. Marcar dados na nuvem para purge em 30 dias (LGPD)
            authRepository.deleteAccount()
            // 3. Limpar preferências relacionadas à conta
            userPreferencesRepository.clearAccountData()
        }
    }
}

data class AccountInfo(
    val displayName: String?,
    val email: String,
    val photoUrl: String?,
    val isPremium: Boolean,
    val memberSince: Long?,
    val lastLoginAt: Long?
)
```

### Delete Account no Firebase

```kotlin
suspend fun deleteAccount(): Result<Unit> {
    return try {
        // 1. Deletar conta no Firebase Auth
        //    Cloud Function trata a exclusão em cascata dos dados do usuário
        firebaseAuth.currentUser?.delete()?.await()
            ?: return Result.failure(Exception("Not logged in"))
        
        // Nota: a exclusão efetiva dos dados na nuvem e purge
        // é feita por Cloud Function para Firebase após 30 dias
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## Definition of Done

- [ ] Tela de conta exibe todas informações
- [ ] Foto do Google carrega corretamente
- [ ] Logout funciona e mantém dados locais
- [ ] Dialog de confirmação de logout
- [ ] Deletar conta funciona com confirmação
- [ ] Re-autenticação se necessário para delete
- [ ] Tratamento de erros
- [ ] Testes unitários
