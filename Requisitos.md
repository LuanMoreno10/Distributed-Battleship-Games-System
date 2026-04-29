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

