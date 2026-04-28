# R1

Tudo implementado. Aqui está o resumo do que foi feito para o R1:

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

