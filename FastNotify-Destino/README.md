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
- **Bloqueio temporário por IP**: após **3** tentativas inválidas bloqueia **30s**, depois **60s, 90s...** (+30s). Token válido reinicia o contador/bloqueio
- Janela **sem decoração**, `alwaysOnTop` (sobrepor qualquer informação)
- Formatação por tipo: **Alerta**, **Notificação**, **Informação**
- Exibição **centralizada** na tela/monitor indicado pela origem (card grande, fontes reforçadas)
- Some automaticamente após o tempo da origem (clique também fecha)
- **Som de alerta** (`sounds/alert_sound.mp3`) ao abrir a notificação — pode ser desativado nas configurações (`soundEnabled`)
- Log de recepção e lista de monitores
- **Logs em arquivo** (`logs/logs-AAAA-MM-DD.log`): mensagens, testes OK/falha, rejeições e ativação de bloqueio com **horário e IP de origem**; tentativas de IPs **já bloqueados** não são registradas; cache em memória com flush automático a cada **10 min** (append no arquivo do dia e limpeza do cache após gravação)
- **Janela de logs** no menu do tray (**Ver logs...**): escolher o dia, filtrar por tipo e **Salvar cache no log** (grava pendências no arquivo do dia atual e limpa o cache)
- **Tray icon** (SVG `img/mensagem-recebida.svg`) com menu: Abrir / **Configurações...** / **Ver logs...** / Sair
- Fechar a janela minimiza para o tray (a escuta continua)
- **Inicia no tray** (sem janela principal); duplo clique / **Abrir janela** no menu do tray abre a UI

## JavaFX no OpenJDK 25

O **OpenJDK 25 não inclui JavaFX**. Ele vem via dependência Maven `org.openjfx:javafx-controls` (baixada no build). Use `mvn javafx:run` para executar.

## Build

```bash
mvn clean package
```

Gera `target/fastnotify-destino.jar`, `target/FastNotify-Destino.exe` (Launch4j, ícone `img/mensagem-recebida.ico`), `target/lib/` e copia `target/config/destino.properties` (sem sobrescrever edits locais).

O `.jar` sozinho não tem ícone no Windows — use o `.exe` gerado (fique na mesma pasta do jar + `lib/`).

## Executar

```bash
mvn javafx:run
```

## Configuração

| Campo | Descrição                                          |
|-------|----------------------------------------------------|
| Porta | Porta de escuta (padrão `9876`), igual à da origem |
| Token | Mesmo valor configurado na origem                  |

| Onde | Arquivo |
|------|---------|
| Fonte (versionado) | `src/main/resources/config/destino.properties` |
| Dentro do jar | `/config/destino.properties` |
| Ao lado do jar | `target/config/destino.properties` |
| Em runtime | `./config/destino.properties` (1ª execução) |

Override: `-Dfastnotify.config=Caminho\custom.properties`.

Pasta de logs (padrão ao lado de `config/`): `-Dfastnotify.logs=Caminho\logs`.

## Fluxo

1. Inicie este aplicativo (**status ativo** na porta).
2. Na origem, aponte IP deste computador + porta + token.
3. Envie — a janela formatada é exibida sobre tudo e fecha sozinha.

## Protocolo

TCP, cabeçalho `FASTNOTIFY/1`:
`magic, version, token, type, durationMs, screenIndex, title, body`.

## Logs

Formato por linha em `logs/logs-2026-09-23.log`:

```text
2026-09-23 14:30:16 | MESSAGE | 192.168.0.10 | Recebida [Alerta] ...
2026-09-23 14:31:02 | TEST_FAIL | 192.168.0.99 | Teste FALHOU | token inválido (1/3)
```

IPs já bloqueados não geram novas linhas de log. Conexões não são registradas (só mensagens, testes, rejeições e bloqueios relevantes).

Tipos: `MESSAGE`, `TEST_OK`, `TEST_FAIL`, `BLOCK` (ativação), `REJECT`, `AUTH_OK`, `ERROR`, `INFO`.
