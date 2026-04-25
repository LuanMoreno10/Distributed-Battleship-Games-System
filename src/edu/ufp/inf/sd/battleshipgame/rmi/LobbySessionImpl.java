package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
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
    public List<String> listGames() throws RemoteException {
        return new ArrayList<>(factory.getActiveGames().keySet());
    }

    @Override
    public BattleshipGameSubject createGame() throws RemoteException {
        // Gerar um ID de jogo aleatório (ex: "Game-1234")
        String gameId = "Game-" + UUID.randomUUID().toString().substring(0, 5);
        BattleshipGameSubject game = new BattleshipGameSubjectImpl(gameId);
        factory.getActiveGames().put(gameId, game);
        System.out.println(username + " created game: " + gameId);
        return game;
    }

    @Override
    public BattleshipGameSubject getProxy(String gameId) throws RemoteException {
        if (!factory.getActiveGames().containsKey(gameId)) {
            System.out.println("Game " + gameId + " not found!");
            return null;
        }
        return factory.getActiveGames().get(gameId);
    }
}
