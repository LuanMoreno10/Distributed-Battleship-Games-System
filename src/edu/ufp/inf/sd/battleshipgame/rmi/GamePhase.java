package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

public enum GamePhase implements Serializable {
    WAITING_FOR_PLAYERS,
    PLACING_SHIPS,
    IN_PROGRESS,
    FINISHED
}

