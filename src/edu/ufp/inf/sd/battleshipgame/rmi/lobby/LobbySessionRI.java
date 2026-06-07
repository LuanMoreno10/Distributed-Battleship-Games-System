package edu.ufp.inf.sd.battleshipgame.rmi.lobby;

import edu.ufp.inf.sd.battleshipgame.model.*;
import edu.ufp.inf.sd.battleshipgame.rmi.game.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LobbySessionRI extends Remote {
    String getUsername(String token) throws RemoteException;

    String getToken() throws RemoteException;

    void logout(String token) throws RemoteException;

    List<GameInfo> listGames(String token) throws RemoteException;

    /** mode: "RMI" ou "PUBSUB" */
    BattleshipGameSubjectRI createGame(String token, String mode) throws RemoteException;

    BattleshipGameSubjectRI getProxy(String token, String gameId) throws RemoteException;
}
