# FastNotify - Destino (Receiver)

Aplicativo **Maven + JavaFX** de exibição de notificações recebidas do **FastNotify - Origem**.

## Pacote

`com.victorqueiroga.fastnotify.destino`

## Stack

- Java 17+
- JavaFX 21 (`javafx-controls`)
- Maven

## Funcionalidades

- Escuta TCP na porta configurada
- Valida **token** (token inválido → notificação rejeitada)
- **PSK opcional**: `psk` vazia → o token de notificação/cadastro já usado vira a chave AES-256-GCM; preenchida → usa a PSK (mesma da Origem)
- **Auto-cadastro na Origem**: painel **Cadastrar nesta Origem** (host/IP da Origem, porta de cadastro `9877`, token de cadastro, nome, **departamento**) — hostname/IP **deste** equipamento vêm do SO; envia `FASTNOTIFY/REG` (v4/v5) com **hostName e hostIp separados**; a Origem grava **preferindo o hostname** (IP é dinâmico)
- **Bloqueio temporário por IP**: após **3** tentativas inválidas bloqueia **30s**, depois **60s, 90s...** (+30s). Token válido reinicia o contador/bloqueio
- Janela **sem decoração**, `alwaysOnTop` (sobrepor qualquer informação)
- Formatação por tipo: **Alerta**, **Notificação**, **Informação**
- Exibição **centralizada** na tela/monitor indicado pela origem (card grande, fontes reforçadas)
- Some automaticamente após o tempo da origem (clique também fecha)
- **Som de alerta** (`sounds/alert_sound.mp3`) ao abrir a notificação — pode ser desativado nas configurações (`soundEnabled`)
- Log de recepção e lista de monitores
- **Logs em arquivo** (`logs/logs-AAAA-MM-DD.log`): mensagens, testes OK/falha, rejeições e ativação de bloqueio com **horário, IP de origem e usuário** (full name / fallback `user.name` + domínio/grupo quando houver); tentativas de IPs **já bloqueados** não são registradas; cache em memória com flush automático a cada **10 min**
- **Rodapé da notificação**: `De: Nome` **ou** `De: DOMÍNIO\usuário` (mutuamente exclusivos — nome só se disponível; senão domínio\usuário) quando o protocolo envia remetente (v6/v7)
- **Retenção de logs**: `logRetentionDays` (padrão `30`, `0` = manter todos) em `destino.properties` ou Configurações → exclui arquivos com data anterior a **hoje − N dias** (startup + a cada 10 min + ao salvar config)
- **Janela de logs** no menu do tray (**Ver logs...**): escolher o dia, filtrar por tipo e **Salvar cache no log** (grava pendências no arquivo do dia atual e limpa o cache)
- **Tray icon** (SVG `img/mensagem-recebida.svg`) com menu: Abrir / **Configurações...** / **Ver logs...** / Sair
- Janela principal: status, **Configurações** e **Cadastrar nesta Origem** (GridPane) — porta/token/som/retenção **só** em Configurações
- Em **Configurações**: **Verificar firewall**, **Liberar porta** (cria/atualiza), **Remover regra**; **Inicializar com Windows** / **Remover inicialização** (porta do firewall só de `config/destino.properties`)
- Ícone da janela a partir do mesmo SVG (`TrayIcons.applyWindowIcons`)
- Fechar a janela minimiza para o tray (a escuta continua)
- **Inicia no tray** (sem janela principal); duplo clique / **Abrir janela** no menu do tray abre a UI
- **Instância única**: se já houver um Destino aberto, o novo atalho/exe mostra aviso e encerra (trava `config/destino.lock`)

## JavaFX no OpenJDK 25

O **OpenJDK 25 não inclui JavaFX**. Ele vem via dependência Maven `org.openjfx:javafx-controls` (baixada no build). Use `mvn javafx:run` para executar.

## Build

```bash
mvn clean package
```

Gera `target/fastnotify-destino.jar`, `target/FastNotify-Destino.exe` (Launch4j, ícone `img/mensagem-recebida.ico`), `target/lib/`, `target/sounds/`, `target/config/destino.properties` (sem sobrescrever edits locais), `target/instalar-inicializacao-destino.{bat,ps1}` e `target/liberar-porta-destino.{bat,ps1}` + `target/remover-porta-destino.bat` (firewall; porta só do `.properties`).

O `.jar` sozinho não tem ícone no Windows — use o `.exe` gerado (fique na mesma pasta do jar + `lib/`).

## Executar

```bash
mvn javafx:run
```

## Configuração

| Campo | Descrição |
|-------|-----------|
| Porta | Porta de escuta de notificações (padrão `9876`), igual à da origem |
| Token | Mesmo valor configurado na origem (token de notificação) |
| PSK | Opcional. Vazia → o token de notificação/cadastro já usado vira a chave AES-GCM |
| Origem (auto-cadastro) | Host/IP da Origem, porta de cadastro (`9877`), token de cadastro, nome deste destino (padrão: hostname do SO) |

| Onde | Arquivo |
|------|---------|
| Fonte (versionado) | `src/main/resources/config/destino.properties` |
| Dentro do jar | `/config/destino.properties` |
| Ao lado do jar | `target/config/destino.properties` |
| Em runtime | `./config/destino.properties` (1ª execução) |

Chaves de auto-cadastro em `destino.properties`: `origemHost`, `origemRegisterPort`, `origemRegisterToken`, `alias`, `department`, `psk` (opcional — se vazia, o token de notificação/cadastro já usado vira a chave de cifra AES-GCM).

Override: `-Dfastnotify.config=Caminho\custom.properties`.

Pasta de logs (padrão ao lado de `config/`): `-Dfastnotify.logs=Caminho\logs`.

## Fluxo

1. Inicie este aplicativo (**status ativo** na porta).
2. **Manual**: na Origem, aponte IP deste computador + porta + token (botão **Novo**), com **Departamento** se desejar.
3. **Automático**: painel **Cadastrar nesta Origem** → host/IP, porta `9877`, token de cadastro da Origem, nome, **departamento** → **Cadastrar nesta Origem**. O item aparece na lista da Origem.
4. Envie da Origem — a janela formatada é exibida sobre tudo e fecha sozinha.

## Firewall

Scripts em `src/main/resources/` → **`target/`** no `mvn package`. A **porta vem só de** `config/destino.properties` (`port`) — não há porta embutida no script.

```bat
cd target
liberar-porta-destino.bat
remover-porta-destino.bat
```

Regra de **entrada TCP** `FastNotify-Destino-Notificacao`. O script **consulta antes**: liberar cria ou **atualiza**; remover apaga se existir. No app: **Configurações → Liberar porta / Remover regra / Verificar firewall** e **Inicializar com Windows / Remover inicialização**. Status CLI: `liberar-porta-destino.ps1 -Acao Status`.

## Protocolo

TCP notificação, cabeçalho `FASTNOTIFY/1`:
`magic, version, token, type, durationMs, screenIndex, isTest, title, body, senderFullName, senderDomain`
(v4/v5: só `senderFullName`; v2/v3 legado: sem remetente).

TCP auto-cadastro (`FASTNOTIFY/REG`, porta `9877` da Origem):
`magic, version, tokenOrigem, alias, port, tokenDestino, screen, durationMs, department, hostName, hostIp` → **ACK**
(v4/v5: hostName + hostIp separados, do SO; v2/v3 legado: sem host).

## Logs

Formato por linha em `logs/logs-2026-09-23.log`:

```text
2026-09-23 14:30:16 | MESSAGE | 192.168.0.10 | João Silva (EMPRESA\TI) | Recebida [Alerta] ...
2026-09-23 14:31:02 | TEST_FAIL | 192.168.0.99 | João Silva (EMPRESA\TI) | Teste FALHOU | token inválido (1/3)
```

Coluna de usuário: **full name** do SO (se indisponível, `user.name`); entre parênteses, domínio/grupo quando houver (AD/Linux). O rodapé da notificação mostra **`De: Nome` ou `De: DOMÍNIO\usuário`** (um só — nome se houver; senão domínio\usuário).

IPs já bloqueados não geram novas linhas de log. Conexões não são registradas (só mensagens, testes, rejeições e bloqueios relevantes).

Tipos: `MESSAGE`, `TEST_OK`, `TEST_FAIL`, `BLOCK` (ativação), `REJECT`, `AUTH_OK`, `REGISTER` (auto-cadastro), `ERROR`, `INFO`.
