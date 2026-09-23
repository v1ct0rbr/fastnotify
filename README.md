# FastNotify

Sistema de **notificações em rede** entre **Origem** (envia) e **Destino** (recebe e exibe) — **JavaFX + Maven + TCP**.

| Papel | Projeto | Pacote | O que faz |
|-------|---------|--------|-----------|
| **Origem** | `FastNotify-Origem` | `com.victorqueiroga.fastnotify.origem` | Cadastra destinos, compõe e envia mensagens |
| **Destino** | `FastNotify-Destino` | `com.victorqueiroga.fastnotify.destino` | Escuta a rede, valida token e mostra a janela |

---

## Como funciona

1. **Destino** sobe no **tray** e fica **escutando TCP** na porta configurada (ex.: `9876`) em silêncio.
2. **Origem** guarda uma lista de **destinos isolados** (`config/destinos.properties`): cada um tem **alias, host, porta, token, tela e tempo** — **não há config global de destino**.
3. Na Origem, você **seleciona** destinos (Ctrl para vários) ou **nenhum** (= todos) e envia:
   - **Alerta / Notificação / Informação** (janela de composição)
   - **Mensagens fixas** (`config/messages.properties`)
   - **Campainha** 🔔 (padrão, sem diálogo; usa o tempo do destino, máx. 8s)
4. A Origem abre TCP para `host:porta`, envia o pacote `FASTNOTIFY/1` com **token** e dados; o Destino **valida** o token e devolve **ACK**.
5. O Destino abre a **janela full-screen sem decoração**, `alwaysOnTop`, **centralizada na tela** pedida, com **som** (`sounds/alert_sound.mp3`) e fecha sozinha após o tempo do destino.
6. IP com token inválido é **bloqueado** (3 falhas → 30s, escala); eventos vão para `logs/logs-AAAA-MM-DD.log` (janela: menu do tray → **Ver logs...**).

```
[Origem]  lista de destinos ──TCP──> [Destino]  porta/token  →  janela + som + ACK
```

Ambos os apps **iniciam ocultos no tray** (duplo clique ou **Abrir janela** no menu).

---

## Pré-requisitos

- **Java 17+** (testado com OpenJDK 25; JavaFX vem do Maven)
- **Maven 3.9+**
- Windows 10+ para `.exe`/tray/startup (Linha de comandos também funciona com `mvn javafx:run`)

---

## Build

```bash
cd FastNotify-Origem && mvn clean package
cd ../FastNotify-Destino && mvn clean package
```

Gera em cada `*/target/`:

- `fastnotify-{origem,destino}.jar`
- `FastNotify-{Origem,Destino}.exe` (Launch4j, ícone no Windows)
- `lib/`, `config/`, e `sounds/` no Destino

Se o app estiver rodando (arquivos travados), use `mvn package` sem `clean`.

---

## Executar

### Desenvolvimento

```bash
# Máquina destino (primeiro)
cd FastNotify-Destino && mvn javafx:run

# Máquina origem
cd FastNotify-Origem && mvn javafx:run
```

### Executável Windows

Use o `.exe` na mesma pasta do `.jar` + `lib/` + `config/` (e `sounds/` no Destino). Requer Java no PATH.

Os apps **abrem no tray**; notificação avisa “duplo clique para abrir”.

---

## Uso (fluxo típico)

### 1. Destino (máquina que vai exibir)

1. Inicie o Destino → status **ativo** na porta.
2. (Opcional) Tray → **Configurações...**: porta, token, som on/off.
3. Fechar a janela **não encerra** — fica no tray escutando.

### 2. Origem (máquina que vai enviar)

1. Inicie a Origem (no tray).
2. Abrir janela → **Novo** (lista de destinos): informe **alias, host, porta, token, tela, tempo**.
3. Opcional: **Testar**, **Testar todos**, **duplo clique** no item, ou **Testar destino** no diálogo (antes de salvar).
4. Selecione destinos (ou nenhum = **todos**) e envie:
   - **Abrir janela de mensagem...** → tipo + título + corpo
   - Mensagem fixa na barra lateral
   - **🔔 Campainha**

Porta/token/tela/tempo **sempre** vêm de cada destino selecionado.

---

## Configurações

| App | Arquivo | Conteúdo |
|-----|---------|----------|
| Origem | `config/destinos.properties` | JSON: destinos isolados |
| Origem | `config/messages.properties` | Mensagens fixas (JSON) |
| Origem | `config/origem.properties` | Legado (mensagens); sem config de destino |
| Destino | `config/destino.properties` | Porta + token + som |

- Preferencial: **GUI** (tray → Configurações / diálogos).
- Arquivos: criados em `./config/` na 1ª execução (de recursos → jar → `target/config` → `./config`).
- Override: `-Dfastnotify.config=Caminho\custom.properties`
- Logs do Destino: pasta padrão ao lado de `config/` ou `-Dfastnotify.logs=Caminho\logs`

### Destinos (`config/destinos.properties`)

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

Padrões ao carregar JSON antigo/campo omisso: `port=9876`, `token=""`, `screen=0`, `durationMs=5000`.

### Protocolo (TCP)

Cabeçalho `FASTNOTIFY/1`:

```text
magic, version, token, type, durationMs, screenIndex, isTest, title, body
```

Tipos: `ALERT`, `NOTIFICATION`, `INFO` (+ flag de teste). Resposta: **ACK** (OK / falha).

---

## Ícone no Windows

O `.jar` usa o ícone do Java. Após o build, o **Launch4j** gera:

- `FastNotify-Origem/target/FastNotify-Origem.exe` (ícone `mensagem-enviada`)
- `FastNotify-Destino/target/FastNotify-Destino.exe` (ícone `mensagem-recebida`)

---

## Inicializar com o Windows (Win10+)

Na **raiz do projeto**, scripts **separados** por app:

```bat
instalar-inicializacao-origem.bat
instalar-inicializacao-destino.bat
```

Ou:

```bat
powershell -ExecutionPolicy Bypass -File instalar-inicializacao-origem.ps1
powershell -ExecutionPolicy Bypass -File instalar-inicializacao-destino.ps1
```

Cada script (sem admin — usuário atual):

1. Localiza o `.exe` do app (`FastNotify-*/target/`, ou busca recursiva).
2. Remove entrada antiga em `HKCU\...\CurrentVersion\Run` e atalhos antigos daquele app.
3. Cria a entrada apontando para o caminho atual.

Chaves: `FastNotify-Origem` e `FastNotify-Destino`. Para remover: apague a chave no Registro ou o atalho em `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup`.

---

## Estrutura

```text
fastnotify/
├── FastNotify-Origem/     # envio
├── FastNotify-Destino/    # recepção
├── config/                # config em runtime (1ª execução)
├── logs/                  # logs do Destino
├── instalar-inicializacao-origem.{bat,ps1}
└── instalar-inicializacao-destino.{bat,ps1}
```

Mais detalhes por app:

- [`FastNotify-Origem/README.md`](FastNotify-Origem/README.md)
- [`FastNotify-Destino/README.md`](FastNotify-Destino/README.md)

---

## Dicas

- **Firewall**: libere a porta de escuta (ex. 9876/tcp) no Destino.
- **Teste rápido**: Origem → **Testar todos** (ou duplo clique) → Destino deve logar `TEST_OK`.
- **Sem seleção na lista** = envia para **todos** os destinos.
- Apps no tray: fechar a janela **não mata** o processo; use menu → **Sair**.
