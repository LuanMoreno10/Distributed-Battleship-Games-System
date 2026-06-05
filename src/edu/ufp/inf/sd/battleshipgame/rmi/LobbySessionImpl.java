package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LobbySessionImpl extends UnicastRemoteObject implements LobbySessionRI {
    private final String username;
    private final String token;
    private final BattleshipFactoryImpl factory;

    public LobbySessionImpl(String username, String token, BattleshipFactoryImpl factory) throws RemoteException {
        super();
        this.username = username;
        this.token = token;
        this.factory = factory;
    }

    private void validateToken(String token) throws RemoteException {
        if (!JwtUtils.isTokenValid(token) || !JwtUtils.getUsernameFromToken(token).equals(username)) {
            throw new RemoteException("Token inválido ou não autorizado.");
        }
    }

    @Override
    public String getUsername(String token) throws RemoteException {
        validateToken(token);
        return username;
    }

    @Override
    public String getToken() throws RemoteException {
        return token;
    }

    @Override
    public void logout(String token) throws RemoteException {
        validateToken(token);
        factory.removeSession(username);
    }

    @Override
    public List<GameInfo> listGames(String token) throws RemoteException {
        validateToken(token);
        List<GameInfo> jogos = new ArrayList<>();
        for (Map.Entry<String, BattleshipGameSubjectRI> entry : factory.getActiveGames().entrySet()) {
            int players = entry.getValue().getPlayerCount();
            String mode = entry.getValue().getGameMode();
            jogos.add(new GameInfo(entry.getKey(), players, mode));
        }
        return jogos;
    }




    
    @Override
    public BattleshipGameSubjectRI createGame(String token, String mode) throws RemoteException {
        validateToken(token);
        String gameId = "Game-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BattleshipGameSubjectRI game = new BattleshipGameSubjectImpl(gameId, mode);
        factory.getActiveGames().put(gameId, game);
        System.out.println("[Servidor] '" + username + "' criou o jogo: " + gameId + " [" + mode + "]");
        return game;
    }

    @Override
    public BattleshipGameSubjectRI getProxy(String token, String gameId) throws RemoteException {
        validateToken(token);
        BattleshipGameSubjectRI game = factory.getActiveGames().get(gameId);
        if (game == null) {
            throw new RemoteException("Jogo '" + gameId + "' não encontrado.");
        }
        System.out.println("[Servidor] '" + username + "' obteve proxy para o jogo: " + gameId);
        return game;
    }
}
