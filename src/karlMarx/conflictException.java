package karlMarx;

public class conflictException extends Exception {
    private int conflictRound;
    public conflictException(int stepsTaken) {
        this.conflictRound = stepsTaken;
    }

    public int getConflictRound() {
        return conflictRound;
    }
}
