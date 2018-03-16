package karlMarx.MessageBoard;

import karlMarx.Position;

import java.util.Set;

public class RequestMoveBox extends Message {
    private int boxId;
    private Set<Position> illegalPositions;

    public RequestMoveBox(int boxId, Set<Position> illegalPositions) {
        status = MessageStatus.PENDING;

        this.boxId = boxId;
        this.illegalPositions = illegalPositions;

        // TODO:
        // find the color, and thereby the agent, that can move this box
        // update the "receivers" array hereafter
    }

    public int getBoxId() {
        return boxId;
    }

    public Set<Position> getIllegalPositions() {
        return illegalPositions;
    }
}
