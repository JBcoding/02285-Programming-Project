package karlMarx;

import java.io.CharArrayReader;
import java.util.*;
import java.util.stream.Collectors;

import karlMarx.Command.Type;

public class Node {
    private static final Random RND = new Random(2);

    public static boolean IS_SINGLE = true;

    public static int MAX_ROW;
    public static int MAX_COL;

    public Agent agent;

    // Arrays are indexed from the top-left of the level, with first index being row and second being column.
    // Row 0: (0,0) (0,1) (0,2) (0,3) ...
    // Row 1: (1,0) (1,1) (1,2) (1,3) ...
    // Row 2: (2,0) (2,1) (2,2) (2,3) ...
    // ...
    // (Start in the top left corner, first go down, then go right)
    // E.g. this.walls[2] is an array of booleans having size MAX_COL.
    // this.walls[row][col] is true if there's a wall at (row, col)
    //

    public static boolean[][] walls = new boolean[MAX_ROW][MAX_COL];
    public ArrayList<Box> boxList = new ArrayList<>();
    public static Set<Goal> goalSet = new HashSet<Goal>();
    public static char[][] goals = new char[MAX_ROW][MAX_COL];
    public static HashMap<Character, ArrayList<Goal>> goalMap = new HashMap<Character, ArrayList<Goal>>();

    public Node parent;
    public Command action;

    private int g;

    private int _hash = 0;

    // Added as part of solution.
    public int h = -1; // cache heuristics value.

    public static void setSize(int rows, int cols) {
        // Dynamic level size.
        Node.MAX_ROW = rows;
        Node.MAX_COL = cols;
        Node.walls = new boolean[rows][cols];
        Node.goals = new char[rows][cols];
    }

    public Node(Agent agent) {
        this((Node)null);
        this.g = 0;
        this.agent = agent;
    }

    private Node(Node parent) {
        this.parent = parent;
        if (parent != null) {
            this.g = parent.g() + 1;
            this.agent = parent.agent.copy();
        }
    }

    public int g() {
        return this.g;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    public boolean isGoalState() {
        return isGoalState(goalSet, null, null);
    }

    public boolean isGoalState(Set<Goal> goals, List<Box> boxesToMove, int[][] penaltyMap) {
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

        if (boxesToMove != null) {
            for (Box b1 : boxesToMove) {
                for (Box b2 : this.boxList) {
                    if (b1.id == b2.id) {
                        if (penaltyMap[b2.row][b2.col] > 0) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean isGoalState(Set<Goal> goals, Set<Pair<Box, Position>> boxPositionGoals) {
        for (Pair<Box, Position> goal : boxPositionGoals) {
            if (!goal.a.isOn(goal.b)) {
                return false;
            }
        }
        return isGoalState(goals, null, null);
    }

    public Node getNodeFromCommand(Command c) {
        // Determine applicability of action
        int newAgentRow = this.agent.row + Command.dirToRowChange(c.dir1);
        int newAgentCol = this.agent.col + Command.dirToColChange(c.dir1);

        if (c.actionType == Type.Move) {
            // Check if there's a wall or box on the cell to which the agent is moving
            if (this.cellIsFree(newAgentRow, newAgentCol)) {
                Node n = this.ChildNode();
                n.action = c;
                n.agent.row = newAgentRow;
                n.agent.col = newAgentCol;
                return n;
            }
        } else if (c.actionType == Type.Push) {
            // Make sure that there's actually a box to move
            Box b = findBox(newAgentRow, newAgentCol);
            if (b != null && agent.color == b.color) {
                int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
                int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
                // .. and that new cell of box is free
                if (this.cellIsFree(newBoxRow, newBoxCol)) {
                    Node n = this.ChildNode();
                    n.action = c;
                    n.agent.row = newAgentRow;
                    n.agent.col = newAgentCol;
                    // Change box position in boxList
                    for (int i = 0; i < n.boxList.size(); i++) {
                        Box box = n.boxList.get(i);
                        if (box.isOn(new Position(newAgentRow, newAgentCol))) {
                            box = box.copy();
                            box.row = newBoxRow;
                            box.col = newBoxCol;
                            n.boxList.set(i, box);
                            break;
                        }
                    }
                    return n;
                }
            }
        } else if (c.actionType == Type.Pull) {
            // Cell is free where agent is going
            if (this.cellIsFree(newAgentRow, newAgentCol)) {
                int boxRow = this.agent.row + Command.dirToRowChange(c.dir2);
                int boxCol = this.agent.col + Command.dirToColChange(c.dir2);
                // .. and there's a box in "dir2" of the agent
                Box b = findBox(boxRow, boxCol);
                if (b != null && agent.color == b.color) {
                    Node n = this.ChildNode();
                    n.action = c;
                    n.agent.row = newAgentRow;
                    n.agent.col = newAgentCol;
                    // Change box position in boxList
                    for (int i = 0; i < n.boxList.size(); i++) {
                        Box box = n.boxList.get(i);
                        if (box.isOn(new Position(boxRow, boxCol))) {
                            box = box.copy();
                            box.row = this.agent.row;
                            box.col = this.agent.col;
                            n.boxList.set(i, box);
                            break;
                        }
                    }
                    return n;
                }
            }
        }
        return null;
    }

    public ArrayList<Node> getExpandedNodes() {
        ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
        for (Command c : Command.EVERY) {
            Node n = getNodeFromCommand(c);
            if (n != null) {
                expandedNodes.add(n);
            }
        }

        Collections.shuffle(expandedNodes, RND);
        return expandedNodes;
    }

    public static boolean inBounds(Position pos) {
        return pos.row >= 0 &&
                pos.row < MAX_ROW &&
                pos.col >= 0 &&
                pos.col < MAX_COL;
    }

    private boolean cellIsFree(int row, int col) {
        return !Node.walls[row][col] && !boxAt(row, col);

    }

    private boolean boxAt(int row, int col) {
        for (Box box : boxList) {
            if (box.row == row && box.col == col) {
                return true;
            }
        }
        return false;
    }

    protected Node ChildNode() {
        Node copy = new Node(this);
        copy.boxList.addAll(boxList);
        return copy;
    }

    public LinkedList<Node> extractPlan() {
        LinkedList<Node> plan = new LinkedList<Node>();
        Node n = this;
        while (!n.isInitialState()) {
            plan.addFirst(n);
            n = n.parent;
        }
        return plan;
    }

    @Override
    public int hashCode() {

        if (this._hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.agent.hashCode();
            //result = prime * result + boxList.hashCode();
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
        Node other = (Node) obj;
        if (!this.agent.equals(other.agent))
            return false;
        //if (!boxList.equals(other.boxList))
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
            // TODO: Possibly recreate toString
            for (int col = 0; col < MAX_COL; col++) {
                Box box = findBox(row, col);
                Goal goal = findGoal(row, col);
                if (box != null) {
                    s.append(box.letter);
                } else if (row == this.agent.row && col == this.agent.col) {
                    s.append(agent.id);
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
    
}
