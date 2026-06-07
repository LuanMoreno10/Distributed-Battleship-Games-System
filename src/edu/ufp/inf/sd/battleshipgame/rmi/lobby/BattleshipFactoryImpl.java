package edu.ufp.inf.sd.battleshipgame.rmi.lobby;

import edu.ufp.inf.sd.battleshipgame.model.*;
import edu.ufp.inf.sd.battleshipgame.rmi.game.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação da BattleshipFactory.
 * Também implementa BattleshipFactoryPeer para sincronização entre nós (R5).
 */
public class BattleshipFactoryImpl extends UnicastRemoteObject implements BattleshipFactoryRI, BattleshipFactoryPeerRI {

    private final Map<String, String> users = new HashMap<>();
    private final Map<String, LobbySessionRI> activeSessions = new HashMap<>();
    private final Map<String, BattleshipGameSubjectRI> activeGames = new HashMap<>();

    private final String nodeId;
    private int connectedClients = 0;

    /** Referência ao nó par para replicação. Pode ser null se o peer ainda não estiver disponível. */
    private volatile BattleshipFactoryPeerRI peer;

    public BattleshipFactoryImpl(String nodeId) throws RemoteException {
        super();
        this.nodeId = nodeId;
    }

    // -------------------------------------------------------------------------
    // BattleshipFactoryRI — acesso dos clientes
    // -------------------------------------------------------------------------

    @Override
    public synchronized LobbySessionRI register(String username, String password) throws RemoteException {

        // Validação
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new RemoteException("Username e password não podem estar vazios.");
        }
        if (users.containsKey(username)) {
            throw new RemoteException("Utilizador '" + username + "' já existe.");
        }

        // Cria User
        users.put(username, password);
        System.out.println("[" + nodeId + "] Novo utilizador registado: " + username);

        // Replicar para o peer (R5)
        replicateRegister(username, password);

        return login(username, password);
    }

    @Override
    public synchronized LobbySessionRI login(String username, String password) throws RemoteException {

        // Validação
        if (!users.containsKey(username) || !users.get(username).equals(password)) {
            throw new RemoteException("Credenciais inválidas para o utilizador '" + username + "'.");
        }
        if (activeSessions.containsKey(username)) {
            throw new RemoteException("O utilizador '" + username + "' já tem uma sessão ativa.");
        }

        // Gerar token JWT
        String token = JwtUtils.generateToken(username);

        LobbySessionRI session = new LobbySessionImpl(username, token, this);
        
        activeSessions.put(username, session);
        System.out.println("[" + nodeId + "] '" + username + "' autenticado. Token gerado.");
        return session;
    }

    public synchronized void removeSession(String username) {
        activeSessions.remove(username);
        System.out.println("[" + nodeId + "] Sessão de '" + username + "' terminada.");
        replicateLogout(username);
    }



// ------------------------------------------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // BattleshipFactoryPeerRI — sincronização entre nós (R5)
    // -------------------------------------------------------------------------

    @Override
    public synchronized void syncRegister(String username, String password) throws RemoteException {
        if (!users.containsKey(username)) {
            users.put(username, password);
            System.out.println("[" + nodeId + "] [SYNC] Utilizador replicado: " + username);
        }
    }

    @Override
    public synchronized void syncLogout(String username) throws RemoteException {
        activeSessions.remove(username);
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public synchronized void registerAsPeer(BattleshipFactoryPeerRI caller, java.util.Map<String, String> callerUsers) throws RemoteException {
        
        // Define o caller como peer deste nó (registo bidirecional)
        setPeer(caller);
        // Sincroniza os utilizadores que o caller já tem
        for (java.util.Map.Entry<String, String> entry : callerUsers.entrySet()) {
            if (!users.containsKey(entry.getKey())) {
                users.put(entry.getKey(), entry.getValue());
                System.out.println("[" + nodeId + "] [SYNC] Utilizador recebido via registerAsPeer: "
                        + entry.getKey());
            }
        }
        // Envia de volta os utilizadores deste nó ao caller
        for (java.util.Map.Entry<String, String> entry : users.entrySet()) {
            try {
                caller.syncRegister(entry.getKey(), entry.getValue());
            } catch (RemoteException ignored) {}
        }
        System.out.println("[" + nodeId + "] Peer bidirecional estabelecido. Utilizadores sincronizados.");
    }

    // -------------------------------------------------------------------------
    // Replicação para o peer (melhor-esforço — não bloqueia em caso de falha)
    // -------------------------------------------------------------------------

    private void replicateRegister(String username, String password) {
        BattleshipFactoryPeerRI p = peer;
        if (p == null) return;
        try {
            p.syncRegister(username, password);
        } catch (RemoteException e) {
            System.err.println("[" + nodeId + "] Aviso: falha ao replicar registo para peer: " + e.getMessage());
            peer = null; // peer caiu; o nó continua a funcionar sozinho
        }
    }

    private void replicateLogout(String username) {
        BattleshipFactoryPeerRI p = peer;
        if (p == null) return;
        try {
            p.syncLogout(username);
        } catch (RemoteException e) {
            System.err.println("[" + nodeId + "] Aviso: falha ao replicar logout para peer: " + e.getMessage());
            peer = null;
        }
    }

    // -------------------------------------------------------------------------
    // Gestão do peer (chamado pelo ServerApp após arranque)
    // -------------------------------------------------------------------------

    /** Liga este nó ao seu par. Aceita null para desligar. */
    public void setPeer(BattleshipFactoryPeerRI peer) {
        this.peer = peer;
        if (peer != null) {
            System.out.println("[" + nodeId + "] Peer ligado. Replicação ativa.");
        }
    }

    public boolean hasPeer() {
        return peer != null;
    }

    public BattleshipFactoryPeerRI getPeerRef() {
        return peer;
    }

    // -------------------------------------------------------------------------
    // Acessores internos (usados por LobbySessionImpl)
    // -------------------------------------------------------------------------


    @Override
    public synchronized void connect() throws RemoteException {
        connectedClients++;
    }

    @Override
    public synchronized void disconnect() throws RemoteException {
        connectedClients--;
    }

    @Override
    public synchronized int getSessionCount() throws RemoteException {
        return connectedClients;
    }

    public synchronized Map<String, String> getUsers() { return new HashMap<>(users); }

    public Map<String, BattleshipGameSubjectRI> getActiveGames() { return activeGames; }

    public String getNodeId() { return nodeId; }
}
