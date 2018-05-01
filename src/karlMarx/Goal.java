package karlMarx;

public class Goal extends Position {
    char letter;

    public Goal(int row, int col, char letter) {
        super(row, col);
        this.letter = letter;
    }

    public Goal(Position position, char letter) {
        super(position);
        this.letter = letter;
    }

    public static boolean isGoal(char c) {
        return c >= 'a' && c <= 'z';
    }

    public String toString() {
        return "Goal at (" + col + "," + row + ") with letter " + letter;
    }
}