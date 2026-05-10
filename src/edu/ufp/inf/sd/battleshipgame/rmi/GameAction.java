package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

public final class GameAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GameActionType type;
    private final String username;

    private final ShipPlacement shipPlacement;
    private final Shot shot;

    private GameAction(GameActionType type, String username, ShipPlacement shipPlacement, Shot shot) {
        this.type = type;
        this.username = username;
        this.shipPlacement = shipPlacement;
        this.shot = shot;
    }

    public static GameAction placeShip(String username, ShipPlacement placement) {
        return new GameAction(GameActionType.PLACE_SHIP, username, placement, null);
    }

    public static GameAction fire(String username, Shot shot) {
        return new GameAction(GameActionType.FIRE, username, null, shot);
    }

    public static GameAction pass(String username) {
        return new GameAction(GameActionType.PASS, username, null, null);
    }

    public GameActionType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public ShipPlacement getShipPlacement() {
        return shipPlacement;
    }

    public Shot getShot() {
        return shot;
    }

    @Override
    public String toString() {
        return "GameAction{" + type + ", user='" + username + '\'' + ", ship=" + shipPlacement + ", shot=" + shot + "}";
    }
}

