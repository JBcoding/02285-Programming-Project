package karlMarx;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MultiNode extends Node {
    
    private int _hash = 0;
    private Agent[] agents;
    
    public MultiNode(Node node, Agent[] agents) {
        super(null);
        this.boxList.addAll(node.boxList);
        this.agents = agents;
    }

    @Override
    public int hashCode() {
        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.deepHashCode(agents);
            result = prime * result + boxList
                    .stream()
                    .sorted()
                    .collect(Collectors.toList()).hashCode();
            this._hash = result;
        }
        return this._hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;
        MultiNode other = (MultiNode) obj;
        if (!Arrays.deepEquals(this.agents, other.agents))
            return false;
        if (!boxList.stream()
                .sorted()
                .collect(Collectors.toList())
                .equals(other.boxList
                        .stream()
                        .sorted()
                        .collect(Collectors.toList())))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < MAX_ROW; row++) {
            if (!Node.walls[row][0]) {
                break;
            }
            // TODO: Possibly recreate toString
            for (int col = 0; col < MAX_COL; col++) {
                Box box = findBox(row, col);
                Goal goal = findGoal(row, col);
                if (box != null) {
                    s.append(box.letter);
                //} else if (row == this.agent.row && col == this.agent.col) {
                    //s.append(agent.id);
                } else if (goal != null) {
                    s.append(goal.letter);
                } else if (Node.walls[row][col]) {
                    s.append("+");
                } else {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        s.append(Arrays.toString(agents));
        s.append("\n");
        
        return s.toString();
    }
    
}
