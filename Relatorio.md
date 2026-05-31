# Distributed Battleship Games System — Relatório e Guia de Defesa

**Sistemas Distribuídos · UFP/FCT · 2026**

---

## 1. Visão Geral da Arquitetura

```
┌──────────────────────────────────────────────────────────────────┐
│                     CLIENTE (ClientApp + GameUI)                  │
│  Terminal : autenticação, lobby, criação/entrada em jogos         │
│  GameUI   : interface gráfica Swing — tabuleiro 10×10             │
│  GameObserver (RMI callback) ou BattleshipGameConsumer (RabbitMQ) │
└───────────────┬──────────────────────────┬────────────────────────┘
                │  Java RMI (porta 1099)    │  AMQP (porta 5672)
                ▼                           ▼
┌────────────────────────────┐    ┌──────────────────────────────┐
│  Servidor Nó A (porta 1099)│◀──▶│  Servidor Nó B (porta 1100)  │
│  BattleshipFactoryImpl      │RMI │  BattleshipFactoryImpl        │
│  LobbySessionImpl           │sync│  Replicação de utilizadores   │
│  BattleshipGameSubjectImpl  │    └──────────────────────────────┘
│  BattleshipGamePublisher    │
└─────────────┬──────────────┘
              │ AMQP (só se modo=PUBSUB)
   ┌──────────▼───────────┐
   │    RabbitMQ Broker    │
   │  Exchange fanout/jogo │
   │  Fila durável/jogador │
   └──────────────────────┘
```

### Tecnologias

| Tecnologia | Utilização |
|---|---|
| **Java RMI** | Comunicação cliente–servidor; sincronização entre nós (R5) |
| **RabbitMQ / AMQP** | Pub/Sub assíncrono e persistente (modo PUBSUB) |
| **JWT · JJWT 0.12.6** | Tokens de sessão seguros — HS256, expiração 8 h |
| **Java Swing** | Interface gráfica do jogo (GameUI) |
| **Maven** | Build e dependências |
| **JUnit 5** | Testes automáticos de lógica de jogo |

### Padrões de Design

| Padrão | Implementação |
|---|---|
| **Factory Method** | `BattleshipFactory` cria `LobbySession` |
| **Observer** | `BattleshipGameSubject` + `BattleshipGameObserver` via RMI |
| **Publish/Subscribe** | `BattleshipGamePublisher` + `BattleshipGameConsumer` via RabbitMQ |
| **DTO** | `GameState`, `GameAction`, `GameInfo`, `ShipPlacement`, `Shot` — Serializable |
| **Proxy** | `BattleshipGameSubject` devolvido pelo lobby é um stub RMI |

### Estrutura de pacotes

```
edu.ufp.inf.sd.battleshipgame/
│
├── rmi/                          Interfaces e implementações RMI
│   ├── BattleshipFactory             Interface: register() / login()
│   ├── BattleshipFactoryImpl         Impl + sincronização peer (R5)
│   ├── BattleshipFactoryPeer         Interface inter-nós: syncRegister / syncLogout / ping
│   ├── LobbySession                  Interface: listGames / createGame(mode) / getProxy
│   ├── LobbySessionImpl              Implementação da sessão de lobby
│   ├── BattleshipGameSubject         Interface: attach / detach / setState / getState / getGameMode
│   ├── BattleshipGameSubjectImpl     Lógica completa do jogo + publisher RabbitMQ
│   ├── BattleshipGameObserver        Interface do callback Observer
│   ├── GameState                     Estado completo do jogo (Serializable)
│   ├── PlayerState                   Estado por jogador: navios + tiros recebidos
│   ├── GameAction                    Ação: PLACE_SHIP / FIRE / PASS
│   ├── GamePhase                     Enum: WAITING / PLACING_SHIPS / IN_PROGRESS / FINISHED
│   ├── GameInfo                      DTO: ID + nº jogadores + modo (para listagem)
│   ├── JwtUtils                      Geração e validação de tokens JWT
│   └── Coordinate / Shot / ShipPlacement / Orientation / GameActionType
│
├── pubsub/                       RabbitMQ Publish/Subscribe
│   ├── BattleshipGamePublisher       Publica GameState no exchange do jogo
│   ├── BattleshipGameConsumer        Consome mensagens da fila do jogador
│   ├── RabbitMqConfig                Host e prefixos de exchange/fila
│   └── SerializationUtils            Serialização Java ↔ byte[]
│
├── Server/
│   └── ServerApp                 Ponto de entrada do servidor (--port, --peer)
│
├── Client/
│   └── ClientApp                 Terminal: autenticação, lobby, abertura da GameUI
│
└── GameUI                        Interface gráfica Swing do jogo
```

---

## 2. Fluxo Completo de Utilização

### Arrancar o sistema

```bash
# Terminal 1 — Servidor (sem flags, sem opções)
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp

# Terminal 2 — Cliente Alice
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Client.ClientApp

# Terminal 3 — Cliente Bob
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Client.ClientApp
```

### Fluxo passo a passo

```
SERVIDOR                    ALICE (Terminal 2)           BOB (Terminal 3)
────────                    ──────────────────           ────────────────
Iniciado na porta 1099
Pronto. Ctrl+C para parar.
                            1. Registar
                               Username: alice
                               Password: 1234
                               → [JWT token mostrado]

                                                         1. Registar
                                                            Username: bob
                                                            Password: 5678
                                                            → [JWT token mostrado]

                            2. Criar novo jogo (e entrar)
                               Escolhe o modo:
                               1. RMI   2. PUBSUB
                               → Opção: 1
                               Jogo criado: Game-XXXXXXXX [modo: RMI]
                               A aguardar adversário...
                               → GameUI da Alice abre
                                 (fase: WAITING FOR PLAYERS)

[Node-1099] 'alice' criou
 Game-XXXXXXXX [RMI]
                                                         2. Entrar num jogo
                                                            → Lista: Game-XXXXXXXX
                                                              (1/2 jogadores, modo: RMI)
                                                            → ID: Game-XXXXXXXX
                                                            Este jogo usa modo: RMI.
                                                            → GameUI do Bob abre

[Node-1099] 'bob' attached
 → fase PLACING_SHIPS
                            Fase: Colocar Navios          Fase: Colocar Navios
                            Clicar no tabuleiro           Aguarda a vez de Alice
                            para colocar 5 navios
                            (botão Rotação H/V)
                                                         Clicar para colocar 5 navios

[Node-1099] → IN_PROGRESS
                            Fase: Em Jogo                 Fase: Em Jogo
                            Clicar no tabuleiro           Aguarda
                            adversário para disparar
                                                         Clicar para disparar

                            Amarelo=HIT  Cinzento=MISS    (idem)

                            → Diálogo fim de jogo         → Diálogo fim de jogo
```

---

## 3. Requisitos Implementados

### R1 — Autenticação: Registo, Login e JWT  `2+2 valores` ✅

**Ficheiros:** `rmi/BattleshipFactory.java` · `rmi/BattleshipFactoryImpl.java` · `rmi/LobbySessionImpl.java` · `rmi/JwtUtils.java`

A interface remota `BattleshipFactory` expõe:
```java
LobbySession register(String username, String password) throws RemoteException;
LobbySession login(String username, String password)    throws RemoteException;
```

**`register()`** — valida campos não vazios, verifica que o username não existe (lança `RemoteException` se já existir), guarda em memória, chama `login()` e devolve a sessão pronta.

**`login()`** — valida credenciais, impede sessão dupla (lança `RemoteException`), gera token JWT e cria `LobbySessionImpl`.

**JWT — `JwtUtils.java`:**
- Algoritmo HS256 com chave de 32 bytes
- Expiração de 8 horas
- `generateToken(username)` → cria JWT com `sub`, `iat`, `exp`
- `getUsernameFromToken(token)` → valida assinatura e extrai username
- `isTokenValid(token)` → true/false sem lançar exceções

Após login, o cliente mostra os primeiros 30 caracteres do token JWT no terminal.

**Como demonstrar:**
1. Registar `alice/1234` → ver token JWT no terminal
2. Tentar registar `alice` novamente → `RemoteException: Utilizador já existe`
3. Tentar login com password errada → `RemoteException: Credenciais inválidas`

---

### R2 — LobbySession: Listar, Criar e Aceder a Jogos  `3 valores` ✅

**Ficheiros:** `rmi/LobbySession.java` · `rmi/LobbySessionImpl.java` · `rmi/GameInfo.java`

```java
List<GameInfo> listGames()                          // ID + nº jogadores + modo
BattleshipGameSubject createGame(String mode)       // cria jogo com modo definido
BattleshipGameSubject getProxy(String gameId)       // obtém stub RMI do jogo
void logout()
```

**`listGames()`** — percorre o mapa de jogos ativos, consulta `getPlayerCount()` e `getGameMode()` de cada jogo, devolve `List<GameInfo>` por cópia (Serializable).

**`createGame(String mode)`** — gera ID `Game-<8 chars UUID>`, cria `BattleshipGameSubjectImpl(gameId, mode)`, regista no servidor, devolve o stub RMI. O `mode` ("RMI" ou "PUBSUB") é guardado permanentemente no jogo.

**`getProxy(gameId)`** — devolve o stub; lança `RemoteException` se o jogo não existir.

**`GameInfo`** — DTO Serializable com `gameId`, `playerCount` e `mode`. Passado por cópia ao cliente.

**Como demonstrar:**
1. Alice cria jogo com modo RMI
2. Bob lista jogos → vê `Game-XXXX (1/2 jogadores, modo: RMI)`
3. Bob entra → terminal diz automaticamente `Este jogo usa modo: RMI`
4. Tentar entrar num ID inválido → `RemoteException: Jogo não encontrado`

---

### R3 — BattleshipGameSubject: Observer RMI + Pub/Sub RabbitMQ  `4+4 valores` ✅

**Ficheiros:** `rmi/BattleshipGameSubject.java` · `rmi/BattleshipGameSubjectImpl.java` · `rmi/BattleshipGameObserver.java` · `pubsub/BattleshipGamePublisher.java` · `pubsub/BattleshipGameConsumer.java` · `GameUI.java`

#### Interface `BattleshipGameSubject`

```java
void attach(String username, BattleshipGameObserver observer)  // aderir ao jogo
void detach(String username)                                    // sair do jogo
void setState(GameAction action)                                // PLACE_SHIP / FIRE / PASS
GameState getState()                                            // snapshot do estado
int getPlayerCount()
String getGameId()
String getGameMode()                                            // "RMI" ou "PUBSUB"
```

#### Ciclo de vida do jogo

```
WAITING_FOR_PLAYERS  →  2º jogador entra   →  PLACING_SHIPS
PLACING_SHIPS        →  ambos colocam 5    →  IN_PROGRESS
IN_PROGRESS          →  todos navios sunk  →  FINISHED
```

#### `BattleshipGameSubjectImpl` — todos os métodos públicos são `synchronized`

**`attach()`** — máx 2 jogadores (lança `RemoteException` se cheio); 1º jogador → `WAITING`; 2º → `PLACING_SHIPS`; chama `propagateState()`.

**`detach()`** — se jogo em curso, oponente é declarado vencedor; chama `propagateState()`.

**`setState(action)`** — verifica turno correto; delega para `handlePlaceShip` / `handleFire` / `handlePass`; chama sempre `propagateState()`.

**`handlePlaceShip()`** — valida fase, comprimento e sobreposição via `PlayerState.canPlace()`; passa turno ao oponente; quando ambos colocaram 5 navios → `IN_PROGRESS`.

**`handleFire()`** — valida fase e célula não repetida; chama `receiveShot()` no PlayerState adversário; se `isAllShipsSunk()` → `FINISHED` + vencedor; senão passa turno.

**`handlePass()`** — transfere turno para o oponente.

**`propagateState()`** — chama `notifyObserversSafe()` + `publishSafe()`.

#### Forma 1 — Observer RMI (síncrono/transiente)

Usado quando o criador escolhe modo **RMI**.

```
Servidor                               Cliente (GameUI)
   │  observer.update(GameState) ─RMI─▶  GameObserver.update()
   │                                           │
   │                                  SwingUtilities.invokeLater()
   │                                           │
   │                                     GameUI.onStateUpdate()
   │                                     (atualiza tabuleiro visual)
```

- `GameObserver` é uma inner class de `GameUI` que implementa `BattleshipGameObserver extends Remote`
- Exportada como `UnicastRemoteObject` — o servidor guarda o stub e chama `update()` após cada `setState()`
- `notifyObserversSafe()` apanha `RemoteException` por observer: se um cliente cair, o observer é removido e o jogo continua

#### Forma 2 — Publish/Subscribe RabbitMQ (assíncrono/persistente)

Usado quando o criador escolhe modo **PUBSUB**.

**Servidor — `BattleshipGamePublisher`:**
- Exchange fanout durável: `battleship.<gameId>`
- `publish(GameState)` → serializa para `byte[]` e publica com flag `PERSISTENT`
- Fechado automaticamente na fase `FINISHED`
- Só é criado se `gameMode == "PUBSUB"` (não tenta RabbitMQ em modo RMI)

**Cliente — `BattleshipGameConsumer`:**
- Fila durável por jogador: `battleship.<gameId>.<username>`
- Bind ao exchange do jogo
- `start(Consumer<GameState>)` → callback chamado em thread RabbitMQ
  - Em `GameUI`: `state -> SwingUtilities.invokeLater(() -> onStateUpdate(state))`
- `basicQos(1)` + `basicAck`/`basicNack`
- Fila persistente: cliente pode desligar e ao voltar recebe mensagens perdidas

Em modo PUBSUB, o cliente ainda faz `attach()` mas com um `NullObserver` (sem-ops). As atualizações chegam exclusivamente via RabbitMQ.

| | Observer RMI | Pub/Sub RabbitMQ |
|---|---|---|
| Comunicação | Síncrona | Assíncrona |
| Persistência | Transiente | Persistente (fila durável) |
| Servidor bloqueia | Sim | Não |
| Cliente pode desligar e reconectar | Não | Sim |

**Como demonstrar:**

*Modo RMI:*
1. Criar jogo com opção 1 (RMI)
2. Ambos jogadores entram → GUIs abrem com título `[Observer/RMI]`
3. Alice coloca navio → tabuleiro do Bob atualiza em tempo real

*Modo PubSub (requer RabbitMQ):*
```bash
sudo systemctl start rabbitmq-server
```
1. Criar jogo com opção 2 (PUBSUB)
2. GUIs abrem com título `[PubSub/RabbitMQ]`
3. No terminal do servidor: `RabbitMQ publisher ativo para Game-XXXX`

---

### R4 — Gestão de 2 Jogadores e Alternância de Turnos  `2 valores` ✅

**Ficheiros:** `rmi/BattleshipGameSubjectImpl.java` · `rmi/GameState.java` · `rmi/PlayerState.java` · `GameUI.java`

**Máximo 2 jogadores** — em `attach()`:
```java
if (observersByUser.size() >= 2)
    throw new RemoteException("Game is already full (max 2 players).");
```

**Jogo só começa com 2** — em `setState()`:
```java
if (gameState.getPhase() == GamePhase.WAITING_FOR_PLAYERS)
    throw new RemoteException("Aguardando por 2 jogadores antes de começar.");
```

**Enforcement de turno** — em `setState()`:
```java
if (!gameState.getCurrentTurn().equals(username))
    throw new RemoteException("Não é a vez de '" + username + "'.");
```

**Alternância automática** — após cada ação válida, `currentTurn` é transferido via `getOpponent(username)`.

**Na GameUI** — botões ativos/inativos conforme o turno:
- Fase `PLACING_SHIPS` + turno do jogador → campo próprio ativo
- Fase `IN_PROGRESS` + turno do jogador → campo adversário ativo
- Fora do turno → todos os botões desativados

#### Fases e transições

| Fase | Quem age | Ação | Próxima fase |
|---|---|---|---|
| `WAITING_FOR_PLAYERS` | — | — | 2º jogador → `PLACING_SHIPS` |
| `PLACING_SHIPS` | `currentTurn` | Clicar no campo próprio | Ambos 5 navios → `IN_PROGRESS` |
| `IN_PROGRESS` | `currentTurn` | Clicar no campo adversário | Todos navios afundados → `FINISHED` |
| `FINISHED` | — | — | Diálogo de fim de jogo |

#### Navios por jogador (ordem obrigatória)

| # | Comprimento |
|---|---|
| 1 | 5 células |
| 2 | 4 células |
| 3 | 3 células |
| 4 | 3 células |
| 5 | 2 células |

---

### R5 — Escalabilidade Horizontal e Tolerância a Falhas  `3 valores` ✅

**Ficheiros:** `rmi/BattleshipFactoryPeer.java` · `rmi/BattleshipFactoryImpl.java` · `Server/ServerApp.java`

#### Interface `BattleshipFactoryPeer`

```java
void syncRegister(String username, String password) // replica registo
void syncLogout(String username)                    // replica logout
boolean ping()                                      // health-check
```

Cada nó regista-se no RMI Registry com dois nomes:
- `BattleshipFactory` → para os clientes
- `BattleshipFactoryPeer` → para o nó par

#### `BattleshipFactoryImpl` — implementa ambas as interfaces

```java
public class BattleshipFactoryImpl extends UnicastRemoteObject
        implements BattleshipFactory, BattleshipFactoryPeer
```

Quando um utilizador se regista no Nó A:
1. Guarda localmente em `users` (HashMap em memória)
2. Chama `peer.syncRegister()` no Nó B
3. Se o Nó B não responder → `peer = null`, continua a funcionar sozinho

**Replicação melhor-esforço** — não bloqueia o cliente em caso de falha do peer.

#### `ServerApp` — arranque dos nós

```bash
# Nó A
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp \
    -Dexec.args="--port 1099 --peer localhost:1100"

# Nó B
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp \
    -Dexec.args="--port 1100 --peer localhost:1099"
```

Quando o servidor arranca com `--peer`:
1. Inicia um **thread daemon** que tenta ligar ao peer (10 tentativas × 3 s)
2. Após ligar, inicia um **thread de monitorização** que faz `ping()` de 5 em 5 s
3. Se o peer não responder → `peer = null` → o servidor continua autónomo
4. O thread volta a tentar reconectar

#### Como demonstrar na defesa

1. Arrancar Nó A e Nó B → ver `Peer ligado. Replicação ativa.` em ambos
2. Registar `alice` no Nó A → ver `[SYNC] Utilizador replicado: alice` no Nó B
3. **Matar o Nó A** (Ctrl+C)
4. Ligar cliente ao Nó B (`--port 1100` no `ClientApp`)
5. Fazer login com `alice/1234` no Nó B → funciona (dados replicados)

---

## 4. Interface Gráfica (GameUI)

**Ficheiro:** `GameUI.java` (pacote `edu.ufp.inf.sd.battleshipgame`)

#### Fluxo terminal → GUI

```
Terminal                                          GUI
────────                                          ───
Login → Lobby
  → Criar jogo → escolher modo (RMI/PUBSUB) ──▶  GameUI abre (Alice)
  → Entrar no jogo → modo automático do servidor ▶ GameUI abre (Bob)
Terminal bloqueia até fechar a janela
Fechar GameUI → volta ao lobby no terminal
```

#### Layout da GameUI

```
┌─────────────────────────────────────────────────────────┐
│  Battleship — alice  |  Jogo: Game-XXXX  [Observer/RMI]  │
│  Fase: Colocar Navios  |  A TUA VEZ de colocar navios!   │
├────────────────────┬────────────────────────────────────┤
│   O Teu Campo      │    Campo do Adversário              │
│   10×10 clicável   │    10×10 clicável                   │
│   (colocar navios) │    (disparar)                       │
├────────────────────┴────────────────────────────────────┤
│  Próximo navio: 5 células | Orientação: Horizontal        │
│  [Orientação: H/V]  [Passar Vez]                          │
├─────────────────────────────────────────────────────────┤
│  Log de Eventos:                                          │
│  alice joined · bob joined · alice placed ...             │
└─────────────────────────────────────────────────────────┘
```

#### Legenda de cores

| Cor | Significado |
|---|---|
| Verde | Navio próprio intacto |
| Vermelho | Navio próprio atingido |
| Cinzento | Tiro falhado |
| Amarelo | Acerto no adversário |

#### Mecanismo interno

- **Modo RMI**: `GameObserver` (inner class, `UnicastRemoteObject`) recebe `update(GameState)` e chama `SwingUtilities.invokeLater(() -> onStateUpdate(state))`
- **Modo PUBSUB**: `BattleshipGameConsumer.start(state -> invokeLater(() -> onStateUpdate(state)))` em thread background; `NullObserver` passado ao `attach()` (obrigatório mas sem lógica)
- **`onStateUpdate()`**: atualiza tabuleiros, status, log, ativa/desativa botões conforme fase e turno
- Fechar janela → `game.detach(username)` + `consumer.close()` automáticos

---

## 5. Como Correr o Projeto

### Pré-requisitos

- Java 17+ · Maven 3.x
- RabbitMQ apenas se escolher modo PUBSUB

```bash
# Instalar e iniciar RabbitMQ (Ubuntu/Debian)
sudo apt install rabbitmq-server
sudo systemctl start rabbitmq-server
```

### Comandos

```bash
# 1. Compilar
mvn compile

# 2. Testes automáticos (sem servidor, sem RabbitMQ)
mvn test

# 3. Servidor simples (1 nó)
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp

# 4. Servidor com 2 nós (R5)
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp \
    -Dexec.args="--port 1099 --peer localhost:1100"

mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp \
    -Dexec.args="--port 1100 --peer localhost:1099"

# 5. Cliente
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Client.ClientApp
```

---

## 6. Testes Automáticos

**Ficheiro:** `src_test/.../R3GameLogicTest.java`

Testes JUnit 5 que correm diretamente sobre `BattleshipGameSubjectImpl` (sem RMI nem RabbitMQ):

| Teste | O que verifica |
|---|---|
| Attach de 2 jogadores | Fase → `PLACING_SHIPS` |
| Colocação dos 5 navios | Fase → `IN_PROGRESS` após ambos colocarem |
| Enforcement de turno | `RemoteException` se jogar fora da vez |
| Disparo HIT/MISS | `PlayerState.hitsReceived` correto |
| Vitória | Fase → `FINISHED` quando todos os navios afundados |
| Desconexão a meio | Oponente declarado vencedor |

```bash
mvn test
# Tests run: 1, Failures: 0, Errors: 0
```

---

## 7. Perguntas Prováveis na Defesa

**"Porque é que usaram RMI e não sockets?"**
> RMI abstrai a rede e permite chamar métodos remotos como locais. O servidor exporta objetos (`BattleshipFactory`, `LobbySession`, `BattleshipGameSubject`) e o cliente recebe stubs que fazem marshalling/unmarshalling automaticamente. Com sockets teríamos de implementar o protocolo de serialização manualmente.

**"O que é um stub RMI?"**
> É um proxy local gerado pelo RMI que implementa a mesma interface do objeto remoto. Quando o cliente chama `jogo.setState(action)`, o stub serializa os argumentos, envia pela rede para o servidor, e devolve o resultado deserializado.

**"Qual a diferença entre os dois modos?"**
> Observer RMI é síncrono: o servidor bloqueia até o cliente responder ao `update()`. Se o cliente desligar, perde as notificações. PubSub RabbitMQ é assíncrono: o servidor publica sem esperar; as mensagens ficam nas filas duráveis e o cliente pode desligar e voltar a recebê-las.

**"Como é que o modo é garantido igual nos dois clientes?"**
> O criador escolhe o modo ao criar o jogo — fica guardado no servidor em `BattleshipGameSubjectImpl.gameMode`. Quando o segundo jogador entra com `getProxy()`, chama `getGameMode()` e usa automaticamente o mesmo modo. Não há hipótese de conflito.

**"Porque é que o GameState é Serializable e não Remote?"**
> Se fosse Remote, cada acesso a um campo seria uma chamada de rede — latência elevada. Como é Serializable, é enviado por cópia completa ao cliente que o usa localmente. É um snapshot — faz sentido ser uma cópia.

**"Como é que garantem thread-safety no jogo?"**
> Todos os métodos públicos de `BattleshipGameSubjectImpl` são `synchronized`. Além disso, `setState()` verifica `currentTurn.equals(username)` antes de processar — um jogador fora de turno recebe `RemoteException`.

**"O que acontece se um cliente se desligar a meio?"**
> Em modo RMI: na próxima chamada `observer.update()`, o servidor apanha `RemoteException`, remove o observer e declara o oponente vencedor. Em modo PubSub: as mensagens ficam na fila durável até o cliente reconectar; o servidor não sabe que o cliente desligou.

**"Como funciona o R5?"**
> Cada nó implementa `BattleshipFactoryPeer` e regista-se no RMI Registry. Quando um utilizador se regista no Nó A, é chamado `peer.syncRegister()` no Nó B. Se o Nó B cair, a chamada falha silenciosamente e o Nó A continua sozinho. Um thread de monitorização deteta a falha e tenta reconectar de 5 em 5 segundos.
