---
spec: "0003"
title: Registro de vacinação
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Registro de vacinação

## Contexto e motivação

O tutor precisa manter o calendário de vacinação e a rastreabilidade das doses do pet.

## Requisitos funcionais

- Registrar tipo de vacina filtrado por espécie, aplicação, próxima dose e dados opcionais de veterinário, clínica, lote e observação.
- Exigir nome customizado para o tipo `OTHER`.
- Calcular `OK` sem próxima dose ou acima de 30 dias, `SCHEDULED` entre 0 e 30 dias e `OVERDUE` quando vencida.
- Agrupar o histórico por tipo e mostrar estado visual.
- Permitir edição e soft delete.

O catálogo inclui vacinas felinas (`V3`, `V4`, `V5`, `FELV`, `FIV`), caninas (`DHPP`, `BORDETELLA`, `LEPTOSPIROSIS`, `LEISHMANIA`, `GRIPE_CANINA`), de coelhos (`RHDV`, `MYXOMATOSIS`), de aves (`POLYOMAVIRUS`) e as opções gerais `RABIES` e `OTHER`.

## Critérios de aceite

- Dada uma vacinação válida, quando salva, então aparece no histórico e seu status é calculado.
- Dada próxima dose em cinco dias, então o status é `SCHEDULED`; dada data vencida, é `OVERDUE`.
- Dada vacina sem próxima dose, então o status é `OK` e nenhuma próxima dose é exibida.
- Dadas múltiplas doses do mesmo tipo, então o resumo identifica a dose mais recente e o histórico preserva todas as aplicações.
- Dados opcionais de rastreabilidade salvos ficam disponíveis no detalhe.

## Estratégia de testes

Unitários cobrem status e validação; integração cobre persistência, agrupamento e soft delete; UI cobre formulário, filtros por espécie e estados visuais.

## Casos de borda

- Próxima dose deve ser posterior à aplicação; aplicação não pode ser futura.
- Raiva e `OTHER` são gerais; demais tipos respeitam a espécie do pet.

## Limitação conhecida

O histórico completo implementado é agrupado por mês; a apresentação agrupada por tipo descrita originalmente ainda não foi verificada.
