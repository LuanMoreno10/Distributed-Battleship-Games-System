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
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new RemoteException("Username e password não podem estar vazios.");
        }
        if (users.containsKey(username)) {
            throw new RemoteException("Utilizador '" + username + "' já existe.");
        }
        users.put(username, password);
        System.out.println("[Servidor] Novo utilizador registado: " + username);
        return login(username, password);
    }

    @Override
    public LobbySession login(String username, String password) throws RemoteException {
        if (!users.containsKey(username) || !users.get(username).equals(password)) {
            throw new RemoteException("Credenciais inválidas para o utilizador '" + username + "'.");
        }
        LobbySession session = new LobbySessionImpl(username, this);
        activeSessions.put(username, session);
        System.out.println("[Servidor] Utilizador '" + username + "' autenticado com sucesso.");
        return session;
    }

    public void removeSession(String username) {
        activeSessions.remove(username);
        System.out.println("[Servidor] Sessão de '" + username + "' terminada.");
    }

    public Map<String, BattleshipGameSubject> getActiveGames() {
        return activeGames;
    }
}
