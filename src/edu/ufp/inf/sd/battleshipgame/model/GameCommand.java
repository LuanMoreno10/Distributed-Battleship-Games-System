package edu.ufp.inf.sd.battleshipgame.model;

import java.io.Serializable;

public final class GameCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String token;
    private final GameAction action;

    public GameCommand(String token, GameAction action) {
        this.token = token;
        this.action = action;
    }

    public String getToken() { return token; }
    public GameAction getAction() { return action; }
}
