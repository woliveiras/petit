---
spec: "0104"
title: Sincronização na rede local
family: local-sharing
phase: 2
status: Draft
owner: ""
depends_on: ["0101", "0103"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Sincronização na rede local

## Contexto e motivação

Dispositivos do mesmo grupo devem trocar mudanças automaticamente quando estão
na mesma rede Wi-Fi, sem servidor remoto e sem manter uma conexão de alto
consumo. O modo planejado usa NSD para descoberta e TCP sobre o Wi-Fi de
infraestrutura para mudanças incrementais.

## Estado atual

Funcionalidade planejada. Não foram encontrados NSD, servidor TCP,
`LanSyncRepository`, `LanSyncWorker` ou indicador global de sincronização no
código atual.

## Requisitos

### Funcionais

- [ ] Registrar e descobrir serviços `_petit._tcp` enquanto o app está em foreground.
- [ ] Autenticar cada sessão com a chave do grupo e a identidade do dispositivo.
- [ ] Trocar changesets bidirecionais desde o último timestamp confirmado.
- [ ] Aplicar as regras da spec 0105 e registrar o resultado da sincronização.
- [ ] Sincronizar mudanças acumuladas ao reconectar à rede local.
- [ ] Agendar tentativas periódicas em background com WorkManager.
- [ ] Exibir estados sincronizando, sincronizado, parceiro indisponível e erro.
- [ ] Permitir desativar sincronização automática e forçar uma tentativa manual.

### Não funcionais

- [ ] Bateria: nunca usar Wi-Fi Direct como conexão persistente.
- [ ] Lifecycle: encerrar anúncio e discovery quando o app sair de foreground.
- [ ] Segurança: rejeitar chave inválida antes de trocar dados e proteger o TCP em trânsito.
- [ ] Confiabilidade: aplicar changesets em lotes, com confirmação e repetição idempotente.
- [ ] Performance: interromper discovery sem parceiro após timeout e usar backoff.
- [ ] Acessibilidade e i18n: estados não dependem apenas de ícone/cor e são localizados.

## Test strategy

Testes unitários cobrem protocolo, autenticação, batching e estados. Testes de
integração usam dois processos para NSD/TCP, Room e WorkManager. A aceitação
final exige dois dispositivos na mesma Wi-Fi, incluindo perda e retorno da
rede. Consulte a [pesquisa de protocolos](../../docs/local-sharing-protocols.md).

## Critérios de aceitação

- [ ] Dados dois membros na mesma Wi-Fi, quando o app entra em foreground, então ambos anunciam/descobrem `_petit._tcp` e iniciam uma sessão autenticada.
- [ ] Dadas mudanças nos dois dispositivos, quando sincronizam, então cada lado envia somente seu changeset e ambos convergem.
- [ ] Dada uma chave de grupo inválida, quando o cliente envia `HELLO`, então o servidor responde com erro e encerra antes de dados de saúde.
- [ ] Dadas mudanças feitas offline, quando a Wi-Fi retorna, então elas são sincronizadas automaticamente sem duplicação.
- [ ] Dado o app em background, quando as constraints são atendidas, então o WorkManager tenta sincronizar no intervalo permitido pelo Android.
- [ ] Dada a sincronização automática desativada, quando o app abre, então não anuncia nem descobre serviços e ainda permite transferência manual.
- [ ] Dada qualquer mudança de estado, quando a UI é observada, então informa a situação sem depender apenas de cor.

## Casos extremos

- Duas instâncias iniciam a conexão simultaneamente.
- NSD retorna o próprio serviço ou múltiplos membros.
- Rede muda durante o handshake ou changeset.
- ACK se perde depois de uma aplicação bem-sucedida.
- Relógios dos dispositivos divergem.
- Chave é revogada durante uma sessão.

## Decisões

| Decisão | Escolha | Justificativa |
| --- | --- | --- |
| Descoberta | Android NSD / DNS-SD `_petit._tcp` | Funciona na rede local sem Google Play Services. |
| Transporte contínuo | TCP sobre Wi-Fi de infraestrutura | Usa o rádio já ativo e evita o custo de um grupo Wi-Fi Direct. |
| Background | Trabalho periódico único, mínimo de 15 minutos | Respeita as restrições e otimizações do Android. |
| Ciclo de vida | NSD em foreground; tentativa limitada em background | Reduz bateria e recursos mantidos pelo processo. |
| Unidade de envio | Changeset em lote desde o último ACK | Evita sincronizar cada escrita e permite repetição. |
| Segurança | Handshake autenticado e canal protegido | A rede local, isoladamente, não é uma fronteira de confiança. |

## Fora de escopo

- Pareamento inicial e troca da chave.
- Wi-Fi Direct persistente.
- Sincronização pela internet ou cloud.
- Garantia de execução contínua quando o Android encerra o processo.
