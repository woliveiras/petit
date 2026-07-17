# US-104: Gate Premium

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

**Prioridade**: P1  
**Épico**: Monetização  
**Fase**: 2

---

## História

> Como usuário do app,  
> Eu quero entender quais recursos são premium,  
> Para que eu possa decidir se vale a pena assinar.

---

## Cenários de Aceite

### Cenário 1: Ver indicador premium em feature bloqueada

```gherkin
DADO que sou usuário gratuito
QUANDO vejo a opção "Sincronização em tempo real" nas configurações
ENTÃO vejo um ícone de ⭐ ou 🔒 indicando que é premium
E ao tocar, vejo informação sobre o plano premium
```

### Cenário 2: Tentar usar feature premium

```gherkin
DADO que sou usuário gratuito
QUANDO tento ativar "Sincronização em tempo real"
ENTÃO vejo um bottom sheet ou dialog explicando:
  - O que a feature faz
  - Que é exclusiva para premium
  - Botão para ver planos
```

### Cenário 3: Listar benefícios premium

```gherkin
DADO que estou no app
QUANDO acesso "Ver planos premium"
ENTÃO vejo lista de benefícios:
  - ☁️ Sincronização em tempo real na nuvem
  - 📱 Múltiplos dispositivos sincronizados automaticamente
  - 👨‍👩‍👧 Compartilhar com família
  - 📄 Exportar PDF (futuro)
```

### Cenário 4: Verificar status premium

```gherkin
DADO que sou usuário premium
QUANDO acesso configurações
ENTÃO vejo "Plano: Premium"
E não vejo indicadores de bloqueio em features premium
E as features premium estão liberadas
```

### Cenário 5: Funcionalidades gratuitas disponíveis sem login

```gherkin
DADO que não estou logado
QUANDO uso o app
ENTÃO posso cadastrar pets, pesar, vacinar, criar lembretes
E posso exportar/importar JSON
MAS não posso fazer backup no Google Drive (requer login)
E não posso usar sync em tempo real (premium)
```

---

## Features por Tier

| Feature | Free (sem login) | Free (com login) | Premium |
|---------|------------------|------------------|---------|
| Cadastro de pets | ✅ | ✅ | ✅ |
| Pesagem + gráfico | ✅ | ✅ | ✅ |
| Vacinação/Vermífugo | ✅ | ✅ | ✅ |
| Lembretes locais | ✅ | ✅ | ✅ |
| Export/Import JSON | ✅ | ✅ | ✅ |
| Login Google | ❌ | ✅ | ✅ |
| Backup manual Google Drive | ❌ | ✅ | ✅ |
| Backup automático Google Drive (2h da madrugada) | ❌ | ✅ | ✅ |
| Restaurar backup do Google Drive | ❌ | ✅ | ✅ |
| Transferência device-to-device (Nearby) | ✅ | ✅ | ✅ |
| Sync em tempo real (Firebase Firestore) | 🔒 | 🔒 | ✅ |
| Múltiplos devices sincronizados | 🔒 | 🔒 | ✅ |
| Compartilhar com família | 🔒 | 🔒 | ✅ |

---

## UI/UX

### Configurações com Gates

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 📦 DADOS                       │
│ ┌────────────────────────────┐ │
│ │ 📤 Exportar dados          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📥 Importar dados          │ │
│ └────────────────────────────┘ │
│                                │
│ ☁️ BACKUP (GOOGLE DRIVE)       │
│ ┌────────────────────────────┐ │
│ │ 💾 Backup manual            │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ⏰ Backup automático (2h)    │ │
│ └────────────────────────────┘ │
│                                │
│ 📶 TRANSFERÊNCIA               │
│ ┌────────────────────────────┐ │
│ │ 🔄 Compartilhar dados       │ │
│ └────────────────────────────┘ │
│                                │
│ 🔒 PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ 🔄 Sync em tempo real      ⭐ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Desbloqueie sync            │ │
│ │ automático multi-device!    │ │
│ │ [Ver planos]               │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Feature Bloqueada

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│         ⭐                     │
│   Recurso Premium              │
│                                │
│   Sincronização em tempo real  │
│   na nuvem                       │
│                                │
│   Seus dados sincronizam         │
│   automaticamente entre todos    │
│   os seus dispositivos.          │
│                                │
│ ┌────────────────────────────┐ │
│ │      VER PLANOS            │ │
│ └────────────────────────────┘ │
│                                │
│        Agora não               │
│                                │
└────────────────────────────────┘
```

### Tela: Planos Premium

```
┌────────────────────────────────┐
│ ← Petit Premium                  │
├────────────────────────────────┤
│                                │
│         ⭐ PREMIUM             │
│                                │
│ Cuide melhor dos seus gatinhos │
│                                │
├────────────────────────────────┤
│                                │
│ ✅ Sincronização em tempo real │
│ ✅ Múltiplos dispositivos       │
│ ✅ Compartilhar com família    │
│ ✅ Suporte prioritário         │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │       MENSAL               │ │
│ │       R$ 9,90/mês          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │       ANUAL                │ │
│ │       R$ 79,90/ano         │ │
│ │       (economize 33%)      │ │
│ └────────────────────────────┘ │
│                                │
│ Cancele quando quiser.         │
│ Seus dados locais são seus.    │
│                                │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### PremiumStatus

```kotlin
enum class PremiumTier {
    FREE,
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY
}

data class PremiumStatus(
    val tier: PremiumTier,
    val expiresAt: Long?,
    val isActive: Boolean
) {
    companion object {
        val FREE = PremiumStatus(PremiumTier.FREE, null, false)
    }
}
```

### PremiumRepository

```kotlin
interface PremiumRepository {
    val premiumStatus: StateFlow<PremiumStatus>
    
    suspend fun checkPremiumStatus(): PremiumStatus
    fun isPremium(): Boolean
}

class PremiumRepositoryImpl(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore  // ou Billing client
) : PremiumRepository {
    
    private val _premiumStatus = MutableStateFlow(PremiumStatus.FREE)
    override val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()
    
    override suspend fun checkPremiumStatus(): PremiumStatus {
        val userId = authRepository.getCurrentUser()?.id ?: return PremiumStatus.FREE
        
        // Verificar no Firebase Firestore ou via Google Play Billing
        val snapshot = firestore.collection("users")
            .document(userId).get().await()
        val userProfile = snapshot.toObject(UserProfile::class.java)
        
        val premiumUntil = userProfile?.premiumUntil ?: 0
        
        val status = when {
            premiumUntil > System.currentTimeMillis() -> PremiumStatus(
                tier = PremiumTier.PREMIUM_MONTHLY,  // ou verificar qual plano
                expiresAt = premiumUntil,
                isActive = true
            )
            else -> PremiumStatus.FREE
        }
        
        _premiumStatus.value = status
        return status
    }
    
    override fun isPremium(): Boolean = _premiumStatus.value.isActive
}
```

### Feature Gate Composable

```kotlin
@Composable
fun PremiumFeatureGate(
    feature: PremiumFeature,
    premiumStatus: PremiumStatus,
    onShowPremiumInfo: () -> Unit,
    content: @Composable () -> Unit
) {
    if (premiumStatus.isActive) {
        content()
    } else {
        Box(
            modifier = Modifier
                .clickable { onShowPremiumInfo() }
                .alpha(0.6f)
        ) {
            content()
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Premium",
                modifier = Modifier.align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

enum class PremiumFeature {
    CLOUD_BACKUP,
    CLOUD_SYNC,
    FAMILY_SHARING,
    PDF_EXPORT
}
```

### Verificação antes de ação

```kotlin
class BackupUseCase(
    private val premiumRepository: PremiumRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        if (!premiumRepository.isPremium()) {
            return Result.failure(PremiumRequiredException("Backup requer plano premium"))
        }
        
        // Executar backup...
        return Result.success(Unit)
    }
}

class PremiumRequiredException(message: String) : Exception(message)
```

---

## Nota sobre Billing

A implementação com Google Play Billing (compra de assinatura) era uma hipótese posterior do roadmap antigo. Caso essa hipótese seja validada, o escopo proposto seria:

1. ✅ Exibir gates visuais
2. ✅ Verificar status premium via Firebase Firestore
3. ✅ Bloquear features premium via código
4. ⏳ Integração com Google Play Billing (implementação futura)

O status premium pode ser definido manualmente no Firebase Console para testes.

---

## Definition of Done

- [ ] Indicador ⭐ em features premium na UI
- [ ] Bottom sheet de feature bloqueada
- [ ] Tela de planos premium
- [ ] PremiumRepository verifica status
- [ ] Feature gates bloqueiam acesso
- [ ] Gate para usuários não logados
- [ ] Testes unitários
