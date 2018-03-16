package karlMarx.MessageBoard;

import karlMarx.Agent;
import karlMarx.Color;
import karlMarx.Map.levelInfo;
import karlMarx.Position;

import java.util.Set;

public class RequestMoveBox extends Message {
    private int boxId;
    private Set<Position> illegalPositions;

    public RequestMoveBox(int boxId, Set<Position> illegalPositions) {
        status = MessageStatus.PENDING;

        this.boxId = boxId;
        this.illegalPositions = illegalPositions;

        // find the color, and thereby the agent, that can move this box
        Color color = levelInfo.getBoxColorFromId(this.boxId);
        Set<Agent> agents = levelInfo.getAgentsFromColor(color);

        // update the "receivers" array hereafter
        receivers = agents.stream().mapToInt(agent -> agent.id).toArray();
    }

    public int getBoxId() {
        return boxId;
    }

    public Set<Position> getIllegalPositions() {
        return illegalPositions;
    }
}
