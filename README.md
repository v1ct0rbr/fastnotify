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
6. IP com token inválido é **bloqueado** (3 falhas → 30s, escala); eventos vão para `logs/logs-AAAA-MM-DD.log` nos **dois** apps (tray → **Ver logs...**).
7. **Departamento**: cada destino (Novo/Editar ou auto-cadastro) pode ter um **departamento** — aparece na lista e nos logs de envio/cadastro.
8. **Auto-cadastro** (opcional): o Destino se **cadastra sozinho** na Origem (painel *Cadastrar nesta Origem* + nome/departamento) na porta de registro (`9877`); a Origem grava **hostname preferencial** (IP separado) e o envio passa a funcionar sem digitar tudo à mão.
9. **Cifra (PSK)**: se `psk` estiver vazia, o **token já usado** (cadastro ou de cada destino) vira a chave AES-256-GCM — sem configurar chave extra.

```
[Origem]  lista de destinos ──TCP 9876──> [Destino]  porta/token  →  janela + som + ACK
[Destino] ──TCP 9877 (registro)────────> [Origem]   grava host   →  lista + dept
```

Ambos os apps **iniciam ocultos no tray** (duplo clique ou **Abrir janela** no menu).

**Instância única**: se já houver Origem/Destino rodando na máquina, o segundo aviso (*Instância já em execução*) e **não abre** (trava em `config/origem.lock` / `config/destino.lock`, liberada ao sair).

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
3. **Cadastrar nesta Origem** (opcional): host/IP da Origem, **porta de cadastro** (`9877`), **token de cadastro** da Origem, nome (alias) deste destino → botão **Cadastrar nesta Origem**. O destino aparece na lista da Origem automaticamente.
4. Fechar a janela **não encerra** — fica no tray escutando.

### 2. Origem (máquina que vai enviar)

1. Inicie a Origem (no tray).
2. Tray → **Configurações...**: defina **Porta de cadastro** (`9877`) e **Token de cadastro** (exigido do Destino). Libere a porta no firewall.
3. Abrir janela → **Novo** (lista de destinos): informe **alias, host, porta, token, tela, tempo** — **ou** deixe o Destino se cadastrar sozinho (passo 3 acima).
4. Opcional: **Testar**, **Testar todos**, **duplo clique** no item, ou **Testar destino** no diálogo (antes de salvar).
5. Selecione destinos (ou nenhum = **todos**) e envie:
   - **Abrir janela de mensagem...** → tipo + título + corpo
   - Mensagem fixa na barra lateral
   - **🔔 Campainha**

Porta/token/tela/tempo **sempre** vêm de cada destino selecionado.

---

## Configurações

| App | Arquivo | Conteúdo |
|-----|---------|----------|
| Origem | `config/destinos.properties` | JSON: destinos isolados (incl. `department`) |
| Origem | `config/messages.properties` | Mensagens fixas (JSON) |
| Origem | `config/origem.properties` | `registerPort`, `registerToken` + legado (mensagens); `psk` opcional (vazia = token como chave) |
| Destino | `config/destino.properties` | Porta + token + som + `origemHost`, `alias`, `department`, `psk` (opcional; vazia = token como chave) |
| Ambos | `logs/logs-AAAA-MM-DD.log` | Log em arquivo (cache 10 min; janela: tray → **Ver logs...**) |
| Ambos | `logRetentionDays` | Retenção em dias (padrão `30`; `0` = manter todos); purge no startup e a cada 10 min |

- Preferencial: **GUI** (tray → Configurações / diálogos / painel de cadastro).
- Arquivos: criados em `./config/` na 1ª execução (de recursos → jar → `target/config` → `./config`).
- Override: `-Dfastnotify.config=Caminho\custom.properties`
- Logs do Destino: pasta padrão ao lado de `config/` ou `-Dfastnotify.logs=Caminho\logs`

### Auto-cadastro (Destino → Origem)

| Lado | Onde | O que |
|------|------|-------|
| Origem | Configurações | **Porta de cadastro** (`9877`) + **Token de cadastro** (vazio = recusa) + **Retenção de logs** |
| Destino | Configurações | Porta/token/som + **Retenção de logs** |
| Destino | Painel *Cadastrar nesta Origem* | host/IP da Origem, porta `9877`, token de cadastro, nome |

- Protocolo de registro: magic `FASTNOTIFY/REG` (independente de `FASTNOTIFY/1`).
- **PSK** (`psk`): se vazia, o token já usado (cadastro ou de cada destino) vira a chave AES-256-GCM; se preenchida, usa a PSK (mesma nos dois lados).
- A Origem grava o destino **preferindo hostname** (IP do socket como fallback; upsert: re cadastrar **atualiza**, não duplica).
- Porta padrão de registro: **9877/tcp** (libere no firewall da Origem).
- Sem token de cadastro preenchido na Origem → cadastro é **recusado**.
- Status na barra da Origem: `Cadastro: porta 9877` (muda ao alterar a porta).

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

Padrões ao carregar JSON antigo/campo omisso: `port=9876`, `token=""`, `screen=0`, `durationMs=5000`, `department=""`.

### Protocolo (TCP)

Cabeçalho `FASTNOTIFY/1` (notificação):

```text
magic, version, token, type, durationMs, screenIndex, isTest, title, body, senderFullName, senderDomain
```

Tipos: `ALERT`, `NOTIFICATION`, `INFO` (+ flag de teste). Resposta: **ACK** (OK / falha).

Cabeçalho `FASTNOTIFY/REG` (auto-cadastro, porta `9877`):

```text
magic, version, tokenOrigem, alias, port, tokenDestino, screen, durationMs, department, hostName, hostIp
```

v4/v5: `hostName` + `hostIp` separados (preferência pelo hostname); v2/v3 legado: sem host. Resposta: mesmo **ACK** de notificação.

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

## Firewall (liberar portas)

Na **raiz do projeto** (Windows, requer **Administrador** para criar regras):

```bat
liberar-portas-firewall.bat
```

O script PowerShell `liberar-portas-firewall.ps1` **lê as portas** em `config/`:

| Arquivo | Chave | Regra criada (entrada TCP) |
|---------|-------|----------------------------|
| `config/destino.properties` | `port` (padrão `9876`) | `FastNotify-Destino-Notificacao` |
| `config/origem.properties` | `registerPort` (padrão `9877`) | `FastNotify-Origem-Cadastro` |

Outros comandos:

```bat
rem Status das regras (pode ser sem admin)
powershell -ExecutionPolicy Bypass -File liberar-portas-firewall.ps1 -Acao Status

rem Remover regras (admin)
remover-portas-firewall.bat
```

Nos **apps**, botão **Firewall** (Origem: barra superior; Destino: Configuração) consulta se a regra da porta configurada existe/está Allow — o resultado vai no log.

**Saída** (Origem → Destino): o Windows libera outbound por padrão; se a rede bloquear, crie regra de saída manualmente para a porta do Destino.

---

## Estrutura

```text
fastnotify/
├── FastNotify-Origem/     # envio
├── FastNotify-Destino/    # recepção
├── config/                # config em runtime (1ª execução)
├── logs/                  # logs do Destino
├── instalar-inicializacao-origem.{bat,ps1}
├── instalar-inicializacao-destino.{bat,ps1}
├── liberar-portas-firewall.{bat,ps1}
└── remover-portas-firewall.bat
```

Mais detalhes por app:

- [`FastNotify-Origem/README.md`](FastNotify-Origem/README.md)
- [`FastNotify-Destino/README.md`](FastNotify-Destino/README.md)

---

## Dicas

- **Firewall**: rode `liberar-portas-firewall.bat` (admin) — lê `config/*` e abre as entradas `9876`/`9877` (ou as portas configuradas). Botão **Firewall** nos apps mostra o status.
- **Teste rápido**: Origem → **Testar todos** (ou duplo clique) → Destino deve logar `TEST_OK`.
- **Auto-cadastro**: Origem com **Token de cadastro** preenchido → Destino preenche host + token → **Cadastrar nesta Origem** → item aparece na lista da Origem.
- **Sem seleção na lista** = envia para **todos** os destinos.
- Apps no tray: fechar a janela **não mata** o processo; use menu → **Sair**.
