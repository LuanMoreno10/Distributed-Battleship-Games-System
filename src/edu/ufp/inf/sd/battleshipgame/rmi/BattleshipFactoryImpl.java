package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class BattleshipFactoryImpl extends UnicastRemoteObject implements BattleshipFactory {
    // Para simplificar persistência e base de dados, guardamos em memória
    private final Map<String, String> users; // username & password
    private final Map<String, LobbySession> activeSessions;
    private final Map<String, BattleshipGameSubject> activeGames;

    public BattleshipFactoryImpl() throws RemoteException {
        super();
        this.users = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.activeGames = new HashMap<>();
    }

    @Override
    public LobbySession register(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            System.out.println("User " + username + " already exists.");
            return null; // ou throw new RemoteException("User exists")
        }
        users.put(username, password);
        System.out.println("Registered new user: " + username);
        return login(username, password);
    }

    @Override
    public LobbySession login(String username, String password) throws RemoteException {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            LobbySession session = new LobbySessionImpl(username, this);
            activeSessions.put(username, session);
            System.out.println("User " + username + " logged in successfully.");
            return session;
        }
        System.out.println("Login failed for user: " + username);
        return null; // ou throw new RemoteException("Invalid credentials")
    }

    public Map<String, BattleshipGameSubject> getActiveGames() {
        return activeGames;
    }
}
