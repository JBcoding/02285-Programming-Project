package karlMarx;

public class Position {
    public int row;
    public int col;
    private int _hash;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public Position copy() {
        return new Position(row, col);
    }

    public Position translate(int row, int col) {
        this.row += row;
        this.col += col;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Position) {
            Position o = (Position) other;
            return row == o.row && col == o.col;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + row;
            result = prime * result + col;
            this._hash = result;
        }
        return this._hash;
    }

    @Override
    public String toString() {
        return "Position - row: " + row + " col: " + col;
    }
}