package karlMarx;

import java.util.*;

import karlMarx.Command.Type;

public class MultiBodyNode implements GeneralNode {
    private static final Random RND = new Random(4);

    public Agent[] agents;

    public static boolean[][] walls;
    public ArrayList<Box> boxList = new ArrayList<>();
    public ArrayList<Box> boxListSorted;
    public int boxListSortedHashCode = 0;
    public static Set<Goal> goalSet = new HashSet<Goal>();
    public static char[][] goals;

    public MultiBodyNode parent;
    public Command action;
    public int agentID;

    private int g;

    private int _hash = 0;

    public int h = -1;

    public MultiBodyNode(Node n) {
        this.boxList = new ArrayList<>(n.boxList);
        this.boxListSorted = new ArrayList<>(n.boxListSorted);
        this.boxListSortedHashCode = n.boxListSortedHashCode;
        MultiBodyNode.goalSet = new HashSet<>(Node.goalSet);
        MultiBodyNode.goals = new char[Node.MAX_ROW][Node.MAX_COL];
        MultiBodyNode.walls = new boolean[Node.MAX_ROW][Node.MAX_COL];
        for (int i = 0; i < Node.MAX_ROW; i++) {
            for (int j = 0; j < Node.MAX_COL; j++) {
                MultiBodyNode.goals[i][j] = Node.goals[i][j];
                MultiBodyNode.walls[i][j] = Node.walls[i][j];
            }
        }
        MultiBodyNode.goals = Node.goals;
    }

    private MultiBodyNode(MultiBodyNode parent) {
        this.parent = parent;
        if (parent != null) {
            this.g = parent.g() + 1;
            this.agents = new Agent[parent.agents.length];
            for (int i = 0; i < this.agents.length; i++) {
                this.agents[i] = parent.agents[i].copy();
            }
        }
    }

    public int g() {
        return this.g;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    public boolean isGoalState() {
        return isGoalState(goalSet, null, null, null, null);
    }

    public boolean isGoalState(Set<Goal> goals, List<Box> boxesToMove, int[][] penaltyMap, Position endPos, Set<Position> illegalPositions) {
        goalLoop:
        for (Goal goal : goals) {
            for (Box box : boxList) {
                if (box.isOn(goal)) {
                    if (Character.toLowerCase(box.letter) == goal.letter) {
                        continue goalLoop;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        return true;
    }

    public MultiBodyNode getNodeFromCommand(Command c, int agentID) {
        // Determine applicability of action
        int newAgentRow = this.agents[agentID].row + Command.dirToRowChange(c.dir1);
        int newAgentCol = this.agents[agentID].col + Command.dirToColChange(c.dir1);

        if (c.actionType == Type.Move) {
            // Check if there's a wall or box on the cell to which the agent is moving
            if (this.cellIsFree(newAgentRow, newAgentCol)) {
                MultiBodyNode n = this.ChildNode();
                n.action = c;
                n.agents[agentID].row = newAgentRow;
                n.agents[agentID].col = newAgentCol;
                n.agentID = agentID;
                return n;
            }
        } else if (c.actionType == Type.Push) {
            // Make sure that there's actually a box to move
            Box b = findBox(newAgentRow, newAgentCol);
            if (b != null && agents[agentID].color == b.color) {
                int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
                int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
                // .. and that new cell of box is free
                if (this.cellIsFree(newBoxRow, newBoxCol)) {
                    MultiBodyNode n = this.ChildNode();
                    n.action = c;
                    n.agents[agentID].row = newAgentRow;
                    n.agents[agentID].col = newAgentCol;
                    n.agentID = agentID;
                    // Change box position in boxList
                    for (int i = 0; i < n.boxList.size(); i++) {
                        Box box = n.boxList.get(i);
                        if (box.isOn(new Position(newAgentRow, newAgentCol))) {
                            box = box.copy();
                            box.row = newBoxRow;
                            box.col = newBoxCol;
                            n.boxList.set(i, box);

                            n.boxListSorted = new ArrayList<>();
                            n.boxListSorted.addAll(n.boxList);
                            insertionSortOneLoop(n, i);
                            n.boxListSortedHashCode = n.boxListSorted.hashCode();
                            break;
                        }
                    }
                    return n;
                }
            }
        } else if (c.actionType == Type.Pull) {
            // Cell is free where agent is going
            if (this.cellIsFree(newAgentRow, newAgentCol)) {
                int boxRow = this.agents[agentID].row + Command.dirToRowChange(c.dir2);
                int boxCol = this.agents[agentID].col + Command.dirToColChange(c.dir2);
                // .. and there's a box in "dir2" of the agent
                Box b = findBox(boxRow, boxCol);
                if (b != null && agents[agentID].color == b.color) {
                    MultiBodyNode n = this.ChildNode();
                    n.action = c;
                    n.agents[agentID].row = newAgentRow;
                    n.agents[agentID].col = newAgentCol;
                    n.agentID = agentID;
                    // Change box position in boxList
                    for (int i = 0; i < n.boxList.size(); i++) {
                        Box box = n.boxList.get(i);
                        if (box.isOn(new Position(boxRow, boxCol))) {
                            box = box.copy();
                            box.row = this.agents[agentID].row;
                            box.col = this.agents[agentID].col;
                            n.boxList.set(i, box);

                            n.boxListSorted = new ArrayList<>();
                            n.boxListSorted.addAll(n.boxList);
                            insertionSortOneLoop(n, i);
                            n.boxListSortedHashCode = n.boxListSorted.hashCode();
                            break;
                        }
                    }
                    return n;
                }
            }
        }
        return null;
    }

    private void insertionSortOneLoop(MultiBodyNode n, int index) {
        Collections.sort(n.boxListSorted);
    }

    public ArrayList<MultiBodyNode> getExpandedNodes() {
        ArrayList<MultiBodyNode> expandedNodes = new ArrayList<MultiBodyNode>(Command.EVERY.length);
        for (Command c : Command.EVERY) {
            for (int i = 0; i < agents.length; i++) {
                for (int j = 0; j < agents.length; j++) {
                    if (i != j) {
                        MultiBodyNode.walls[agents[j].row][agents[j].col] = true;
                    }
                }
                MultiBodyNode n = getNodeFromCommand(c, i);
                if (n != null) {
                    expandedNodes.add(n);
                }
                for (int j = 0; j < agents.length; j++) {
                    if (i != j) {
                        MultiBodyNode.walls[agents[j].row][agents[j].col] = false;
                    }
                }
            }
        }

        Collections.shuffle(expandedNodes, RND);
        return expandedNodes;
    }

    public boolean cellIsFree(int row, int col) {
        return !walls[row][col] && !boxAt(row, col);

    }

    private boolean boxAt(int row, int col) {
        for (Box box : boxList) {
            if (box.row == row && box.col == col) {
                return true;
            }
        }
        return false;
    }

    protected MultiBodyNode ChildNode() {
        MultiBodyNode copy = new MultiBodyNode(this);
        copy.boxList.addAll(boxList);
        copy.boxListSorted = this.boxListSorted;
        copy.boxListSortedHashCode = boxListSortedHashCode;
        return copy;
    }

    public LinkedList<Pair<Command, Integer>> extractPlan() {
        LinkedList<Pair<Command, Integer>> plan = new LinkedList<>();
        MultiBodyNode n = this;
        while (!n.isInitialState()) {
            plan.addFirst(new Pair<>(n.action, n.agentID));
            n = n.parent;
        }
        return plan;
    }

    @Override
    public int hashCode() {

        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.deepHashCode(this.agents);
            //result = prime * result + boxList.hashCode();
            result = prime * result + boxListSortedHashCode;
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
        MultiBodyNode other = (MultiBodyNode) obj;
        if (!Arrays.deepEquals(this.agents, other.agents))
            return false;
        //if (!boxList.equals(other.boxList))
        if (!boxListSorted.equals(other.boxListSorted))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            // TODO: Possibly recreate toString
            for (int col = 0; col < Node.MAX_COL; col++) {
                Box box = findBox(row, col);
                Goal goal = findGoal(row, col);
                if (box != null) {
                    s.append(box.letter);
                } else if (walls[row][col] && goal != null) {
                    s.append("@");
                } else if (goal != null) {
                    s.append(goal.letter);
                } else if (walls[row][col]) {
                    s.append("+");
                } else {
                    boolean agent = false;
                    for (int i = 0; i < this.agents.length; i++) {
                        if (this.agents[i].row == row && this.agents[i].col == col) {
                            s.append(this.agents[i].id);
                            agent = true;
                        }
                    }
                    if (!agent) {
                        s.append(" ");
                    }
                }
            }
            s.append("\n");
        }
        return s.toString();
    }

    public Box findBox(int row, int col) {
        for (Box b : boxList) {
            if (b.row == row && b.col == col) {
                return b;
            }
        }
        return null;
    }

    public static Goal findGoal(int row, int col) {
        for (Goal g : goalSet) {
            if (g.row == row && g.col == col) {
                return g;
            }
        }
        return null;
    }

    public void setAgents(Agent[] agents) {
        this.agents = agents;
    }
}
