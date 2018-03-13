package karlMarx;

public class Box extends Position {
    char letter;
    Color color;
    private int _hash;

    public Box(int row, int col, char letter, Color color) {
        super(row, col);
        this.letter = letter;
        this.color = color;
    }

    public String toString() {
        return "Box at (" + col + "," + row + ")";
    }

    public Box(Position position, char letter, Color color) {
        super(position.row, position.col);
        this.letter = letter;
        this.color = color;
    }

    public Box copy() {
        return new Box(row, col, letter, color);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Box) {
            Box o = (Box) other;
            return row == o.row && col == o.col && letter == o.letter && color == o.color;
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
            result = prime * result + letter;
            result = prime * result + color.ordinal();
            this._hash = result;
        }
        return this._hash;
    }
}