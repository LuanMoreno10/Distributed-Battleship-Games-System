package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LobbySessionImpl extends UnicastRemoteObject implements LobbySession {
    private final String username;
    private final BattleshipFactoryImpl factory;

    public LobbySessionImpl(String username, BattleshipFactoryImpl factory) throws RemoteException {
        super();
        this.username = username;
        this.factory = factory;
    }

    @Override
    public String getUsername() throws RemoteException {
        return username;
    }

    @Override
    public List<GameInfo> listGames() throws RemoteException {
        List<GameInfo> jogos = new ArrayList<>();
        for (Map.Entry<String, BattleshipGameSubject> entry : factory.getActiveGames().entrySet()) {
            int players = entry.getValue().getPlayerCount();
            jogos.add(new GameInfo(entry.getKey(), players));
        }
        return jogos;
    }

    @Override
    public BattleshipGameSubject createGame() throws RemoteException {
        String gameId = "Game-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BattleshipGameSubject game = new BattleshipGameSubjectImpl(gameId);
        factory.getActiveGames().put(gameId, game);
        System.out.println("[Servidor] '" + username + "' criou o jogo: " + gameId);
        return game;
    }

    @Override
    public BattleshipGameSubject getProxy(String gameId) throws RemoteException {
        BattleshipGameSubject game = factory.getActiveGames().get(gameId);
        if (game == null) {
            throw new RemoteException("Jogo '" + gameId + "' não encontrado.");
        }
        System.out.println("[Servidor] '" + username + "' obteve proxy para o jogo: " + gameId);
        return game;
    }
}
