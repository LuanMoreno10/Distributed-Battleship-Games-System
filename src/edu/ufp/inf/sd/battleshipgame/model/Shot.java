package edu.ufp.inf.sd.battleshipgame.model;

import java.io.Serializable;

public final class Shot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Coordinate target;

    public Shot(Coordinate target) {
        this.target = target;
    }

    public Coordinate getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Shot{" + target + "}";
    }
}

