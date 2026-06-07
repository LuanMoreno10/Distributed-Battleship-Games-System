package edu.ufp.inf.sd.battleshipgame.rmi.lobby;

import edu.ufp.inf.sd.battleshipgame.model.*;
import edu.ufp.inf.sd.battleshipgame.rmi.game.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipFactoryRI extends Remote {
    LobbySessionRI register(String username, String password) throws RemoteException;

    LobbySessionRI login(String username, String password) throws RemoteException;

    void connect() throws RemoteException;

    void disconnect() throws RemoteException;

    int getSessionCount() throws RemoteException;
}
