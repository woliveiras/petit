# Plano: Sincronização na rede local

## Estado de partida

Não há implementação de NSD/TCP no projeto. Este plano deve começar somente
após aprovação explícita da spec e pressupõe identidade/chave (0101), grupo
local (0103) e regras determinísticas de conflito (0105).

## Sequência de implementação

1. Definir mensagens versionadas `HELLO`, `HELLO_ACK`, `CHANGESET`, `ACK`, `ERROR` e `CLOSE`.
2. Implementar `NsdServiceManager` com register, discover, resolve, timeout e filtro do próprio serviço.
3. Implementar servidor/cliente TCP com autenticação antes do payload e limites de tamanho/tempo.
4. Criar `LanSyncRepository` para changesets, ACK e integração transacional com o resolver.
5. Integrar lifecycle: iniciar em `ON_START`, parar em `ON_STOP` e limpar listeners/sockets.
6. Criar trabalho periódico único com `NetworkType.CONNECTED`, backoff e intervalo mínimo de 15 minutos.
7. Implementar configuração on/off, ação manual e indicador global acessível.
8. Testar em dois processos e depois em dois dispositivos na mesma rede.

## Fluxo do protocolo

1. Cliente envia `HELLO {protocolVersion, familyGroupKey, deviceId, lastSyncTimestamp}`.
2. Servidor valida versão, chave e membro; erro encerra a sessão.
3. Servidor responde `HELLO_ACK {deviceId, lastSyncTimestamp}`.
4. Ambos trocam `CHANGESET` com entidades posteriores ao timestamp conhecido.
5. Cada lado aplica o lote de forma idempotente e responde `ACK {newSyncTimestamp}`.
6. Ambos encerram a sessão; sem ACK, o lote pode ser reenviado com segurança.

## Bateria e lifecycle

| Contexto | Comportamento |
| --- | --- |
| Foreground | NSD ativo e TCP sob demanda. |
| Background | WorkManager periódico, limitado pelas constraints. |
| Processo encerrado | Nenhum serviço persistente. |
| Parceiro ausente | Discovery termina por timeout e tenta depois com backoff. |

Wi-Fi Direct é proibido para sincronização contínua. Nearby permanece reservado
ao pareamento e à transferência pontual.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Descoberta lenta ou bloqueada pela rede | Timeout, backoff, estado explícito e ação manual. |
| TCP expõe dados na LAN | Autenticar antes do payload e usar canal protegido. |
| Sessões simultâneas duplicam trabalho | Eleger direção por IDs e manter aplicação idempotente. |
| Consumo em background | Trabalho periódico único, constraints e batching. |
| Relógios divergentes | Não considerar timestamp suficiente para desempate; aplicar a spec 0105. |

## Verificação final

1. Executar testes do protocolo com dois processos locais e falhas injetadas.
2. Executar `./gradlew spotlessCheck` e `./gradlew test`.
3. Executar `./gradlew assembleDebug && ./gradlew installDebug`.
4. Em dois dispositivos, validar foreground, background, perda/retorno de Wi-Fi e chave inválida.
5. Confirmar que NSD e sockets são liberados ao sair do app e que Wi-Fi Direct não é mantido.
