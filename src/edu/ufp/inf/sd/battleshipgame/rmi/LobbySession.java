package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LobbySession extends Remote {
    String getUsername() throws RemoteException;

    List<String> listGames() throws RemoteException;

    BattleshipGameSubject createGame() throws RemoteException;

    BattleshipGameSubject getProxy(String gameId) throws RemoteException;
}
