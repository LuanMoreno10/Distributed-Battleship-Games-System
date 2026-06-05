package edu.ufp.inf.sd.battleshipgame.Client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameObserverRI;
import edu.ufp.inf.sd.battleshipgame.rmi.GameState;

/**
 * Observer RMI (callback) usado pelo cliente para receber updates síncronos do servidor.
 */
public class ClientGameObserverImpl extends UnicastRemoteObject implements BattleshipGameObserverRI {

    private volatile GameState lastState;

    public ClientGameObserverImpl() throws RemoteException {
        super();
    }

    @Override
    public void update(GameState state) throws RemoteException {
        this.lastState = state;

        List<String> log = state.getEventLog();
        String last = log.isEmpty() ? "(sem eventos)" : log.get(log.size() - 1);
        System.out.println("\n[UPDATE] " + last);
        System.out.println("        phase=" + state.getPhase() + ", turn=" + state.getCurrentTurn() + ", winner=" + state.getWinner());
        System.out.print("> ");
    }

    public GameState getLastState() {
        return lastState;
    }
}

