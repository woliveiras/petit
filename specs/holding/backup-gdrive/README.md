# Fase N+1 - Backup Google Drive ⏸️ ON HOLD

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

> **Status**: Em holding — poderá ser reavaliada se houver demanda validada por backup na nuvem.

## Motivo do Holding

Backup no Google Drive foi adiado porque:
1. Export/Import JSON já atende como backup manual
2. A demanda imediata é compartilhamento local entre dispositivos da casa
3. Requer Firebase Auth (também em holding)
4. Poderá ser reavaliado se houver demanda validada por backup automático na nuvem

## Specs Preservadas

### Backup Manual (ex-Fase 3)
- [US-N11: Backup Manual](./us-201-manual-backup.md)
- [US-N12: Restaurar Backup](./us-202-restore-backup.md)
- [US-N13: Gerenciar Backups](./us-203-manage-backups.md)

### Backup Automático (ex-Fase 4)
- [README original do auto-backup](./README-auto-backup.md)
- [US-N14a: Backup Automático](./us-301-auto-backup.md)
- [US-N14b: Configurações de Backup](./us-302-backup-settings.md)
- [US-N14c: Triggers de Backup](./us-303-backup-triggers.md)

### Transferência Device-to-Device (referência histórica)
- [US-204 original](./us-204-device-transfer.md) — Esta proposta serviu como referência para o compartilhamento familiar, mas permanece não implementada neste formato

---

## Pré-requisitos

- Fase 2 completa (Firebase Auth)
- Google Cloud Console com Drive API habilitada
- OAuth configurado para Drive API (scope: `https://www.googleapis.com/auth/drive.appdata`)

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-201](./us-201-manual-backup.md) | Backup Manual | P0 |
| [US-202](./us-202-restore-backup.md) | Restaurar Backup | P0 |
| [US-203](./us-203-manage-backups.md) | Gerenciar Backups | P1 |
| [US-204](./us-204-device-transfer.md) | Transferência Device-to-Device | P1 |

---

## Arquitetura

### Google Drive API — appDataFolder

Backups são salvos no **appDataFolder** do Google Drive:
- Pasta especial oculta do usuário (não aparece no Drive UI)
- Acessível apenas pelo app que criou os dados
- Automaticamente isolada por conta Google
- Sem consumir quota de armazenamento do usuário na maioria dos casos

### Fluxo de Backup

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Room     │────▶│ ExportBundle│────▶│   Google    │
│  Database   │     │    JSON     │     │    Drive     │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                                              ▼
                                        appDataFolder/
                                        └── petit_backup_2026-03-15.json
```

### Fluxo de Restore

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Google   │────▶│ ExportBundle│────▶│    Room     │
│    Drive    │     │    JSON     │     │  Database   │
└─────────────┘     └─────────────┘     └─────────────┘
```

---

## Configuração Google Drive API

### 1. Google Cloud Console

1. Habilitar Google Drive API no projeto
2. Configurar OAuth consent screen
3. Adicionar scope: `https://www.googleapis.com/auth/drive.appdata`
4. Baixar `google-services.json` (se ainda não tiver)

### 2. Dependências

```kotlin
dependencies {
    // Google Drive API
    implementation("com.google.android.gms:play-services-drive:VERSION")
    implementation("com.google.api-client:google-api-client-android:VERSION")
    implementation("com.google.apis:google-api-services-drive:VERSION")
}
```

### 3. Permissões no Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Estrutura de Arquivos no Google Drive

```
appDataFolder/
└── {userId}/
    ├── petit_backup_2026-03-18T10-30-00Z.json    (mais recente)
    ├── petit_backup_2026-03-15T14-20-00Z.json
    ├── petit_backup_2026-03-10T09-15-00Z.json
    └── metadata.json                           (índice de backups)
```

### Metadata File

```json
{
  "backups": [
    {
      "fileId": "abc123",
      "fileName": "petit_backup_2026-03-18T10:30:00Z.json",
      "createdAt": "2026-03-18T10:30:00Z",
      "sizeBytes": 15420,
      "petCount": 2,
      "appVersion": "1.0.0"
    }
  ],
  "lastBackupAt": "2026-03-18T10:30:00Z"
}
```

---

## Política de Retenção de Backups

| Tipo | Retenção | Regra |
|------|----------|-------|
| Backups manuais (Fase 3) | Até o usuário deletar (máx 10) | Usuário controla; ao atingir 10, o mais antigo é removido automaticamente |
| Backups automáticos (Fase 4) | Últimos 30 dias (rolling window) | Cleanup automático mantém custo previsível |
| Cancelamento de premium | 90 dias após expiração | Grace period para re-assinar sem perder dados |
| Exclusão de conta | 30 dias, depois purge permanente | Atende LGPD (direito ao esquecimento) com margem para recuperação |

### LGPD (Lei 13.709/2018)

- **Princípio da necessidade**: guardar apenas pelo tempo necessário à finalidade
- **Direito à eliminação**: o usuário pode pedir exclusão a qualquer momento
- Prazos de retenção devem constar nos Termos de Uso e Política de Privacidade

---

## Critérios de Aceite Globais

- [ ] Usuário premium pode fazer backup manual
- [ ] Usuário premium pode restaurar de backup
- [ ] Lista de backups mostra data e tamanho
- [ ] Pode deletar backups antigos
- [ ] Funciona apenas com conexão de internet
- [ ] Feedback claro durante operações (progress)
- [ ] Tratamento de erros de rede/quota
- [ ] RLS garante isolamento por usuário
- [ ] Máximo de 10 backups manuais por usuário (auto-cleanup do mais antigo)
- [ ] Backups mantidos por 90 dias após cancelamento de premium
- [ ] Backups purgados em até 30 dias após exclusão de conta
