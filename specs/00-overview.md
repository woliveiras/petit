# Petit

## Visão Geral do Produto

**Propósito**: ajudar tutores a não esquecer cuidados básicos e a manter o
histórico de saúde de seus pets em um único lugar, fácil de encontrar e de
compartilhar com profissionais veterinários.

**Princípio Central**: Local-first (não cloud-first com cache)

**Plataforma**: Android nativo (Kotlin + Jetpack Compose)

---

## Modelo de Negócio

### Freemium

| Feature | Gratuito | Premium |
|---------|----------|---------|
| Cadastro de pets | ✅ | ✅ |
| Pesagem + gráfico | ✅ | ✅ |
| Vacinação | ✅ | ✅ |
| Desparasitação | ✅ | ✅ |
| Lembretes locais | ✅ | ✅ |
| Export/Import JSON | ✅ | ✅ |
| Compartilhamento local (grupo familiar) | ✅ | ✅ |
| Sync contínuo na rede local | ✅ | ✅ |
| Login Google | ❌ (futuro) | ❌ (futuro) |
| Backup Google Drive | ❌ (futuro) | ❌ (futuro) |
| Sync em tempo real na nuvem | ❌ (futuro) | ✅ (futuro) |

---

## Stack Técnica

| Componente | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| Banco Local | Room |
| Preferências | DataStore |
| Background Jobs | WorkManager |
| Compartilhamento Local | Nearby Connections API + NSD (mDNS) |
| Transferência Local | Nearby Connections API (P2P_POINT_TO_POINT) |
| Sync na Rede Local | NSD (Network Service Discovery) + TCP Sockets |
| Analytics | ❌ (futuro — Firebase Analytics) |
| Crash Reporting | ❌ (futuro — Firebase Crashlytics) |

> **Firebase e serviços cloud estão em holding.** Serão reavaliados quando houver demanda de usuários por backup na nuvem, sync multi-device remoto ou login Google.

---

## Roadmap de Fases

| Fase | Escopo | Objetivo | Status |
|------|--------|----------|--------|
| 1 | MVP Local | App funcional sem backend, export/import manual | Concluída historicamente na fonte |
| 2 | Compartilhamento Familiar | Compartilhamento local entre dispositivos da casa | Parcial — consulte o estado verificado da fase |
| N | 🔒 Firebase Auth | Login Google (opcional) | ⏸️ On Hold |
| N+1 | 🔒 Backup Google Drive | Backup manual/automático para Google Drive | ⏸️ On Hold |
| N+2 | 🔒 Cloud Sync | Sync em tempo real via Firestore (premium) | ⏸️ On Hold |

> **Nota:** As fases N, N+1 e N+2 foram movidas para holding. Firebase e serviços cloud serão implementados quando houver demanda real dos usuários.

---

## Índice de Specs

### Documentos Base
1. [Arquitetura](./01-architecture.md) - Princípios, camadas e estrutura de pacotes
2. [Domínios e Entidades](./02-domains.md) - Modelos de dados e relacionamentos

### Fases de Desenvolvimento

#### [Fase 1 - MVP Local](./phase-1/README.md) — concluída historicamente
App funcional 100% offline, sem backend.
- US-001: Gerenciamento de pets
- US-002: Acompanhamento de Peso
- US-003: Registro de Vacinação
- US-004: Registro de Desparasitação
- US-005: Lembretes Locais
- US-006: Export/Import JSON
- US-007: Dashboard Home

#### [Fase 2 - Compartilhamento Familiar](./phase-2/README.md) — parcialmente implementada
Compartilhamento local entre dispositivos da casa, sem servidor remoto.
- US-101: Pareamento de Dispositivos
- US-102: Transferência One-Shot
- US-103: Grupo Familiar Local
- US-104: Sync Contínuo na Rede Local
- US-105: Resolução de Conflitos Local

### Fases em Holding (pendente demanda de usuários)

#### [Fase N - Firebase Auth](./holding/firebase-auth/README.md)
Login Google (opcional, ativado quando usuário tenta backup para Google Drive).
- US-N01: Login com Google
- US-N02: Gerenciamento de Conta
- US-N03: Vinculação de Dados
- US-N04: Gate Premium

#### [Fase N+1 - Backup Google Drive](./holding/backup-gdrive/README.md)
Backup manual e automático no Google Drive.
- US-N11: Backup Manual
- US-N12: Restaurar Backup
- US-N13: Gerenciar Backups
- US-N14: Backup Automático

#### [Fase N+2 - Cloud Sync](./holding/cloud-sync/README.md)
Sincronização em tempo real via Firebase Firestore (premium).
- US-N21: Sync em Tempo Real
- US-N22: Múltiplos Dispositivos
- US-N23: Resolução de Conflitos Cloud
- US-N24: Sync Offline-First
- US-N25: Compartilhamento Família (cloud)
