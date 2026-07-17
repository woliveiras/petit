# Fase 4 - Backup Automático

> **Status: ON HOLD — proposta histórica, não implementada.** Este documento preserva uma hipótese do antigo roadmap para futura validação; serviços, arquitetura, disponibilidade e monetização descritos aqui não são decisões atuais do Petit.

## Objetivo

Implementar **backup automático diário** dos dados para Google Drive (appDataFolder), agendado para 2h da madrugada, como funcionalidade **gratuita** para todos os usuários logados.

## Escopo

- ✅ Backup automático diário (2h da madrugada) - gratuito
- ✅ Habilitar/desabilitar backup automático
- ✅ Sync apenas em Wi-Fi (configurável)
- ✅ Notificação de backup bem-sucedido (opcional)
- ✅ Retenção automática: rolling window de 30 dias
- ❌ Sync em tempo real entre dispositivos (Fase 5 - premium)
- ❌ Resolução de conflitos multi-device (Fase 5 - premium)

---

## Pré-requisitos

- Fase 3 completa (Backup manual Google Drive)
- WorkManager configurado
- Login Google ativo (backup requer login)

---

## User Stories

| ID | Feature | Prioridade |
|----|---------|------------|
| [US-301](./us-301-auto-backup.md) | Backup Automático | P0 |
| [US-302](./us-302-backup-settings.md) | Configurações de Backup | P0 |
| [US-303](./us-303-backup-triggers.md) | Triggers de Backup | P1 |

---

## Arquitetura

### WorkManager para Backup Diário

```
┌─────────────────────────────────────────────────────────────┐
│                        WorkManager                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PeriodicWorkRequest (24h, agendado para 2h da madrugada)    │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ AutoBackupWorker                                      │     │
│  │                                                       │     │
│  │ - Verifica Login Google                               │     │
│  │ - Verifica Wi-Fi (se configurado)                     │     │
│  │ - Executa backup para Google Drive                   │     │
│  │ - Remove backups > 30 dias (rolling window)          │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Google Drive   │
                    │  (appDataFolder) │
                    └─────────────────┘
```

### Fluxo de Backup Automático

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│  Data Change   │────▶│   Debounce     │────▶│   Check        │
│  (Room write)  │     │   (5 min)      │     │   Conditions   │
└────────────────┘     └────────────────┘     └────────────────┘
                                                      │
                             ┌────────────────────────┼────────┐
                             │                        │        │
                             ▼                        ▼        ▼
                       ┌──────────┐           ┌──────────┐  ┌─────┐
                       │ Premium? │           │ Wi-Fi?   │  │ ... │
                       └──────────┘           └──────────┘  └─────┘
                             │                        │
                             └────────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │ Execute Backup  │
                              └─────────────────┘
```

---

## Estratégia de Backup

### Backup Diário (2h da madrugada)
- Agendado via WorkManager com PeriodicWorkRequest (24h)
- Horário fixo: 2h da madrugada (horário ideal: usuário dormindo, device carregando, Wi-Fi ativo)
- Executa em background via WorkManager
- Respeita configuração de Wi-Fi only (padrão: ativo)
- Somente executa se Login Google estiver ativo

### Retenção
- Rolling window de 30 dias para backups automáticos
- Backups manuais não contam neste limite (máx 10, gerenciado na Fase 3)
- Auto-cleanup ao criar novo backup: remove automáticos com mais de 30 dias
- Após exclusão de conta: purge permanente em 30 dias (LGPD)

---

## Critérios de Aceite Globais

- [ ] Backup automático funciona em background
- [ ] Agendamento às 2h da madrugada funciona corretamente
- [ ] Opção Wi-Fi only é respeitada
- [ ] Battery optimization handling (Doze mode)
- [ ] Login Google é verificado antes do backup
- [ ] Retenção de 30 dias funciona (auto-cleanup)
- [ ] Notificações de backup (opcional)
- [ ] Integração com sistema de backups existente
