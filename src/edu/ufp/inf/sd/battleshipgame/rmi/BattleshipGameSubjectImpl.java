package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class BattleshipGameSubjectImpl extends UnicastRemoteObject implements BattleshipGameSubject {
    private final String gameId;
    private final List<BattleshipGameObserver> observers;
    private Object gameState; // Temporário, depois podemos tipar

    public BattleshipGameSubjectImpl(String gameId) throws RemoteException {
        super();
        this.gameId = gameId;
        this.observers = new ArrayList<>();
    }

    @Override
    public void attach(BattleshipGameObserver observer) throws RemoteException {
        if (observers.size() < 2) {
            observers.add(observer);
            System.out.println("Player attached to game " + gameId);
        } else {
            throw new RemoteException("Game is already full (max 2 players).");
        }
    }

    @Override
    public void detach(BattleshipGameObserver observer) throws RemoteException {
        observers.remove(observer);
        System.out.println("Player detached from game " + gameId);
    }

    @Override
    public void setState(Object state) throws RemoteException {
        this.gameState = state;
        System.out.println("Game " + gameId + " state updated.");
        notifyObservers();
    }

    @Override
    public Object getState() throws RemoteException {
        return gameState;
    }

    private void notifyObservers() throws RemoteException {
        for (BattleshipGameObserver observer : observers) {
            try {
                observer.update(gameState);
            } catch (RemoteException e) {
                System.out.println("Observer not responding. Unsubscribing...");
                // Idealmente tratar a remoção durante iteração ou após
            }
        }
    }

    @Override
    public int getPlayerCount() throws RemoteException {
        return observers.size();
    }

    public String getGameId() {
        return gameId;
    }
}
