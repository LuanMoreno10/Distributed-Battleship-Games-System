package edu.ufp.inf.sd.battleshipgame.model;

import java.io.Serializable;
import java.util.Objects;

public final class Coordinate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row; // 0-9
    private final int col; // 0-9

    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate that)) return false;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "" + (char) ('A' + row) + (col + 1);
    }
}

