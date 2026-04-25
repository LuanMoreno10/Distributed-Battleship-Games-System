package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipGameObserver extends Remote {
    void update(Object state) throws RemoteException;
}
