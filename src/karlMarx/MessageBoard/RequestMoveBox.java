package karlMarx.MessageBoard;

import karlMarx.Agent;
import karlMarx.Color;
import karlMarx.Map.level;
import karlMarx.Position;

import java.util.Set;
import java.util.stream.Collectors;

public class RequestMoveBox extends Message {
    private int boxId;
    private Set<Position> illegalPositions;

    public RequestMoveBox(int boxId, Set<Position> illegalPositions) {
        status = MessageStatus.PENDING;

        this.boxId = boxId;
        this.illegalPositions = illegalPositions;

        // find the color, and thereby the agent, that can move this box
        Color color = level.getBoxColorFromId(this.boxId);
        Set<Agent> agents = level.getAgentsFromColor(color);

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
