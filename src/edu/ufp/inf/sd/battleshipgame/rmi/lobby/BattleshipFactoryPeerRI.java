package edu.ufp.inf.sd.battleshipgame.rmi.lobby;

import edu.ufp.inf.sd.battleshipgame.model.*;
import edu.ufp.inf.sd.battleshipgame.rmi.game.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI de sincronização entre nós servidores (R5).
 * Cada nó expõe este interface para que o seu par possa replicar dados de utilizadores.
 */
public interface BattleshipFactoryPeerRI extends Remote {

    /** Replica um registo de utilizador. Idempotente: ignorado se o utilizador já existir. */
    void syncRegister(String username, String password) throws RemoteException;

    /** Notifica o peer que uma sessão foi terminada (logout). */
    void syncLogout(String username) throws RemoteException;

    /** Health-check para detetar se o nó está vivo. */
    boolean ping() throws RemoteException;

    /**
     * Registo bidirecional: o nó B apresenta-se ao nó A como peer,
     * e envia todos os utilizadores que já tem para sincronização inicial.
     * Permite que o Nó B se ligue ao Nó A depois deste já estar a correr.
     */
    void registerAsPeer(BattleshipFactoryPeerRI caller, java.util.Map<String, String> existingUsers)
            throws RemoteException;
}
