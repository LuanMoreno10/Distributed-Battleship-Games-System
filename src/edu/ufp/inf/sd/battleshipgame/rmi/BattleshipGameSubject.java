package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipGameSubject extends Remote {
    /**
     * Associa um jogador ao jogo.
     * No modo Observer (RMI callbacks) o servidor irá notificar este observer.
     */
    void attach(String username, BattleshipGameObserver observer) throws RemoteException;

    /** Remove o jogador (e respetivo observer) do jogo. */
    void detach(String username) throws RemoteException;

    /**
     * Aplica uma ação de jogo (posicionar navio, disparar tiro, passar turno).
     * Após aplicar, o servidor propaga o novo estado via update() e/ou Pub/Sub.
     */
    void setState(GameAction action) throws RemoteException;

    /** Snapshot do estado atual do jogo (enviado por cópia). */
    GameState getState() throws RemoteException;

    int getPlayerCount() throws RemoteException;
}
