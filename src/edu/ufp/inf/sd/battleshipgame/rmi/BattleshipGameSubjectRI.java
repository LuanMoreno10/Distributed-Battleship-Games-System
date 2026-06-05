package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleshipGameSubjectRI extends Remote {
    /**
     * Associa um jogador ao jogo.
     * No modo Observer (RMI callbacks) o servidor irá notificar este observer.
     */
    void attach(String token, String username, BattleshipGameObserverRI observer) throws RemoteException;

    /** Remove o jogador (e respetivo observer) do jogo. */
    void detach(String token, String username) throws RemoteException;

    /**
     * Aplica uma ação de jogo (posicionar navio, disparar tiro, passar turno).
     * Após aplicar, o servidor propaga o novo estado via update() e/ou Pub/Sub.
     */
    void setState(String token, GameAction action) throws RemoteException;

    /** Snapshot do estado atual do jogo (enviado por cópia). */
    GameState getState(String token) throws RemoteException;

    int getPlayerCount() throws RemoteException;

    String getGameId() throws RemoteException;

    /** Devolve o modo escolhido pelo criador: "RMI" ou "PUBSUB". */
    String getGameMode() throws RemoteException;
}
