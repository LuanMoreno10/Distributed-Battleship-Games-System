package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

public enum GameActionType implements Serializable {
    PLACE_SHIP,
    FIRE,
    PASS
}

