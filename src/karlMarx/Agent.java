package karlMarx;

public class Agent extends Position {
    int id;
    Color color;
    private int _hash;

    public Agent(int row, int col, int id, Color color) {
        super(row, col);
        this.id = id;
        this.color = color;
    }
    
    public Agent(Position position, int id, Color color) {
        super(position.row, position.col);
        this.id = id;
        this.color = color;
    }

    public Agent copy() {
        return new Agent(row, col, id, color);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Agent) {
            Agent o = (Agent) other;
            return row == o.row && col == o.col && id == o.id && color == o.color;
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
            result = prime * result + id;
            result = prime * result + color.ordinal();
            this._hash = result;
        }
        return this._hash;
    }

    public String toString() {
        return "Agent at (" + col + "," + row + ")";
    }

}
