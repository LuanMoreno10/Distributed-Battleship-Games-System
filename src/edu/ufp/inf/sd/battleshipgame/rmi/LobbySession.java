package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LobbySession extends Remote {
    String getUsername() throws RemoteException;

    String getToken() throws RemoteException;

    void logout() throws RemoteException;

    List<GameInfo> listGames() throws RemoteException;

    /** mode: "RMI" ou "PUBSUB" */
    BattleshipGameSubject createGame(String mode) throws RemoteException;

    BattleshipGameSubject getProxy(String gameId) throws RemoteException;
}
