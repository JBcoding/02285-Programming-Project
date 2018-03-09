package karlMarx;

public class Goal {
    Position position;
    char letter;

    public Goal(int row, int col, char letter) {
    	position = new Position(row, col);
        this.letter = letter;
    }

    public Goal(Position position, char letter) {
    	this.position = position.copy();
        this.letter = letter;
    }

    public String toString() {
        return "(" + position.col + "," + position.row + ")";
    }
}