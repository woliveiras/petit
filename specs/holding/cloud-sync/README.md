# Fase N+2 - Cloud Sync (Firebase Firestore) ⏸️ ON HOLD

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por sync remoto em tempo real.

## Motivo do Holding

Cloud sync foi adiado porque:
1. A Fase 2 (Compartilhamento Familiar local) atende a demanda atual de compartilhamento
2. Requer Firebase Auth e infraestrutura cloud (custos operacionais)
3. Sync local via NSD na rede Wi-Fi de casa é suficiente para uso doméstico
4. A hipótese de oferecê-lo como recurso premium só poderá ser considerada após validação de demanda e sustentabilidade

## Specs Preservadas (ex-Fase 5)

- [US-N21: Sync em Tempo Real](./us-401-realtime-sync.md)
- [US-N22: Múltiplos Dispositivos](./us-402-multi-device.md)
- [US-N23: Resolução de Conflitos Cloud](./us-403-conflict-resolution.md)
- [US-N24: Sync Offline-First](./us-404-offline-sync.md)
- [US-N25: Compartilhamento Família (cloud)](./us-405-family-sharing.md)

---

## Pré-requisitos

- Fase 2 completa (Firebase Auth)
- Fase 3/4 completas (Backup Google Drive funciona como fallback)
- Coleções configuradas no Firestore com Security Rules
- Usuário premium ativo

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-401](./us-401-realtime-sync.md) | Sincronização em Tempo Real | P0 |
| [US-402](./us-402-multi-device.md) | Múltiplos Dispositivos | P0 |
| [US-403](./us-403-conflict-resolution.md) | Resolução de Conflitos | P0 |
| [US-404](./us-404-offline-sync.md) | Sync Offline-First | P1 |
| [US-405](./us-405-family-sharing.md) | Compartilhamento Família | P2 |

---

## Arquitetura

### Modelo de Sync

```
┌─────────────────────────────────────────────────────────────┐
│                        Device A                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │  (verdade)  │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                  │           │
└─────────────────────────────────────────────────────────────┘
                                                   │
                    ┌──────────────────────────────┴──────────┐
                    │     Firebase Firestore (com Security Rules) │
                    │  ┌─────────────────────────────────────┐ │
                    │  │ pets (userId, Security Rules)          │ │
                    │  │ weight_entries (userId, Security Rules)│ │
                    │  │ vaccination_entries (userId, Rules)    │ │
                    │  │ deworming_entries (userId, Rules)      │ │
                    │  └─────────────────────────────────────┘ │
                    └──────────────────────────────────────────┘
                                                   │
┌──────────────────────────────────────────────────┼───────────┐
│                        Device B                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    Room     │◀───▶│  SyncEngine │◀───▶│  Firestore  │   │
│  │  (verdade)  │     │             │     │  (Firebase) │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Estrutura do Firestore (Firebase)

```
// Coleções no Firestore (cada uma com Security Rules)

// pets/{petId}
{
  "userId": "uid-do-usuario",
  "name": "Luna",
  "birthDate": 1700000000000,
  "sex": "F",
  "microchipNumber": null,
  "passportNumber": null,
  "photoUri": null,
  "notes": null,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000,
  "deletedAt": null
}

// weight_entries/{entryId}, vaccination_entries/{entryId}, deworming_entries/{entryId}
// (estrutura similar com userId + petId + Security Rules)

// sync_metadata/{userId}
{
  "lastSyncAt": 1700000000000,
  "deviceCount": 1
}
```

### Fluxo de Sync

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Local Write   │────▶│   SyncEngine    │────▶│   Firestore    │
│                 │     │                 │     │                 │
│ 1. Write Room   │     │ 2. Mark PENDING │     │ 3. Upsert       │
│                 │     │    SYNC         │     │                 │
│                 │◀────│                 │◀────│ 4. Confirm      │
│                 │     │ 5. Mark SYNCED  │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Firestore     │────▶│   SyncEngine    │────▶│   Local Room    │
│   Realtime      │     │                 │     │                 │
│                 │     │ 1. Receive      │     │ 2. Compare      │
│                 │     │    change       │     │    updatedAt    │
│                 │     │                 │     │ 3. Update if    │
│                 │     │                 │     │    newer        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /pets/{petId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /weight_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /vaccination_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /deworming_entries/{entryId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }

    // Compartilhamento família (futuro)
    match /families/{familyId} {
      allow read: if request.auth != null && request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null && request.auth.uid == resource.data.createdBy;
    }
    match /pets/{petId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == resource.data.userId ||
        (resource.data.familyId != null &&
         request.auth.uid in get(/databases/$(database)/documents/families/$(resource.data.familyId)).data.memberIds)
      );
    }
  }
}
```

---

## Critérios de Aceite Globais

- [ ] Dados sincronizam em tempo real entre dispositivos
- [ ] Princípio offline-first mantido (Room é verdade)
- [ ] Conflitos resolvidos automaticamente (last-write-wins)
- [ ] Sync funciona em background
- [ ] Premium gate aplicado
- [ ] Sync não bloqueia UI
- [ ] Erros de sync tratados graciosamente
- [ ] Indicador visual de status de sync
- [ ] Firestore Security Rules garantem isolamento de dados por usuário
