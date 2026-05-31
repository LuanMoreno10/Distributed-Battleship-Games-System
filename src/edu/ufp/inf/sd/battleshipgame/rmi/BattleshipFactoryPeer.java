package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI de sincronização entre nós servidores (R5).
 * Cada nó expõe este interface para que o seu par possa replicar dados de utilizadores.
 */
public interface BattleshipFactoryPeer extends Remote {

    /** Replica um registo de utilizador. Idempotente: ignorado se o utilizador já existir. */
    void syncRegister(String username, String password) throws RemoteException;

    /** Notifica o peer que uma sessão foi terminada (logout). */
    void syncLogout(String username) throws RemoteException;

    /** Health-check para detetar se o nó está vivo. */
    boolean ping() throws RemoteException;
}
