# Especificações do Petit

Documentação funcional, técnica e de produto do Petit. Estes documentos ajudam
pessoas contribuidoras a entender o produto, as decisões já tomadas e o escopo
de cada fase.

## Proveniência

Este acervo foi migrado e adaptado em **2026-07-17** a partir do repositório
privado `getmiw/specs-miw`, no commit `09b4497`. Na origem, o produto se chamava
MiW e era voltado exclusivamente a gatos. A migração atualiza o nome para Petit,
generaliza o domínio para pets de diferentes espécies e reconcilia referências
técnicas com o código presente neste repositório.

Os status importados descrevem o planejamento histórico e não substituem a
verificação no código. Neste acervo:

- **Concluída historicamente**: assim constava na fonte; critérios individuais
  ainda podem estar pendentes ou ter evoluído.
- **Em andamento/próxima**: trabalho planejado ou parcialmente implementado.
- **Em espera (holding)**: hipótese preservada, sem compromisso de entrega.
- **Pendente**: critério sem evidência de conclusão no documento ou no código.

Todos os **46 documentos Markdown** da fonte foram preservados. O antigo agente
de geração de specs foi arquivado, pois suas instruções foram substituídas pelo
workflow atual deste repositório.

## Navegação rápida

- [Requisitos do produto e monetização](REQUIREMENTS.md)
- [Visão geral das specs](00-overview.md)
- [Arquitetura](01-architecture.md)
- [Domínios e entidades](02-domains.md)
- [Pesquisa sobre protocolos de compartilhamento local](local-sharing-protocols.md)
- [Plano técnico da Fase 2](development-plan-phase2.md)
- [Plano de compartilhamento familiar](family-sharing-plan.md)

## Fases

- [Fase 1 — MVP Local](phase-1/README.md) — concluída historicamente
- [Fase 2 — Compartilhamento Familiar](phase-2/README.md) — parcialmente implementada; consulte o estado verificado

## Fases em Holding (pendente demanda de usuários)

- [Fase N — Firebase Auth](holding/firebase-auth/README.md) ⏸️
- [Fase N+1 — Backup Google Drive](holding/backup-gdrive/README.md) ⏸️
- [Fase N+2 — Cloud Sync](holding/cloud-sync/README.md) ⏸️

## Entregas concluídas

- [Registro histórico da Fase 1](completed/phase-1/README.md)

## Arquivo histórico

- [Antigo agente gerador de specs](archive/specs-generator-agent.md) — preservado
  para proveniência; não é uma instrução ativa
