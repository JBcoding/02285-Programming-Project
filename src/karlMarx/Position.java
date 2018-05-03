package karlMarx;

public class Position {
    public int row;
    public int col;
    private int _hash;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public Position(Position position) {
        this.row = position.row;
        this.col = position.col;
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

    public boolean isOn(Position pos) {
        return row == pos.row && col == pos.col;
    }

    public Position[] getNeighbours() {
        return new Position[] {
                new Position(row + 1, col),
                new Position(row - 1, col),
                new Position(row, col + 1),
                new Position(row, col - 1)};
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