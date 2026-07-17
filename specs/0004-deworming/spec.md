---
spec: "0004"
title: Registro de desparasitação
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Registro de desparasitação

## Contexto e motivação

O tutor precisa registrar vermífugos e antiparasitários externos para manter a proteção do pet atualizada.

## Requisitos funcionais

- Registrar tipo `INTERNAL`, `EXTERNAL` ou `BOTH`, medicamento, aplicação, próxima dose e observação.
- Exigir medicamento, impedir aplicação futura e exigir próxima dose posterior à aplicação.
- Calcular `OK`, `SCHEDULED` ou `OVERDUE` para cada registro.
- Listar histórico por data, com indicadores visuais, edição e soft delete.
- Contabilizar `BOTH` nas categorias interna e externa quando a visão por categoria estiver disponível.

## Critérios de aceite

- Dados registros interno, externo ou combinado válidos, quando salva, então o tipo correto é persistido e o status é calculado.
- Dada próxima dose em cinco dias, então o indicador é `SCHEDULED`; dada dose vencida, é `OVERDUE`.
- Dados registros de diferentes tipos, quando abre o histórico, então eles aparecem em ordem decrescente.
- Dado tipo `BOTH`, quando a saúde por categoria é calculada, então ele conta como interno e externo.
- Dado um registro, quando edita ou exclui, então a tela reflete a mudança e a exclusão é lógica.

## Estratégia de testes

Unitários cobrem status e categorias; integração cobre Room, ordenação e soft delete; UI cobre formulário e indicadores.

## Limitação conhecida

A API atual calcula status por registro; a separação visual e o cálculo agregado por categoria permanecem pendentes.
