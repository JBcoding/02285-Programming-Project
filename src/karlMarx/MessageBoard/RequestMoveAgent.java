package karlMarx.MessageBoard;

import karlMarx.Position;

import java.util.Set;

public class RequestMoveAgent extends Message {
    private int agentId;
    private Set<Position> illegalPositions;

    public RequestMoveAgent(int agentId, Set<Position> illegalPositions) {
        status = MessageStatus.PENDING;

        this.agentId = agentId;
        this.illegalPositions = illegalPositions;

        receivers = new int[]{agentId};
    }

    public int getAgentId() {
        return agentId;
    }

    public Set<Position> getIllegalPositions() {
        return illegalPositions;
    }
}
