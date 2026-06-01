# Guia de Defesa — Distributed Battleship Games System

**Sistemas Distribuídos · UFP/FCT · 2026**

---

## Como correr o projeto

```bash
# Terminal 1 — Python (codebase RMI)
cd runscripts && bash _1_runpython.sh

# Terminal 2 — Servidor
cd runscripts && bash _2_runserver.sh

# Terminal 3 — Cliente 1
cd runscripts && bash _3_runclient.sh

# Terminal 4 — Cliente 2
cd runscripts && bash _3_runclient.sh
```

**Para R5 (2 nós):**
```bash
bash _2_runserver.sh    # Terminal 2 — Nó A (arranca primeiro)
bash _2_runserverB.sh   # Terminal 3 — Nó B (liga-se ao A automaticamente)
```

---

## 1. Visão Geral — O que o projeto faz

É um sistema distribuído de Batalha Naval onde:

1. Os clientes registam-se e fazem login num servidor central via **Java RMI**
2. Um cliente cria um jogo e escolhe o modo de comunicação (**RMI** ou **RabbitMQ**)
3. Outro cliente entra no jogo — usa automaticamente o mesmo modo
4. O jogo abre numa **interface gráfica** (GameUI) para ambos
5. As atualizações de estado propagam-se via RMI (síncrono) ou RabbitMQ (assíncrono)
6. Se um servidor cair, o outro continua com os dados replicados (R5)

---

## 2. Requisito R1 — Autenticação e JWT

### O que está implementado

**Ficheiros:**
- `src/.../rmi/BattleshipFactory.java` — interface RMI
- `src/.../rmi/BattleshipFactoryImpl.java` — implementação (linha 34: register, linha 51: login)
- `src/.../rmi/JwtUtils.java` — tokens JWT
- `src/.../rmi/LobbySessionImpl.java` — sessão criada após login

### Fluxo no código

```
ClientApp.autenticar()          [Client/ClientApp.java : linha 105]
    └── factory.register()      [rmi/BattleshipFactory.java]
            └── BattleshipFactoryImpl.register()  [linha 34]
                    ├── users.put(username, password)     [guarda em memória]
                    ├── replicateRegister()               [linha 120 — replica para peer R5]
                    └── login()                           [linha 51]
                            ├── JwtUtils.generateToken()  [gera token JWT]
                            └── new LobbySessionImpl()    [cria sessão]
```

### JWT — onde é gerado e usado

```java
// JwtUtils.java
generateToken(username)    // cria JWT com subject, iat, exp (+8h), assina HS256
getUsernameFromToken(token) // valida e extrai username
isTokenValid(token)         // true/false
```

O token é gerado em `BattleshipFactoryImpl.login()` linha 59 e devolvido ao cliente que o mostra no terminal.

---

## 3. Requisito R2 — LobbySession

### O que está implementado

**Ficheiros:**
- `src/.../rmi/LobbySession.java` — interface RMI
- `src/.../rmi/LobbySessionImpl.java` — implementação
- `src/.../rmi/GameInfo.java` — DTO com ID, nº jogadores e modo

### Fluxo no código

```
ClientApp.menuLobby()              [ClientApp.java : linha 164]
    ├── session.listGames()        [LobbySessionImpl.java : lista jogos com GameInfo]
    ├── session.createGame(mode)   [LobbySessionImpl.java : cria jogo com modo RMI/PUBSUB]
    │       └── new BattleshipGameSubjectImpl(gameId, mode)
    └── session.getProxy(gameId)   [LobbySessionImpl.java : devolve stub RMI do jogo]
```

### Porquê GameInfo é Serializable e não Remote

Em RMI há dois tipos de objetos:
- **Remote** → o objeto fica no servidor, o cliente recebe um stub (proxy)
- **Serializable** → o objeto é copiado por completo para o cliente

`GameInfo` é uma "fotografia" simples (ID + nº jogadores + modo) — não precisa de ficar no servidor. É mais eficiente enviar por cópia.

---

## 4. Requisito R3 — Observer RMI + Pub/Sub RabbitMQ

### O que está implementado

**Ficheiros:**
- `src/.../rmi/BattleshipGameSubjectImpl.java` — lógica do jogo (todos os métodos synchronized)
- `src/.../rmi/BattleshipGameObserver.java` — interface do callback
- `src/.../pubsub/BattleshipGamePublisher.java` — publica no RabbitMQ (servidor)
- `src/.../pubsub/BattleshipGameConsumer.java` — consome do RabbitMQ (cliente)
- `src/.../GameUI.java` — recebe atualizações e atualiza o tabuleiro visual

### Ciclo de vida do jogo

```
WAITING_FOR_PLAYERS  →  2º jogador entra (attach)  →  PLACING_SHIPS
PLACING_SHIPS        →  ambos colocam 5 navios      →  IN_PROGRESS
IN_PROGRESS          →  todos navios afundados       →  FINISHED
```

### Fluxo do Observer RMI

```
GameUI.attachToGame()                      [GameUI.java : linha 218]
    └── game.attach(username, GameObserver) [BattleshipGameSubjectImpl : linha 41]
            └── propagateState()            [linha 241]
                    └── notifyObserversSafe() [linha 259]
                            └── observer.update(gameState)  ← RMI call ao cliente!
                                    └── GameObserver.update() [GameUI.java : linha 453]
                                            └── SwingUtilities.invokeLater(onStateUpdate)

Jogador faz jogada:
GameUI.onMyBoardClick()                    [GameUI.java : linha 396]
    └── game.setState(GameAction.placeShip) [RMI call ao servidor]
            └── BattleshipGameSubjectImpl.setState() [linha 111]
                    ├── handlePlaceShip() / handleFire() / handlePass()
                    └── propagateState()   → observer.update() para TODOS os jogadores
```

### Fluxo do Pub/Sub RabbitMQ

```
Servidor — quando o jogo é criado (modo PUBSUB):
BattleshipGameSubjectImpl construtor [linha 21]
    └── new BattleshipGamePublisher(gameId)
            ├── cria ligação ao RabbitMQ (localhost:5672)
            └── cria exchange fanout durável: "battleship.Game-XXXX"

Servidor — após cada setState():
propagateState() → publishSafe() [linha 278]
    └── publisher.publish(gameState)
            └── serializa GameState para bytes
            └── publica no exchange "battleship.Game-XXXX"
            └── RabbitMQ entrega às filas de TODOS os jogadores

Cliente — quando entra no jogo (modo PUBSUB):
GameUI.attachToGame() [linha 218]
    ├── new BattleshipGameConsumer(gameId, username)
    │       ├── cria ligação ao RabbitMQ
    │       ├── cria fila durável: "battleship.Game-XXXX.alice"
    │       └── bind da fila ao exchange do jogo
    ├── consumer.start(state → onStateUpdate(state))
    │       └── basicConsume() — ouve mensagens em background thread
    └── game.attach(username, NullObserver)  ← attach obrigatório mas sem callbacks
```

### Diferença entre os dois modos

| | Observer RMI | Pub/Sub RabbitMQ |
|---|---|---|
| Servidor chama | `observer.update()` diretamente | `publisher.publish()` → broker → consumer |
| Cliente recebe em | Thread RMI | Thread RabbitMQ (background) |
| Se cliente desligar | Perde atualizações | Fila guarda as mensagens |
| Servidor bloqueia | Sim (espera resposta) | Não (publica e continua) |

---

## 5. Requisito R4 — 2 Jogadores e Turnos

### O que está implementado

**Ficheiro:** `src/.../rmi/BattleshipGameSubjectImpl.java`

```java
// attach() linha 41 — máximo 2 jogadores
if (observersByUser.size() >= 2)
    throw new RemoteException("Game is already full");

// setState() linha 111 — só joga quem tem o turno
if (!gameState.getCurrentTurn().equals(username))
    throw new RemoteException("Não é a vez de...");
```

### Transições automáticas de fase

```
attach() do 2º jogador    → fase: PLACING_SHIPS   [linha 70]
Ambos colocam 5 navios    → fase: IN_PROGRESS     [handlePlaceShip : linha 174]
Todos navios afundados    → fase: FINISHED         [handleFire : linha 216]
Jogador sai a meio        → oponente ganha         [detach : linha 94]
```

### Na GameUI — botões ativados por turno

```java
// updateButtonStates() [GameUI.java : linha 363]
boolean placing = phase == PLACING_SHIPS && myTurn;
boolean firing  = phase == IN_PROGRESS  && myTurn;

myBoard.btnGrid[r][c].setEnabled(placing && !alreadyPlaced);   // colocar navios
enemyBoard.btnGrid[r][c].setEnabled(firing && !alreadyShot);   // disparar
```

---

## 6. Requisito R5 — Escalabilidade e Tolerância a Falhas

### O que está implementado

**Ficheiros:**
- `src/.../rmi/BattleshipFactoryPeer.java` — interface de sincronização
- `src/.../rmi/BattleshipFactoryImpl.java` — implementa replicação (linha 76: syncRegister)
- `src/.../Server/ServerApp.java` — thread de ligação e monitorização ao peer

### Fluxo de replicação

```
Cliente regista alice no Nó A:
BattleshipFactoryImpl.register() [linha 34]
    ├── users.put("alice", "1234")        ← guarda no Nó A
    └── replicateRegister() [linha 120]
            └── peer.syncRegister("alice","1234")  ← RMI call ao Nó B!
                    └── BattleshipFactoryImpl.syncRegister() [linha 76]
                            └── users.put("alice","1234")   ← guarda no Nó B também
```

### Ligação bidirecional automática

```
_2_runserver.sh arranca (Nó A, sem peer)
_2_runserverB.sh arranca (Nó B, --peer localhost:1099)

ServerApp.connectToPeer() [ServerApp.java]
    ├── conecta ao registry do Nó A
    ├── factory.setPeer(peerA)     ← Nó B passa a replicar para A
    └── peerA.registerAsPeer(factoryB, users)  ← Nó A passa a replicar para B
            └── BattleshipFactoryImpl.registerAsPeer() [linha 94]
                    ├── setPeer(caller)           ← Nó A agora conhece o Nó B
                    └── sincroniza utilizadores nos dois sentidos
```

### Thread de monitorização

```java
// ServerApp.monitorPeer()
while (true) {
    Thread.sleep(5000);         // verifica de 5 em 5 segundos
    factory.getPeerRef().ping() // se falhar → peer = null → reconecta
}
```

### Failover automático no cliente

```java
// ClientApp.menuLobby() [linha 164]
} catch (RemoteException e) {
    // servidor caiu
    BattleshipFactory backup = ligarAoMelhorNo(new String[0]);
    LobbySession novaSession = backup.login(credenciais[0], credenciais[1]);
    menuLobby(scanner, novaSession, credenciais);  // continua no backup
}
```

### Balanceamento de carga

```java
// ClientApp.ligarAoMelhorNo() [linha 58]
// Tenta todos os servidores conhecidos e escolhe o com menos sessões
for (String[] srv : DEFAULT_SERVERS) {
    int carga = f.getSessionCount();   // getSessionCount() via RMI
    if (carga < menorCarga) melhor = f;
}
```

---

## 7. Perguntas e Respostas para a Defesa

---

### PERGUNTAS SOBRE RMI

**"O que é o RMI e como funciona?"**
> RMI (Remote Method Invocation) é uma tecnologia Java que permite chamar métodos de objetos que estão noutro processo ou máquina. O servidor exporta objetos remotos e regista-os num Registry (porta 1099). O cliente faz lookup no Registry e recebe um **stub** — um proxy local que implementa a mesma interface. Quando o cliente chama um método no stub, este serializa os argumentos, envia pela rede, o servidor executa o método e devolve o resultado.

**"O que é um stub RMI?"**
> É um proxy gerado automaticamente que fica no cliente mas implementa a interface remota. Quando o cliente chama `jogo.setState(action)`, não está a executar nada localmente — o stub serializa `action`, envia para o servidor via TCP, o servidor executa `BattleshipGameSubjectImpl.setState()` e devolve o resultado. É transparente para o código.

**"Onde é criado o RMI Registry?"**
> Em `ServerApp.java` — `LocateRegistry.createRegistry(1099)`. O registry é criado no mesmo JVM do servidor para que as classes sejam encontradas. Se usássemos um registry externo (`rmiregistry`), ele não teria as classes no classpath e lançaria `ClassNotFoundException`.

**"O que é o codebase RMI e para que serve o Python?"**
> O codebase é um URL onde o servidor publica as suas classes para que o cliente as possa descarregar dinamicamente. O Python serve um servidor HTTP simples na pasta `target/classes/` — quando o cliente recebe um stub do servidor, a JVM do cliente pode descarregar as classes necessárias via HTTP se não as tiver localmente.

**"Porque é que os métodos do BattleshipGameSubjectImpl são synchronized?"**
> Porque múltiplos clientes podem chamar métodos em simultâneo — cada chamada RMI vem num thread separado. Sem `synchronized`, dois jogadores podiam fazer `setState()` ao mesmo tempo, causando condições de corrida no estado do jogo. O `synchronized` garante que só um thread executa de cada vez.

---

### PERGUNTAS SOBRE RABBITMQ

**"O que é o RabbitMQ e como funciona no projeto?"**
> RabbitMQ é um message broker — um intermediário que recebe mensagens de um publicador e entrega a filas de consumidores. No projeto, o servidor é o **publicador** e os clientes são os **consumidores**. Após cada jogada, o servidor publica o novo `GameState` no exchange do jogo. O RabbitMQ entrega às filas de cada jogador. Cada cliente tem um consumer a ouvir a sua fila em background.

**"O que é um exchange fanout?"**
> Um exchange fanout distribui cada mensagem recebida para **todas** as filas que estão ligadas a ele. No projeto, o exchange `battleship.Game-XXXX` envia cada `GameState` para a fila de Alice E para a fila de Bob simultaneamente. Se usássemos direct ou topic, tínhamos de especificar a routing key — fanout é mais simples para broadcast.

**"O que são filas duráveis e porque as usam?"**
> Filas duráveis sobrevivem a reinicializações do RabbitMQ. No projeto as filas `battleship.Game-XXXX.alice` são declaradas com `durable=true`. Se o broker reiniciar, as filas existem. Mais importante: se o cliente desligar temporariamente, as mensagens ficam na fila e são entregues quando o cliente voltar a ligar — ao contrário do Observer RMI que as perderia.

**"Porque é que em modo PubSub passam um NullObserver ao attach()?"**
> O `attach()` é obrigatório para participar no jogo — regista o jogador no estado, define a fase, etc. Mas em modo PubSub não queremos receber atualizações via RMI (usamos RabbitMQ). O `NullObserver` satisfaz o contrato da interface sem fazer nada quando `update()` é chamado.

**"O que é basicQos(1)?"**
> É um limite de prefetch — o broker só entrega 1 mensagem não confirmada de cada vez ao consumer. O consumer processa, chama `basicAck()` para confirmar, e só então recebe a próxima. Evita que o consumer seja inundado com mensagens se o processamento for lento.

---

### PERGUNTAS SOBRE DESIGN

**"Que padrões de design usaram?"**
> Usámos três: **Factory Method** — a `BattleshipFactory` cria `LobbySession` sem o cliente saber a classe concreta. **Observer** — o servidor notifica os clientes via `BattleshipGameObserver.update()` após cada setState. **Publish/Subscribe** — variante assíncrona do Observer onde o servidor publica no RabbitMQ e os clientes consomem independentemente.

**"Porque é que o GameState é Serializable e não Remote?"**
> Se fosse Remote, cada acesso a um campo seria uma chamada de rede — lento e complexo. Como é Serializable, é enviado por cópia completa ao cliente que o usa localmente. Faz sentido porque é um snapshot imutável do estado num momento — não precisa de ser um proxy de um objeto vivo no servidor.

**"O que é o padrão DTO e onde usam?"**
> Data Transfer Object — objetos simples Serializable que servem apenas para transferir dados entre camadas. Usamos `GameState`, `GameAction`, `GameInfo`, `ShipPlacement`, `Shot`, `Coordinate` — todos Serializable, sem lógica de negócio, enviados por cópia via RMI.

---

### PERGUNTAS SOBRE JWT

**"O que é JWT e porque o usam?"**
> JWT (JSON Web Token) é um token de autenticação assinado digitalmente. Após o login, o servidor gera um token com `subject=username`, data de emissão e expiração (8h), assinado com chave HS256. O cliente recebe e usa esse token como prova de identidade. Sem o token, o servidor pode verificar se uma sessão é legítima sem precisar de consultar a base de dados.

**"Onde é gerado e validado o JWT?"**
> Gerado em `BattleshipFactoryImpl.login()` linha 59 — `JwtUtils.generateToken(username)`. Armazenado na `LobbySessionImpl`. O cliente pode ver os primeiros 30 caracteres no terminal após o login.

---

### PERGUNTAS SOBRE R5

**"Como funciona a replicação?"**
> Cada servidor implementa `BattleshipFactoryPeer` com `syncRegister()` e `syncLogout()`. Quando um utilizador se regista no Nó A, após guardar localmente, é chamado `peer.syncRegister()` via RMI no Nó B. O Nó B guarda o utilizador. Se o peer não responder, a replicação falha silenciosamente mas o Nó A continua a funcionar.

**"O que acontece quando o servidor primário cai?"**
> O thread de monitorização no Nó B deteta que o `ping()` falhou → marca `peer = null`. O cliente, na próxima chamada RMI, recebe `RemoteException` → o `catch` em `menuLobby()` tenta `ligarAoMelhorNo()` → conecta ao Nó B → faz `login()` com as credenciais guardadas → a sessão é restaurada automaticamente.

**"O que é o balanceamento de carga?"**
> Ao arrancar, o cliente tenta ligar a todos os servidores conhecidos (1099 e 1100), consulta `getSessionCount()` em cada um e escolhe o com menos sessões ativas. Se um estiver em baixo, usa o outro automaticamente — sem intervenção do utilizador.

**"O que não é replicado?"**
> Os jogos em curso (`BattleshipGameSubjectImpl`) não são replicados — ficam em memória no servidor que os criou. O protocolo só pede replicação de dados de utilizadores. Se o servidor que tem o jogo cair, esse jogo perde-se. O serviço em si (login, criar novo jogo) continua no outro nó.

---

### PERGUNTAS SOBRE A INTERFACE GRÁFICA

**"Como funciona a GameUI?"**
> A GameUI abre após o cliente entrar num jogo. Tem dois tabuleiros 10×10: o campo próprio (navios + tiros recebidos) e o campo adversário (onde se dispara). Os botões são ativados/desativados automaticamente com base na fase e no turno. Em modo RMI, um `GameObserver` recebe `update()` do servidor e chama `onStateUpdate()` na EDT via `SwingUtilities.invokeLater()`. Em modo PubSub, um consumer RabbitMQ faz o mesmo.

**"Porque é que usam SwingUtilities.invokeLater()?"**
> Swing não é thread-safe — só o Event Dispatch Thread (EDT) pode modificar componentes. As callbacks RMI e RabbitMQ chegam em threads separados. `invokeLater()` agenda a execução na EDT de forma segura.

---

## 8. O que demonstrar passo a passo

### Demo R1 + R2 (3 min)
1. Arrancar servidor → mostrar "Pronto"
2. Cliente 1: registar `alice/1234` → mostrar JWT no terminal
3. Tentar registar `alice` novamente → mostrar erro "já existe"
4. Cliente 2: registar `bob/5678`
5. Alice: listar jogos → vazio
6. Alice: criar jogo com modo RMI → mostrar ID
7. Bob: listar jogos → vê `Game-XXXX (1/2 jogadores, modo: RMI)`

### Demo R3 + R4 (5 min)
8. Bob: entrar no jogo → GameUIs abrem nos dois clientes
9. Alice coloca navios → Bob vê o tabuleiro atualizar em tempo real
10. Bob coloca navios → Alice vê
11. Alice dispara → amarelo no campo adversário de Alice, log atualiza em Bob
12. Tentar disparar quando não é a vez → mostrar erro no GameUI

### Demo RabbitMQ (3 min)
13. Arrancar RabbitMQ: `sudo systemctl start rabbitmq-server`
14. Criar novo jogo com modo PUBSUB
15. Mostrar no servidor: `RabbitMQ publisher ativo`
16. Mostrar no browser `http://localhost:15672`: exchange + 2 queues + consumers
17. Jogar uma jogada → ver spike no gráfico Message rates

### Demo R5 (3 min)
18. Arrancar Nó A: `bash _2_runserver.sh`
19. Registar `alice/1234` no Nó A
20. Arrancar Nó B: `bash _2_runserverB.sh`
21. Mostrar: `Peer bidirecional estabelecido. Utilizadores sincronizados. alice replicada`
22. Matar o Nó A (Ctrl+C)
23. Cliente tenta jogar → mensagem `[R5] A procurar servidor de backup...`
24. Cliente reconecta automaticamente ao Nó B
25. Login com `alice/1234` no Nó B → funciona!
