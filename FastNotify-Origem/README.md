# FastNotify - Origem (Sender)

Aplicativo **Maven + JavaFX** de envio de notificações em rede para o **FastNotify - Destino**.

## Pacote

`com.victorqueiroga.fastnotify.origem`

## Stack

- Java 17+
- JavaFX 21 (`javafx-controls`)
- Maven

## Funcionalidades

- **Destinos isolados**: cada destino tem o **seu** alias, host (hostname preferencial + IP separado), porta, token, tela, tempo e **departamento** — **não existe config global de destino**
- **Auto-cadastro**: o Destino envia **hostname e IP separados** (REG v4/v5); a Origem grava o **hostname** como alvo de conexão (IP é dinâmico) e mantém o IP como fallback/informação
- **Múltiplos destinos** em `config/destinos.properties` — seleção múltipla (Ctrl) para enviar a 1 ou vários
- **Logs em arquivo** (`logs/logs-AAAA-MM-DD.log`): envio, teste, cadastro, firewall, config e erros; coluna de usuário com **full name** (fallback `user.name`) + domínio/grupo quando houver; cache com flush a cada 10 min; janela **Ver logs...** no tray
- **Remetente na notificação**: o Destino exibe no rodapé **só** o nome (`De: Nome`) se houver; senão **só** `DOMÍNIO\usuário` (protocolo v6/v7 com `senderFullName` + `senderDomain`; legado v2–v5 sem os campos)
- **Retenção de logs**: `logRetentionDays` (padrão `30`, `0` = manter todos) em `origem.properties` ou Configurações → exclui `logs-AAAA-MM-DD.log` com data anterior a **hoje − N dias** (startup + a cada 10 min + ao salvar config)
- **Auto-cadastro de Destinos**: escuta na **porta de registro** (`9877`); o Destino se cadastra sozinho com **hostname e IP do SO separados**; a Origem grava **preferindo o hostname** (IP dinâmico) e usa o IP como fallback (upsert por hostname/IP — re cadastrar atualiza, não duplica). Status na barra: `Cadastro: porta 9877`
- **PSK opcional**: `psk` vazia → o token de cadastro/destino já usado vira a chave AES-256-GCM; preenchida → usa a PSK (mesma no Destino)
- **Teste por destino**: botão **Testar** (selecionado), **Testar todos**, **duplo clique** na lista e **Testar destino** dentro do diálogo Novo/Editar
- Mensagens fixas para reenvio rápido (`config/messages.properties`)
- Janela de composição: **Alerta**, **Notificação** ou **Informação** (tela/tempo vêm de cada destino)
- **Campainha** (botão amarelo 🔔 e item no tray): mensagem padrão sem abrir o diálogo; usa o tempo de cada destino (máx. 8s)
- Log de envios
- **Tray icon** (SVG `img/mensagem-enviada.svg`) com menu: Abrir / **Configurações...** / **🔔 Campainha** / Sair
- Botão **Firewall**: consulta se a regra de entrada da porta de cadastro existe/está ativa no Windows
- Fechar a janela minimiza para o tray (o app continua rodando)
- **Inicia no tray** (sem janela principal); duplo clique / **Abrir janela** no menu do tray abre a UI
- **Instância única**: se já houver uma Origem aberta, o novo atalho/exe mostra aviso e encerra (trava `config/origem.lock`)

## JavaFX no OpenJDK 25

O **OpenJDK 25 não inclui JavaFX**. Ele vem via dependência Maven `org.openjfx:javafx-controls` (baixada no build). Use `mvn javafx:run` para executar.

## Arquivos de dados

| Arquivo | Conteúdo |
|---------|----------|
| `config/destinos.properties` | **Destinos isolados** (JSON) — alias, host (hostname preferencial), hostIp, porta, token, tela, tempo, departamento |
| `config/messages.properties` | Mensagens fixas (JSON) |
| `config/origem.properties` | `registerPort` (padrão `9877`) + `registerToken` (cadastro) + `psk` opcional (vazia = token como chave); legado: mensagens fixas antigas |

Override opcional: `-Dfastnotify.config=Caminho\custom.properties`.

### Auto-cadastro (`FASTNOTIFY/REG`)

1. Tray → **Configurações...** → defina **Porta de cadastro** e **Token de cadastro** (vazio = cadastro recusado). Libere a porta no firewall (`liberar-portas-firewall.bat` na raiz do projeto, admin).
2. No Destino, painel **Cadastrar nesta Origem**: host/IP desta Origem, porta, token, nome (hostname/IP do Destino saem do SO).
3. A Origem valida o token, grava o destino **preferindo hostname** (IP separado) e responde **OK — Cadastrado/Atualizado**.

Botão **Firewall** na barra mostra se a regra `FastNotify-Origem-Cadastro` da porta configurada está Allow/ativa.

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
      "durationMs": 5000,
      "department": "Recepção"
    },
    {
      "alias": "CEO",
      "host": "192.168.1.50",
      "port": 9900,
      "token": "token-ceo",
      "screen": 1,
      "durationMs": 8000,
      "department": "Diretoria"
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
| `department` | `""` (vazio) |

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

TCP notificação, cabeçalho `FASTNOTIFY/1`:
`magic, version, token, type, durationMs, screenIndex, isTest, title, body, senderFullName, senderDomain`
(v4/v5: só `senderFullName`; v2/v3 legado: sem remetente).

TCP auto-cadastro (porta `9877`), cabeçalho `FASTNOTIFY/REG`:
`magic, version, tokenOrigem, alias, port, tokenDestino, screen, durationMs, department, hostName, hostIp` → **ACK**
(v4/v5: hostName + hostIp separados; v2/v3 legado: sem host).
