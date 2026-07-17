---
spec: "0001"
title: Gerenciamento de pets
family: pet-care
phase: 1
status: Implemented
owner: ""
depends_on: []
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Gerenciamento de pets

## Contexto e motivação

O tutor precisa cadastrar e manter os dados de cada pet para acompanhar seu histórico de saúde individualmente.

## Requisitos funcionais

- Cadastrar, listar, visualizar, editar e excluir logicamente um pet.
- Exigir nome (1–50 caracteres) e tipo; aceitar foto, nascimento não futuro, sexo, raça, cor, microchip, passaporte e observações.
- Suportar gato, cão, coelho, ave, hamster e outro.
- Exibir no perfil atalhos para vacinas, peso, desparasitação e compartilhamento.
- Persistir os dados localmente e ocultar registros com `deletedAt` preenchido.

## Critérios de aceite

- Dado um nome e tipo válidos, quando o tutor salva, então o pet aparece na lista após reiniciar o app.
- Dado o nome vazio, quando tenta salvar, então vê “Nome é obrigatório” e nada é persistido.
- Dado um pet existente, quando edita e salva, então os campos e `updatedAt` são atualizados.
- Dado um pet existente, quando confirma a exclusão, então ele some das consultas ativas e permanece com `deletedAt` preenchido.
- Dado o seletor ou a câmera, quando o tutor fornece uma imagem JPG ou PNG de até 5 MB e salva, então ela fica disponível no perfil.

## Estratégia de testes

Testes unitários cobrem validações e mapeamentos; testes de integração cobrem Room, CRUD, persistência e soft delete; testes de UI cobrem formulário e navegação.

## Casos de borda

- Rejeitar nascimento futuro e limites excedidos dos campos.
- Preservar pets excluídos no banco, sem exibi-los nas consultas ativas.
- Tratar falta ou perda de acesso à URI de foto sem corromper os demais dados.

## Limitação conhecida

O app seleciona imagens da galeria, mas captura pela câmera e validação explícita do limite de 5 MB ainda não foram verificadas na implementação.

## Fora de escopo

- Sincronização entre dispositivos e compartilhamento do perfil.
