package karlMarx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import karlMarx.Command.Type;

public class Node {
    private static final Random RND = new Random(1);

    public static int MAX_ROW;
    public static int MAX_COL;

    public int agentId;
    public int agentRow;
    public int agentCol;

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
    public ArrayList<Box> boxList = new ArrayList<Box>();
    public static ArrayList<Goal> goalList = new ArrayList<Goal>();
    public static char[][] goals = new char[MAX_ROW][MAX_COL];
    public static HashMap<Character, ArrayList<Goal>> goalMap = new HashMap<Character, ArrayList<Goal>>();
    public static HashMap<Character, Color> colors;

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

    public Node(Node parent) {
        this.parent = parent;
        if (parent == null) {
            this.g = 0;
            this.agentId = 0;
        } else {
            this.g = parent.g() + 1;
            this.agentId = parent.agentId;
        }

    }

    public int g() {
        return this.g;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    public boolean isGoalState() {
        goalLoop:
        // Lol
        for (Goal goal : goalList) {
            for (Box box : boxList) {
                if (box.position.equals(goal.position)) {
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

    public ArrayList<Node> getExpandedNodes() {
        ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
        for (Command c : Command.EVERY) {
            // Determine applicability of action
            int newAgentRow = this.agentRow + Command.dirToRowChange(c.dir1);
            int newAgentCol = this.agentCol + Command.dirToColChange(c.dir1);

            if (c.actionType == Type.Move) {
                // Check if there's a wall or box on the cell to which the agent is moving
                if (this.cellIsFree(newAgentRow, newAgentCol)) {
                    Node n = this.ChildNode();
                    n.action = c;
                    n.agentRow = newAgentRow;
                    n.agentCol = newAgentCol;
                    expandedNodes.add(n);
                }
            } else if (c.actionType == Type.Push) {
                // Make sure that there's actually a box to move
                Box b = findBox(newAgentRow, newAgentCol);
                if (b != null && colors.get(agentId) == colors.get(b.letter)) {
                    int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
                    int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
                    // .. and that new cell of box is free
                    if (this.cellIsFree(newBoxRow, newBoxCol)) {
                        Node n = this.ChildNode();
                        n.action = c;
                        n.agentRow = newAgentRow;
                        n.agentCol = newAgentCol;
                        // Change box position in boxList
                        for (Box box : n.boxList) {
                            if (box.position.equals(new Position(newAgentRow, newAgentCol))) {
                                box.position = new Position(newBoxRow, newBoxCol);
                                break;
                            }
                        }
                        expandedNodes.add(n);
                    }
                }
            } else if (c.actionType == Type.Pull) {
                // Cell is free where agent is going
                if (this.cellIsFree(newAgentRow, newAgentCol)) {
                    int boxRow = this.agentRow + Command.dirToRowChange(c.dir2);
                    int boxCol = this.agentCol + Command.dirToColChange(c.dir2);
                    // .. and there's a box in "dir2" of the agent
                    Box b = findBox(boxRow, boxCol);
                    if (b != null && colors.get(agentId) == colors.get(b.letter)) {
                        Node n = this.ChildNode();
                        n.action = c;
                        n.agentRow = newAgentRow;
                        n.agentCol = newAgentCol;
                        // Change box position in boxList
                        for (Box box : n.boxList) {
                            if (box.position.equals(new Position(boxRow, boxCol))) {
                                box.position = new Position(this.agentRow, this.agentCol);
                                break;
                            }
                        }
                        expandedNodes.add(n);
                    }
                }
            }
        }
        Collections.shuffle(expandedNodes, RND);
        return expandedNodes;
    }

    private boolean cellIsFree(int row, int col) {
        if (Node.walls[row][col]) {
            return false;
        }
        return !boxAt(row, col);
    }

    private boolean boxAt(int row, int col) {
        for (Box box : boxList) {
            if (box.position.row == row && box.position.col == col) {
                return true;
            }
        }
        return false;
    }

    private Node ChildNode() {
        Node copy = new Node(this);
        for (Box box : boxList) {
            copy.boxList.add(box.copy());
        }
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
            result = prime * result + this.agentCol;
            result = prime * result + this.agentRow;
            result = prime * result + Arrays.deepHashCode(Node.walls);
            result = prime * result + boxList.hashCode();
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
        if (this.agentRow != other.agentRow || this.agentCol != other.agentCol)
            return false;
        if (!boxList.equals(other.boxList))
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
                } else if (goal != null) {
                    s.append(goal.letter);
                } else if (Node.walls[row][col]) {
                    s.append("+");
                } else if (row == this.agentRow && col == this.agentCol) {
                    s.append("0");
                } else {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }

    private Box findBox(int row, int col) {
        for (Box b : boxList) {
            if (b.position.row == row && b.position.col == col) {
                return b;
            }
        }
        return null;
    }

    private Goal findGoal(int row, int col) {
        for (Goal g : goalList) {
            if (g.position.row == row && g.position.col == col) {
                return g;
            }
        }
        return null;
    }

}
