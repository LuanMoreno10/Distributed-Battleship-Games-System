package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

/**
 * Objeto de valor transferido por RMI com informação resumida de um jogo.
 * Serializable porque é passado por cópia (não é um objeto remoto).
 */
public class GameInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String gameId;
    private final int playerCount;
    private final String mode;

    public GameInfo(String gameId, int playerCount, String mode) {
        this.gameId = gameId;
        this.playerCount = playerCount;
        this.mode = mode;
    }

    public String getGameId()    { return gameId; }
    public int    getPlayerCount() { return playerCount; }
    public String getMode()      { return mode; }

    @Override
    public String toString() {
        return gameId + " (" + playerCount + "/2 jogadores, modo: " + mode + ")";
    }
}
