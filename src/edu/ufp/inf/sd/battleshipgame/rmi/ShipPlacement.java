package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;

public final class ShipPlacement implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Coordinate start;
    private final int length;
    private final Orientation orientation;

    public ShipPlacement(Coordinate start, int length, Orientation orientation) {
        this.start = start;
        this.length = length;
        this.orientation = orientation;
    }

    public Coordinate getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return "Ship{" + start + ", len=" + length + ", " + orientation + "}";
    }
}

