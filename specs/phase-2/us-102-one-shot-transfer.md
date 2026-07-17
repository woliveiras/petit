# US-102: Transferência One-Shot

> **Estado no Petit (2026-07-17): parcialmente implementado.** Existem
> `NearbyTransferRepository`, `SendDataUseCase`, `MergeDataUseCase` e a UI de
> transferência. A opção atual `REPLACE` sobrescreve entidades coincidentes,
> mas não apaga registros locais ausentes no bundle como exige o cenário 3.
> O fluxo completo, o progresso de envio e os cenários de erro ainda precisam
> de testes em dois dispositivos. Origem:
> `getmiw/specs-miw@09b4497`.

**Prioridade**: P0
**Épico**: Compartilhamento Familiar
**Fase**: 2

---

## História

> Como usuário do app,
> Eu quero enviar todos os dados dos meus pets para o dispositivo de outra pessoa cuidadora,
> Para que ambas tenham acesso às mesmas informações.

---

## Cenários de Aceite

### Cenário 1: Enviar dados

```gherkin
DADO que estou pareado com outro dispositivo
E tenho dados locais (pets, pesos, vacinas, etc.)
QUANDO acesso "Compartilhar com Família"
E toco em "Enviar Dados"
ENTÃO meus dados são serializados em ExportBundle JSON
E enviados via Nearby Connections
E vejo progresso "Enviando... X%"
E ao final vejo "Dados enviados com sucesso!"
```

### Cenário 2: Receber e mesclar dados

```gherkin
DADO que recebi dados do outro dispositivo
E tenho dados locais existentes
QUANDO escolho "Mesclar com Locais"
ENTÃO dados são combinados por UUID
E duplicatas são resolvidas por updatedAt (mais recente vence)
E vejo resumo: "2 pets atualizados, 5 pesagens adicionadas"
```

### Cenário 3: Receber e substituir dados

```gherkin
DADO que recebi dados do outro dispositivo
QUANDO escolho "Substituir Dados Locais"
ENTÃO vejo confirmação "Seus dados locais serão apagados. Continuar?"
QUANDO confirmo
ENTÃO todos os dados locais são deletados
E dados recebidos são importados
E vejo "Dados restaurados com sucesso"
```

### Cenário 4: Transferência sem internet

```gherkin
DADO que ambos devices estão sem internet
MAS estão com Bluetooth ou Wi-Fi ativo
QUANDO inicio transferência
ENTÃO funciona normalmente via Nearby Connections
```

### Cenário 5: Erro durante transferência

```gherkin
DADO que transferência está em andamento
QUANDO ocorre erro (devices se afastam, Bluetooth desliga)
ENTÃO transferência é cancelada
E dados parciais são descartados
E vejo "Transferência interrompida. Tente novamente."
```

---

## UI/UX

### Tela: Enviar Dados

```
┌────────────────────────────────┐
│ ← Enviar Dados                  │
├────────────────────────────────┤
│                                │
│        [dispositivo] → [dispositivo]                │
│                                │
│  Dados a enviar:               │
│  ┌────────────────────────────┐│
│  │ 2 pets                    ││
│  │ 25 pesagens                ││
│  │ 8 vacinas                  ││
│  │ 6 vermífugos               ││
│  │ 12 tarefas                 ││
│  └────────────────────────────┘│
│                                │
│ ┌────────────────────────────┐ │
│ │      ENVIAR AGORA          │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Tela: Progresso

```
┌────────────────────────────────┐
│ Enviando...                     │
├────────────────────────────────┤
│                                │
│         ████████░░             │
│            80%                 │
│                                │
│  Enviando dados...             │
│  2 pets • 25 registros        │
│                                │
│  Não feche o app               │
│                                │
└────────────────────────────────┘
```

### Dialog: Escolher Ação (receptor)

```
┌────────────────────────────────┐
│         Sim                     │
│                                │
│   Dados recebidos!             │
│   2 pets • 51 registros       │
│                                │
│ ┌────────────────────────────┐ │
│ │    MESCLAR COM LOCAIS      │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    SUBSTITUIR LOCAIS       │ │
│ └────────────────────────────┘ │
│                                │
│       Cancelar                 │
└────────────────────────────────┘
```

---

## Requisitos Técnicos

### NearbyTransferRepository

```kotlin
interface NearbyTransferRepository {
    val transferState: Flow<TransferState>
    suspend fun sendData(endpointId: String, bundle: ExportBundle)
    val connectedPeerName: String?
    val connectedPeerId: String?
    fun disconnect()
}

sealed interface TransferState {
    data object Idle : TransferState
    data class Sending(val bytesTransferred: Long, val totalBytes: Long) : TransferState
    data class Receiving(val bytesTransferred: Long, val totalBytes: Long) : TransferState
    data class Complete(val bundle: ExportBundle) : TransferState
    data class Error(val message: String) : TransferState
}
```

### Estratégia de Merge

```kotlin
class MergeStrategy {
    /**
     * Merge por UUID + updatedAt (last-write-wins)
     * - Se entidade não existe localmente → inserir
     * - Se entidade existe e remota é mais recente → atualizar
     * - Se entidade existe e local é mais recente → manter local
     * - Se deletedAt != null → propagar soft delete
     */
    suspend fun merge(local: ExportBundle, remote: ExportBundle): MergeResult
}

data class MergeResult(
    val petsAdded: Int,
    val petsUpdated: Int,
    val weightsAdded: Int,
    val weightsUpdated: Int,
    val vaccinationsAdded: Int,
    val vaccinationsUpdated: Int,
    val dewormingsAdded: Int,
    val dewormingsUpdated: Int,
    val tasksAdded: Int,
    val tasksUpdated: Int,
    val conflictsResolved: Int
)
```

---

## Critérios de Aceite

- [ ] Transmissor envia todos os dados como ExportBundle JSON
- [ ] Receptor recebe e pode escolher "Mesclar" ou "Substituir"
- [ ] Merge resolve duplicatas por UUID + updatedAt
- [ ] Soft deletes são propagados corretamente
- [ ] Progresso é mostrado durante transferência
- [ ] Transferência funciona sem internet
- [ ] Erros são tratados e dados parciais descartados
- [ ] Resumo mostra quantidades de entidades afetadas
