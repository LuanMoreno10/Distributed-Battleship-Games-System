package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipGameObserverRI extends Remote {
    void update(GameState state) throws RemoteException;
}
