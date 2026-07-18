# Runbook: validação física de compartilhamento local

## Controle do documento

| Campo | Valor |
| --- | --- |
| Família | `local-sharing` |
| Specs | 0101, 0102, 0103, 0104 e 0105 |
| Tipo | Teste manual em dois dispositivos Android físicos |
| Estado inicial esperado | Specs em `Implemented`; tarefas físicas abertas |
| Resultado permitido | `Pass`, `Fail` ou `Blocked` |

## Objetivo

Validar em hardware real os comportamentos que não podem ser comprovados apenas
com testes locais, emulador ou dois processos: Nearby Connections, permissões e
rádios reais, ausência de internet, NSD em uma rede Wi-Fi real, interrupções de
transporte, background e convergência entre dois bancos independentes.

Este runbook não substitui `./gradlew spotlessCheck`, `./gradlew test` ou os
testes instrumentados. Execute-o somente sobre um commit que já tenha passado
essas verificações.

## Critério de conclusão

A família pode mudar de `Implemented` para `Completed` somente quando:

1. todos os casos obrigatórios deste runbook estiverem em `Pass`;
2. não houver defeito aberto que invalide um critério de aceitação;
3. as evidências identificarem commit, APK, dispositivos e resultado;
4. as tarefas físicas de 0101–0105 estiverem marcadas como concluídas;
5. os critérios físicos correspondentes nas specs estiverem reconciliados.

Não use `Pass` para um passo não executado. Use `Blocked` quando o ambiente não
permitir a execução e registre a causa.

## Responsabilidades

- **Executor:** opera os dispositivos e registra evidências sem alterar o fluxo.
- **Observador:** confere os resultados nos dois dispositivos e registra tempos,
  contadores e divergências.
- Uma pessoa pode exercer ambos os papéis, mas as verificações devem cobrir os
  dois lados.

## Pré-condições

- Dois dispositivos Android físicos, chamados **A** e **B**.
- Versões de Android e modelos registrados na folha de execução.
- Mesmo APK debug, gerado a partir do mesmo commit, instalado nos dois.
- Cabo/ADB ou depuração sem fio disponível para coleta de logs.
- Bluetooth e Wi-Fi funcionais nos dois dispositivos.
- Uma rede Wi-Fi local em que os clientes possam se comunicar entre si.
- Capacidade de bloquear o acesso à internet sem desligar a rede local.
- Android Studio Database Inspector disponível para o caso de empate exato.
- Dados usados no teste não podem conter informações clínicas reais.

## Preparação do build

```bash
git status --short
git rev-parse --short HEAD
./gradlew spotlessCheck
./gradlew test
./gradlew assembleDebug && ./gradlew installDebug
```

O APK fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Instale o mesmo arquivo em A e B. Se `installDebug` selecionar apenas um
dispositivo, instale explicitamente no outro:

```bash
adb -s <serial-A> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial-B> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Registro e evidências

Crie uma pasta fora do repositório ou ignorada pelo Git:

```text
local-sharing-<commit>-<AAAA-MM-DD-HHMM>/
```

Para cada caso, registre:

- resultado (`Pass`, `Fail` ou `Blocked`);
- horário de início e fim;
- captura ou gravação dos dois dispositivos;
- logcat relevante, sem chaves ou dados sensíveis;
- estado e contadores exibidos;
- observações e ID do bugfix, se houver falha.

Comandos úteis:

```bash
adb -s <serial-A> logcat -c
adb -s <serial-B> logcat -c
adb -s <serial-A> logcat -v threadtime > device-A.log
adb -s <serial-B> logcat -v threadtime > device-B.log
```

## Fixtures

Depois de limpar os dados do app nos dois dispositivos, crie:

| Dispositivo | Pet | Registro exclusivo | Nome do dispositivo |
| --- | --- | --- | --- |
| A | `Mimi-A` | peso identificável como `A` | `Petit A` |
| B | `Mimi-B` | tarefa identificável como `B` | `Petit B` |

Anote a quantidade inicial de pets, pesos, vacinas, vermífugos e tarefas em
cada dispositivo. Esses valores serão usados nos contadores de MERGE e REPLACE.

## Casos de teste

### LS-PAIR-01 — código incorreto e pareamento correto

**Cobre:** 0101; autorização, retry e persistência pós-autorização.

1. Em A, inicie o modo que anuncia o código de quatro dígitos.
2. Em B, inicie a descoberta e selecione A.
3. Digite um código diferente do exibido em A.
4. Confirme que B mostra rejeição e permite nova tentativa.
5. Confirme que nenhum grupo incompleto aparece em A ou B.
6. Digite o código correto.
7. Confirme o pareamento nos dois dispositivos.
8. Feche e reabra o app nos dois.

**Resultado esperado:** o código incorreto não autoriza nem persiste associação;
o correto persiste o mesmo grupo e as identidades estáveis nos dois lados.

**Evidência mínima:** vídeo do código incorreto, retry, sucesso e estado após
reinício.

### LS-PAIR-02 — cancelamento e perda do endpoint

**Cobre:** 0101; cleanup idempotente.

1. Inicie anúncio em A e descoberta em B.
2. Cancele em B antes de informar o código.
3. Repita e desligue Bluetooth ou Wi-Fi em A enquanto B aguarda.
4. Restaure os rádios e repita o pareamento desde o início.

**Resultado esperado:** anúncio, descoberta e conexão são encerrados sem grupo
parcial; uma nova tentativa funciona sem reiniciar o dispositivo.

### LS-PAIR-03 — pareamento sem internet

**Cobre:** 0101; operação local sem servidor remoto.

1. Mantenha Bluetooth e a rede Wi-Fi local ativos.
2. Bloqueie internet móvel e acesso WAN nos dois dispositivos.
3. Confirme em navegador que não há acesso à internet.
4. Execute LS-PAIR-01 com o código correto.

**Resultado esperado:** pareamento concluído normalmente sem acesso à internet.

### LS-XFER-01 — MERGE autorizado

**Cobre:** 0102 e 0105; transferência, merge e contadores.

1. Use um grupo pareado e mantenha as fixtures diferentes em A e B.
2. Inicie a transferência A → B.
3. Em B, escolha MERGE.
4. Observe o progresso até 100%.
5. Compare o banco visível de B com as fixtures.
6. Confira adicionados, atualizados e removidos por tipo.

**Resultado esperado:** B conserva seus dados e recebe os dados de A; progresso
é monotônico e os contadores correspondem às diferenças anotadas.

### LS-XFER-02 — REPLACE destrutivo

**Cobre:** 0102; confirmação e substituição exata.

1. Crie em B um registro que não existe em A.
2. Inicie A → B e selecione REPLACE.
3. Cancele a primeira confirmação e confirme que nada mudou.
4. Repita e confirme a ação destrutiva.
5. Compare todos os tipos de entidade em A e B.

**Resultado esperado:** sem confirmação, nada muda; após confirmação, B reflete
exatamente o bundle de A e remove os registros ausentes, com contadores corretos.

### LS-XFER-03 — interrupção, cancelamento e retry

**Cobre:** 0102; payload parcial e recuperação.

1. Prepare dados suficientes para que o progresso seja visível.
2. Inicie A → B.
3. Durante a transferência, desligue Bluetooth/Wi-Fi em um dispositivo.
4. Confirme que B não persistiu um payload parcial.
5. Restaure os rádios e execute novamente até concluir.
6. Repita usando o botão de cancelamento da interface.
7. Repita sem acesso à internet.

**Resultado esperado:** interrupção e cancelamento não alteram o banco; retry
conclui sem duplicação; internet não é necessária.

### LS-GROUP-01 — rename e último sync

**Cobre:** 0103; identidade estável e estados da UI.

1. Anote o UUID, chave/grupo e nome de A.
2. Renomeie A para `Petit A Renamed`.
3. Confirme o novo nome localmente e reinicie o app.
4. Sincronize com B.
5. Confirme o novo nome em B e o horário de último sync nos dois.

**Resultado esperado:** somente o nome muda; UUID, grupo e autorização permanecem.

### LS-GROUP-02 — remoção e revogação da chave antiga

**Cobre:** 0103; remoção idempotente e revogação.

1. Em A, remova B e confirme a ação.
2. Sincronize ou permita a propagação do evento.
3. Em B, tente iniciar sync usando a associação antiga.
4. Repita a remoção em A, se a interface ainda permitir.
5. Confira que os pets permanecem em ambos.

**Resultado esperado:** B desaparece da associação, não acessa dados com a chave
antiga, a repetição não cria efeitos adicionais e os dados clínicos permanecem.

### LS-GROUP-03 — saída offline e novo grupo

**Cobre:** 0103 e 0104; outbox de saída e isolamento multi-grupo.

1. Pareie novamente A e B, se necessário.
2. Deixe A offline e faça A sair do grupo.
3. Confirme que a chave visível é removida e os pets de A permanecem.
4. Em A, entre em um novo grupo com um terceiro peer ou recrie a associação em B.
5. Restabeleça a rede antiga e execute tentativas manuais até o `LEAVE` ser entregue.
6. Confirme que o grupo atual continua sincronizando mesmo se o peer antigo não
   estiver disponível.
7. Reinicie A e repita uma tentativa.

**Resultado esperado:** o `LEAVE` antigo é entregue uma única vez, não vaza dados
clínicos, não bloqueia o grupo atual e sua credencial restrita é descartada após ACK.

### LS-CONFLICT-01 — edit/edit

**Cobre:** 0105; determinismo e simetria.

1. Sincronize um registro-base nos dois dispositivos.
2. Desative o sync e desconecte a rede.
3. Edite o mesmo registro com valores diferentes em A e B, anotando a ordem.
4. Reconecte e sincronize nos dois sentidos.
5. Repita a sincronização sem novas alterações.

**Resultado esperado:** ambos terminam com a versão realmente mais recente; o
retry não muda estado nem contadores novamente.

### LS-CONFLICT-02 — edit/delete

1. Sincronize um registro-base.
2. Offline, edite-o em A e exclua-o em B.
3. Execute uma vez com a edição mais recente e outra com a exclusão mais recente.
4. Sincronize nos dois sentidos.

**Resultado esperado:** a operação com timestamp efetivamente mais recente vence;
ambos convergem sem ressuscitar tombstone indevidamente.

### LS-CONFLICT-03 — delete/delete

1. Sincronize um registro-base.
2. Offline, exclua-o em A e B.
3. Reconecte, sincronize nos dois sentidos e repita.

**Resultado esperado:** ambos mantêm um resultado excluído convergente e
idempotente, sem duplicação de histórico.

### LS-CONFLICT-04 — empate exato de timestamp

1. Parta de um registro-base sincronizado.
2. Offline, crie payloads divergentes em A e B.
3. No Database Inspector, atribua o mesmo `updatedAt` às duas versões. Registre
   o valor usado e exporte evidência das linhas, sem conteúdo sensível.
4. Sincronize A → B e B → A.
5. Repita a ordem começando de bancos restaurados e invertendo o primeiro envio.
6. Execute um retry sem novas alterações.

**Resultado esperado:** o vencedor é igual nas duas ordens, conforme o
tie-breaker documentado; retry é idempotente.

### LS-LAN-01 — descoberta e sync bidirecional em foreground

**Cobre:** 0104; NSD, autenticação e changesets bidirecionais.

1. Coloque A e B no mesmo Wi-Fi com isolamento de clientes desativado.
2. Ative sync automático nos dois.
3. Crie uma alteração exclusiva em cada dispositivo.
4. Abra os dois apps em foreground.
5. Observe estados de descoberta, sync e conclusão.
6. Confira dados, histórico, peer, horário, tipo e contadores nos dois.
7. Saia do app e confira no log que descoberta e sockets foram liberados.

**Resultado esperado:** ambos convergem; nenhum dado clínico é enviado antes da
autenticação; o lifecycle encerra recursos fora do foreground.

### LS-LAN-02 — perda e retorno do Wi-Fi

1. Inicie uma sessão com alterações pendentes nos dois.
2. Desligue o Wi-Fi durante a transferência.
3. Confirme estado de erro/peer indisponível e ausência de persistência parcial.
4. Religue o Wi-Fi sem abrir uma nova associação.
5. Aguarde ou acione tentativa manual.

**Resultado esperado:** alterações acumuladas convergem sem duplicação e o ACK
perdido pode ser repetido com segurança.

### LS-LAN-03 — background e WorkManager

1. Ative sync automático e deixe alterações pendentes.
2. Coloque os apps em background.
3. Mantenha os dispositivos carregados e na rede pelo intervalo permitido pelo
   Android, nunca inferior aos 15 minutos configurados.
4. Registre o estado do WorkManager com `adb shell dumpsys jobscheduler` e logs.
5. Reabra os apps e confira dados e histórico.

**Resultado esperado:** existe trabalho periódico único, com rede conectada e
backoff; não existe serviço persistente ou grupo Wi-Fi Direct contínuo.

### LS-LAN-04 — toggle off e tentativa manual

1. Desative sync automático em A.
2. Reabra A e aguarde além do timeout normal.
3. Confirme por UI/log que A não anuncia nem descobre automaticamente.
4. Crie uma alteração e use a tentativa manual.
5. Reative o toggle e repita em foreground.

**Resultado esperado:** o toggle interrompe NSD e worker automáticos; a ação
manual inicia uma tentativa nova e não reutiliza um estado `Synced` antigo.

### LS-LAN-05 — chave inválida antes do payload

Este caso exige uma build debug e debugger porque o fluxo normal filtra grupos
diferentes ainda no NSD.

1. Mantenha A e B no mesmo grupo e inicie a descoberta.
2. Em B, pause no início de `LanSessionRunner.runClient`, depois que o peer foi
   resolvido e antes da criação de `LanHandshakeClient`.
3. Apenas para essa sessão, substitua no debugger a chave usada pelo handshake
   por outra chave Base64URL válida de 32 bytes, preservando o group ID já
   anunciado. Não persista a chave alterada.
4. Retome a execução e capture logs dos dois dispositivos.
5. Confira que A retorna erro e fecha antes de receber `CHANGESET`.
6. Remova a alteração do debugger e confirme que uma tentativa normal funciona.

**Resultado esperado:** `AUTHENTICATION_FAILED`/`ERROR` e `CLOSE` antes de
qualquer payload clínico; nenhum banco ou credencial persistente é alterado.

Se o debugger não permitir a injeção sem alterar o APK, registre `Blocked` e
use o teste automatizado de handshake inválido como evidência complementar; não
marque a validação física como `Pass`.

## Regressão final

Depois de todos os casos:

1. reinicie A e B;
2. confirme que o grupo ativo correto continua disponível;
3. confirme que pets e histórico local não foram apagados por remove/leave;
4. execute mais um sync sem alterações e confirme ausência de duplicação;
5. execute novamente `./gradlew spotlessCheck` e `./gradlew test` no commit testado;
6. confira `git status --short` antes de atualizar specs e tarefas.

## Tratamento de falhas

Se um resultado for `Fail` e o comportamento não estiver descrito como pendência
da spec:

1. preserve logs, vídeos, commit, dispositivos e passos de reprodução;
2. crie `docs/bugfixes/YYYY-MM-DD-<slug>.md`;
3. documente reprodução, impacto, hipótese, regressão e proposta;
4. respeite o bugfix approval gate antes de alterar produção;
5. após a correção, repita o caso que falhou e a regressão final.

## Folha de execução

```text
Data/hora:
Executor:
Observador:
Commit:
APK SHA-256:
Dispositivo A / Android / serial:
Dispositivo B / Android / serial:
Rede / roteador / isolamento de clientes:
Internet disponível no início: sim / não

LS-PAIR-01: Pass / Fail / Blocked — evidência:
LS-PAIR-02: Pass / Fail / Blocked — evidência:
LS-PAIR-03: Pass / Fail / Blocked — evidência:
LS-XFER-01: Pass / Fail / Blocked — evidência:
LS-XFER-02: Pass / Fail / Blocked — evidência:
LS-XFER-03: Pass / Fail / Blocked — evidência:
LS-GROUP-01: Pass / Fail / Blocked — evidência:
LS-GROUP-02: Pass / Fail / Blocked — evidência:
LS-GROUP-03: Pass / Fail / Blocked — evidência:
LS-CONFLICT-01: Pass / Fail / Blocked — evidência:
LS-CONFLICT-02: Pass / Fail / Blocked — evidência:
LS-CONFLICT-03: Pass / Fail / Blocked — evidência:
LS-CONFLICT-04: Pass / Fail / Blocked — evidência:
LS-LAN-01: Pass / Fail / Blocked — evidência:
LS-LAN-02: Pass / Fail / Blocked — evidência:
LS-LAN-03: Pass / Fail / Blocked — evidência:
LS-LAN-04: Pass / Fail / Blocked — evidência:
LS-LAN-05: Pass / Fail / Blocked — evidência:

Resultado geral: Pass / Fail / Blocked
Bugfixes abertos:
Observações:
```
