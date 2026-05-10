package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

/**
 * Estado local (do servidor) para um jogador.
 * É Serializable para poder ser enviado aos clientes em getState()/update().
 */
public class PlayerState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 10;

    // lengths clássicos do Battleship
    private static final int[] DEFAULT_SHIPS = {5, 4, 3, 3, 2};

    private final boolean[][] ships = new boolean[SIZE][SIZE];
    private final boolean[][] hitsReceived = new boolean[SIZE][SIZE];

    private int nextShipIndex = 0;

    public int shipsRemainingToPlace() {
        return DEFAULT_SHIPS.length - nextShipIndex;
    }

    public int nextShipLength() {
        if (nextShipIndex >= DEFAULT_SHIPS.length) {
            return -1;
        }
        return DEFAULT_SHIPS[nextShipIndex];
    }

    public boolean allShipsPlaced() {
        return nextShipIndex >= DEFAULT_SHIPS.length;
    }

    public void placeNextShip(ShipPlacement placement) {
        if (allShipsPlaced()) {
            throw new IllegalStateException("All ships already placed.");
        }
        int requiredLen = DEFAULT_SHIPS[nextShipIndex];
        if (placement.getLength() != requiredLen) {
            throw new IllegalArgumentException("Expected ship length " + requiredLen + " but got " + placement.getLength());
        }
        if (!canPlace(placement)) {
            throw new IllegalArgumentException("Invalid placement: out of bounds or overlaps existing ship.");
        }

        int r = placement.getStart().getRow();
        int c = placement.getStart().getCol();
        for (int i = 0; i < placement.getLength(); i++) {
            ships[r][c] = true;
            if (placement.getOrientation() == Orientation.HORIZONTAL) {
                c++;
            } else {
                r++;
            }
        }
        nextShipIndex++;
    }

    public boolean canPlace(ShipPlacement placement) {
        int r = placement.getStart().getRow();
        int c = placement.getStart().getCol();

        for (int i = 0; i < placement.getLength(); i++) {
            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                return false;
            }
            if (ships[r][c]) {
                return false;
            }
            if (placement.getOrientation() == Orientation.HORIZONTAL) {
                c++;
            } else {
                r++;
            }
        }
        return true;
    }

    /**
     * Regista um tiro recebido.
     * @return true se foi hit, false se foi miss
     */
    public boolean receiveShot(Shot shot) {
        int r = shot.getTarget().getRow();
        int c = shot.getTarget().getCol();
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            throw new IllegalArgumentException("Shot out of bounds: " + shot.getTarget());
        }
        hitsReceived[r][c] = true;
        return ships[r][c];
    }

    public boolean isAllShipsSunk() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (ships[r][c] && !hitsReceived[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean[][] getShips() {
        return ships;
    }

    public boolean[][] getHitsReceived() {
        return hitsReceived;
    }
}

