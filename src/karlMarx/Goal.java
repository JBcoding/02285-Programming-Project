package karlMarx;

public class Goal {
    int row, col;
    char letter;

    public Goal(int row, int col, char letter) {
        this.row = row;
        this.col = col;
        this.letter = letter;
    }

    public String toString() {
        return "(" + this.col + "," + this.row + ")";
    }
}