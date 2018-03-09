package karlMarx;

public class Box {
    Position position;
    char letter;
    private int _hash;

    public Box(int row, int col, char letter) {
        position = new Position(row, col);
        this.letter = letter;
    }

    public String toString() {
        return "(" + position.col + "," + position.row + ")";
    }

    public Box(Position position, char letter) {
        this.position = position;
        this.letter = letter;
    }

    public Box copy() {
        return new Box(position.copy(), letter);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Box) {
            Box o = (Box) other;
            return position.equals(o.position) && letter == o.letter;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + position.hashCode();
            result = prime * result + letter;
            this._hash = result;
        }
        return this._hash;
    }
}