---
spec: "0002"
title: Acompanhamento de peso
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: ["0001"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Acompanhamento de peso

## Contexto e motivação

O tutor precisa registrar o peso ao longo do tempo para identificar variações na saúde do pet.

## Requisitos funcionais

- Registrar data, peso em kg ou g e observação opcional.
- Normalizar a persistência em gramas e manter no máximo uma pesagem ativa por pet e dia.
- Listar o histórico por data decrescente e exibir gráfico de barras da evolução.
- Permitir editar e excluir logicamente uma pesagem.
- Rejeitar peso menor ou igual a zero, superior a 50 kg e data futura.

## Critérios de aceite

- Dado `3,5 kg` ou `350 g`, quando salva, então persiste respectivamente 3500 ou 350 gramas.
- Dada uma pesagem no mesmo pet e data, quando outra é salva, então a anterior é substituída.
- Dadas várias pesagens, quando abre a tela, então vê histórico decrescente e gráfico com datas e kg.
- Dado valor inválido ou data futura, quando salva, então vê erro e nada é persistido.
- Dada uma pesagem, quando edita ou exclui, então lista e gráfico refletem a mudança e a exclusão é lógica.

## Estratégia de testes

Testes unitários cobrem conversão e validação; integração cobre unicidade por dia, Room, ordenação e soft delete; UI cobre formulário e gráfico.

## Fora de escopo

- Diagnóstico clínico ou recomendação automática de faixa ideal.
