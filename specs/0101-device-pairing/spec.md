---
spec: "0101"
title: Pareamento de dispositivos
family: local-sharing
phase: 2
status: In Progress
owner: ""
depends_on: []
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Pareamento de dispositivos

## Contexto e motivação

Pessoas cuidadoras precisam autorizar dois dispositivos próximos a compartilhar
os dados de saúde dos mesmos pets sem conta, servidor remoto ou internet. O
Petit usa Nearby Connections para descoberta e transporte e persiste uma chave
comum do grupo familiar depois do pareamento.

## Estado atual

Nearby Connections, permissões, telas, repositórios e persistência já existem.
A interface atual descobre dispositivos, mas ainda não permite que o receptor
digite e valide o código de quatro dígitos previsto pelo fluxo. O pareamento
completo também não foi validado em dois dispositivos reais.

## Requisitos

### Funcionais

- [x] Iniciar e cancelar advertising ou discovery pelo fluxo de pareamento.
- [x] Solicitar as permissões de Bluetooth e Wi-Fi exigidas pela versão do Android.
- [x] Persistir a chave e a identidade local do grupo familiar.
- [ ] Exibir ao transmissor um código de pareamento de quatro dígitos.
- [ ] Permitir que o receptor informe o código e rejeitar códigos inválidos.
- [ ] Trocar a mesma chave de grupo entre os dois dispositivos após validação.
- [ ] Confirmar o pareamento sem acesso à internet.
- [ ] Desfazer o pareamento sem apagar os dados locais.

### Não funcionais

- [ ] Segurança: aceitar a conexão somente após os dois lados validarem o mesmo código.
- [x] Privacidade: não enviar dados a um servidor remoto durante o pareamento.
- [ ] Acessibilidade: fornecer rótulos e alvos de toque de pelo menos 48 dp.
- [ ] Internacionalização: manter textos visíveis em `strings.xml` para pt-BR, en e es.
- [ ] Compatibilidade: apresentar fallback claro quando Google Play Services não estiver disponível.

## Test strategy

Testes unitários cobrem geração/validação do código e transições de estado.
Testes de integração cobrem DataStore, permissões e callbacks do Nearby. A
aceitação final exige dois dispositivos Android reais, com o cenário repetido
sem internet. Consulte a [pesquisa de protocolos](../../docs/local-sharing-protocols.md).

## Critérios de aceitação

- [ ] Dado um transmissor pronto, quando inicia o pareamento, então vê um código de quatro dígitos e o advertising fica ativo.
- [ ] Dado um receptor próximo, quando informa o código correto, então os dispositivos estabelecem conexão e persistem a mesma chave do grupo.
- [ ] Dado um código incorreto, quando o receptor tenta conectar, então a conexão é rejeitada e uma nova tentativa é permitida.
- [ ] Dado um fluxo em espera, quando a pessoa cancela, então advertising e discovery são interrompidos e nenhum grupo incompleto permanece.
- [ ] Dados Bluetooth ou Wi-Fi ativos e internet indisponível, quando o fluxo completo é executado, então o pareamento termina com sucesso.
- [ ] Dado um dispositivo pareado, quando ele desfaz o pareamento, então perde a referência de sincronização e mantém os dados locais.

## Casos extremos

- Permissão negada ou revogada durante descoberta.
- Bluetooth e Wi-Fi indisponíveis.
- Endpoint desaparece antes da validação.
- Código expira, colide ou recebe tentativas repetidas.
- Processo é recriado durante advertising ou discovery.

## Decisões

| Decisão | Escolha | Justificativa |
| --- | --- | --- |
| Transporte | Nearby Connections com `P2P_STAR` | Corresponde à implementação atual e abstrai BLE, Bluetooth e Wi-Fi Direct. |
| Autorização inicial | Código de quatro dígitos | Permite confirmação presencial simples antes da troca da chave. |
| Identidade do grupo | Chave persistida no DataStore | Mantém o fluxo local e permite autenticar transferências posteriores. |
| Uso de Wi-Fi Direct | Somente transporte pontual gerenciado pelo Nearby | Evita mantê-lo ativo para sincronização contínua. |

## Fora de escopo

- Transferir o conjunto de dados após parear; consulte a spec 0102.
- Gerenciar membros já pareados; consulte a spec 0103.
- Sincronização contínua pela rede local; consulte a spec 0104.
- Login, Firebase ou qualquer serviço cloud.
