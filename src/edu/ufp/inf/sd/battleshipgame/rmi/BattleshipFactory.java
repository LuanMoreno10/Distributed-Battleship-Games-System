package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipFactory extends Remote {
    LobbySession register(String username, String password) throws RemoteException;

    LobbySession login(String username, String password) throws RemoteException;
}
