package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipGameSubject extends Remote {
    void attach(BattleshipGameObserver observer) throws RemoteException;

    void detach(BattleshipGameObserver observer) throws RemoteException;

    void setState(Object state) throws RemoteException;

    Object getState() throws RemoteException;

    int getPlayerCount() throws RemoteException;
}
