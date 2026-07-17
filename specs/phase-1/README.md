# Fase 1 - MVP Local

> **Status histórico da fonte**: concluída em março de 2026. Os checkboxes nos
> documentos detalhados prevalecem para critérios individuais.

## Objetivo

Entregar um app funcional **sem backend obrigatório**, com todas as features core funcionando 100% offline.

## Escopo

- ✅ Cadastro de pets
- ✅ Registro de peso + gráfico
- ✅ Registro de vacinação
- ✅ Registro de desparasitação
- ✅ Lembretes locais
- ✅ Export JSON
- ✅ Import JSON
- ❌ Login (Fase 2)
- ❌ Sync na nuvem (Fase 4/5)
- ❌ Backup automático (Fase 3)

---

## User Stories

### Índice

| ID | Feature | Prioridade | Status |
|----|---------|------------|--------|
| [US-001](../completed/phase-1/us-001-pet-management.md) | Gerenciamento de Pets | P0 | Histórico |
| [US-002](../completed/phase-1/us-002-weight-tracking.md) | Acompanhamento de Peso | P0 | Histórico |
| [US-003](../completed/phase-1/us-003-vaccination.md) | Registro de Vacinação | P0 | Histórico |
| [US-004](../completed/phase-1/us-004-deworming.md) | Registro de Desparasitação | P0 | Histórico |
| [US-005](../completed/phase-1/us-005-reminders.md) | Lembretes Locais | P1 | Histórico |
| [US-006](../completed/phase-1/us-006-export-import.md) | Export/Import JSON | P1 | Histórico |
| [US-007](../completed/phase-1/us-007-home-dashboard.md) | Dashboard Home | P0 | Histórico |

---

## Definição de Prioridades

- **P0**: Crítico para MVP - deve estar pronto
- **P1**: Importante - ideal para MVP
- **P2**: Nice to have - pode ficar para depois

---

## Critérios de Aceite Globais da Fase 1

- [x] App funciona 100% offline
- [x] Dados persistem entre sessões (Room database)
- [x] Não há dependência de internet para nenhuma operação
- [ ] UI responsiva e sem travamentos
- [ ] Navegação intuitiva entre screens
- [ ] Tema Material 3 consistente
