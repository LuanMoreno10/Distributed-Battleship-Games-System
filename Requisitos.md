# R1

 *BattleshipFactoryImpl* — corrigido:

    - register() valida campos vazios e lança RemoteException se o utilizador já existir

    - login() lança RemoteException se as credenciais forem inválidas ou se já houver sessão ativa

    - Adicionado removeSession() para limpeza de sessões

*ClientApp.java* — criado de raiz:

    - Liga-se ao RMI Registry (localhost:1099) e faz lookup da BattleshipFactory

    - Menu de autenticação: Registar (username + password → LobbySession) ou Login

    - Após autenticação bem-sucedida, entra no menu do Lobby:
        -> Listar jogos disponíveis
        -> Criar novo jogo
        -> Entrar num jogo existente (por ID)
    - Erros são apresentados ao utilizador com mensagens descritivas (vindas do servidor via RemoteException)

Para testar: correr primeiro o ServerApp, depois o ClientApp.

# R2

------------------------------------------------------------------------------

O problema do listGames()

Antes devolvia List<String> com apenas os IDs. O R2 exige também o número de jogadores — mas RMI não permite passar um objeto arbitrário a não ser que seja Remote (proxy) ou Serializable (cópia).

2 Soluções:

    -> Remote: O servidor envia uma lista de "comandos à distância" (stubs). O cliente recebe apenas uma forma de contactar o objeto real no servidor.

    -> Serializable (Usado): Nesta abordagem, o servidor tira uma "fotografia" do estado do jogo e envia-a para o cliente.

    
------------------------------------------------------------------------------

O que foi criado/alterado:

*GameInfo.java* — novo objeto serializável (implements Serializable) com gameId e playerCount. É passado por cópia do servidor para o cliente via RMI. O toString() devolve "Game-ABC (1/2 jogadores)".

*BattleshipGameSubject.java* — adicionado getPlayerCount() à interface remota, implementado em BattleshipGameSubjectImpl como observers.size().

*LobbySession.java* — listGames() passou de List<String> para List<GameInfo>.

*LobbySessionImpl.java* — listGames() itera os jogos ativos, consulta o getPlayerCount() de cada um, e constrói os GameInfo. getProxy() agora lança RemoteException em vez de retornar null. O ID do jogo ficou com 8 caracteres em maiúsculas para ser mais legível.

*ClientApp.java* — listarJogos() usa List<GameInfo> e o toString() já formata tudo automaticamente.

# R3

------------------------------------------------------------------------------

## O que o R3 pede

Cada `BattleshipGameSubject` deve:

1. Permitir **associar/desassociar** um par de jogadores ao jogo (`attach/detach`).
2. Gerir o desenrolar do jogo e a respetiva propagação do estado (`setState/getState`).
3. Só iniciar o jogo depois de **2 jogadores** se associarem.
4. Implementar a propagação de atualizações de **duas formas** (obrigatório):
   - **Observer** síncrono/transiente (RMI callbacks)
   - **Publish/Subscribe** assíncrono/persistente (RabbitMQ)

------------------------------------------------------------------------------

## Implementação feita

### 1) Observer (RMI)

* `BattleshipGameSubject`:
  - `attach(String username, BattleshipGameObserver observer)`
  - `detach(String username)`
  - `setState(GameAction action)`
  - `getState(): GameState`

* `BattleshipGameSubjectImpl`:
  - Aceita no máximo **2 jogadores**.
  - Mantém um `GameState` serializável.
  - Após cada `setState()`, chama `observer.update(GameState)` para ambos.

* Novos DTOs serializáveis:
  - `GameState`, `PlayerState`, `GameAction`, `ShipPlacement`, `Shot`, etc.

### 2) Publish/Subscribe (RabbitMQ)

* `BattleshipGamePublisher` (server-side) publica snapshots `GameState` num exchange `fanout`:
  - Exchange: `battleship.<gameId>`
  - Mensagens persistentes (`deliveryMode=2`)

* `BattleshipGameConsumer` (client-side) consome de uma fila durável por jogador:
  - Queue: `battleship.<gameId>.<username>`
  - Bind ao exchange do jogo

O servidor pode ser iniciado com `--pubsub` para ativar a publicação adicional via RabbitMQ.

------------------------------------------------------------------------------

## Como executar (CLI)

### Compilar

```bash
cd /Users/luanmoreno18/Desktop/BattleshipGame
mvn -q test
```

### Servidor

```bash
# só RMI (Observer)
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp

# RMI + RabbitMQ Pub/Sub
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Server.ServerApp -Dexec.args="--pubsub"
```

### Cliente (2 instâncias)

```bash
mvn -q exec:java -Dexec.mainClass=edu.ufp.inf.sd.battleshipgame.Client.ClientApp
```

No menu do jogo, escolhe:
1) Observer (RMI callbacks) **ou** 2) Pub/Sub (RabbitMQ).


