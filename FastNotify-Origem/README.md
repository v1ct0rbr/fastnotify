# FastNotify - Origem (Sender)

Aplicativo **Maven + JavaFX** de envio de notificações em rede para o **FastNotify - Destino**.

## Pacote

`com.victorqueiroga.fastnotify.origem`

## Stack

- Java 17+
- JavaFX 21 (`javafx-controls`)
- Maven

## Funcionalidades

- **Destinos isolados**: cada destino tem o **seu** alias, host (hostname/IP), porta, token, tela e tempo — **não existe config global de destino**
- **Múltiplos destinos** em `config/destinos.properties` — seleção múltipla (Ctrl) para enviar a 1 ou vários
- **Teste por destino**: botão **Testar** (selecionado), **Testar todos**, **duplo clique** na lista e **Testar destino** dentro do diálogo Novo/Editar
- Mensagens fixas para reenvio rápido (`config/messages.properties`)
- Janela de composição: **Alerta**, **Notificação** ou **Informação** (tela/tempo vêm de cada destino)
- **Campainha** (botão amarelo 🔔 e item no tray): mensagem padrão sem abrir o diálogo; usa o tempo de cada destino (máx. 8s)
- Log de envios
- **Tray icon** (SVG `img/mensagem-enviada.svg`) com menu: Abrir / **Configurações...** / **🔔 Campainha** / Sair
- Fechar a janela minimiza para o tray (o app continua rodando)
- **Inicia no tray** (sem janela principal); duplo clique / **Abrir janela** no menu do tray abre a UI

## JavaFX no OpenJDK 25

O **OpenJDK 25 não inclui JavaFX**. Ele vem via dependência Maven `org.openjfx:javafx-controls` (baixada no build). Use `mvn javafx:run` para executar.

## Arquivos de dados

| Arquivo | Conteúdo |
|---------|----------|
| `config/destinos.properties` | **Destinos isolados** (JSON) — host, porta, token, tela, tempo |
| `config/messages.properties` | Mensagens fixas (JSON) |
| `config/origem.properties` | Legado (mensagens fixas antigas); **não** guarda mais config de destino |

Override opcional: `-Dfastnotify.config=Caminho\custom.properties`.

## Build

```bash
mvn clean package
```

Gera:

- `target/fastnotify-origem.jar`
- `target/FastNotify-Origem.exe` (Launch4j, ícone `img/mensagem-enviada.ico`)
- `target/lib/` (dependências)
- `target/config/`

O `.jar` sozinho não tem ícone no Windows — use o `.exe` gerado (fique na mesma pasta do jar + `lib/`).

## Executar

```bash
mvn javafx:run
```

## Destinos isolados (`config/destinos.properties` — JSON)

Cada item é **autocontido**; sem herança de config global:

```json
{
  "destinos": [
    {
      "alias": "Recepção",
      "host": "recepcao.local",
      "port": 9876,
      "token": "token-recepcao",
      "screen": 0,
      "durationMs": 5000
    },
    {
      "alias": "CEO",
      "host": "192.168.1.50",
      "port": 9900,
      "token": "token-ceo",
      "screen": 1,
      "durationMs": 8000
    }
  ]
}
```

| Campo | Padrão ao carregar JSON antigo/omisso |
|-------|----------------------------------------|
| `port` | `9876` |
| `token` | `""` (vazio) |
| `screen` | `0` |
| `durationMs` | `5000` |

### Comportamento

- **Sem seleção** → envia/testa **todos** os destinos.
- **Com seleção** (Ctrl) → só os selecionados.
- Envio, reenvio, compose e campainha usam **sempre** porta/token/tela/tempo **do destino**.
- **Testar** → item selecionado; **Testar todos** → lista inteira; **duplo clique** → um destino; no diálogo **Testar destino** → valida o form antes de salvar.

| Onde | Arquivo |
|------|---------|
| Em runtime | `./config/destinos.properties` (criado na 1ª execução) |

### Mensagens fixas (`config/messages.properties` — JSON)

Carregadas ao abrir a Origem e salvas ao editar/reenviar:

```json
{
  "messages": [
    {
      "type": "NOTIFICATION",
      "title": "Manutenção programada",
      "body": "Sistema entra em manutenção às 22h."
    }
  ]
}
```

## Uso

1. Inicie o **Destino** (porta/token dele).
2. Na **Origem**, cadastre destinos (**Novo**): host, porta, token, tela, tempo — cada um isolado.
3. Opcionalmente **Testar** / **Testar todos** / duplo clique.
4. Selecione destinos (ou nenhum = todos) e use mensagens fixas, **Abrir janela de mensagem...** ou **🔔 Campainha**.

## Protocolo

TCP, cabeçalho `FASTNOTIFY/1`:
`magic, version, token, type, durationMs, screenIndex, title, body`.
