package karlMarx;

import java.util.*;

import karlMarx.Command.Type;

public class Node {
    private static final Random RND = new Random(4);

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
    public ArrayList<Box> boxListSorted;
    public int boxListSortedHashCode = 0;
    public static Set<Goal> goalSet = new HashSet<Goal>();
    public static char[][] goals = new char[MAX_ROW][MAX_COL];
    public static HashMap<Character, ArrayList<Goal>> goalMap = new HashMap<Character, ArrayList<Goal>>();

    public Node parent;
    public Command action;
    public List<Command> actions;

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
        this((Node) null);
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
        return isGoalState(goalSet, null, null, null, null);
    }

    public boolean isGoalState(Set<Goal> goals, List<Box> boxesToMove, int[][] penaltyMap, Position endPos, Set<Position> illegalPositions) {
        if (endPos != null) {
            if (!(agent.row == endPos.row && agent.col == endPos.col)) {
                return false;
            }
        }

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

        if (illegalPositions != null) {
            for (Box b : this.boxList) {
                if (illegalPositions.contains(new Position(b))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void quickApplyCommands(Node n, List<Command> cmds, SearchState ss) {
        n.agent.row = ss.getPosition().row;
        n.agent.col = ss.getPosition().col;
        if (ss.getBox() != null) {
            for (int i = 0; i < n.boxList.size(); i ++) {
                Box b = n.boxList.get(i);
                if (b.id == ss.getBox().id) {
                    Box bb = b.copy();
                    bb.row = ss.getBoxPosition().row;
                    bb.col = ss.getBoxPosition().col;
                    n.boxList.set(i, bb);

                    n.boxListSorted = new ArrayList<>();
                    n.boxListSorted.addAll(n.boxList);
                    n.insertionSortOneLoop(n, 0);
                    n.boxListSortedHashCode = n.boxListSorted.hashCode();

                    break;
                }
            }
        }
        n.actions = cmds;
        n.g += cmds.size() - 1;
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

    private void insertionSortOneLoop(Node n, int index) {
        /*int i = index;

        List<Box> lll = new ArrayList<>(n.boxListSorted);
        //System.err.println(index);
        //System.err.println("A: " + n.boxListSorted);
        while (index > 0 && n.boxListSorted.get(index).compareTo(n.boxListSorted.get(index - 1)) > 0) {
            Box temp = n.boxListSorted.get(index);
            n.boxListSorted.set(index, n.boxListSorted.get(index - 1));
            n.boxListSorted.set(index - 1, temp);
            index --;
        }
        while (index < n.boxListSorted.size() - 1 && n.boxListSorted.get(index).compareTo(n.boxListSorted.get(index + 1)) < 0) {
            Box temp = n.boxListSorted.get(index);
            n.boxListSorted.set(index, n.boxListSorted.get(index + 1));
            n.boxListSorted.set(index + 1, temp);
            index ++;
        }*/
        //List<Box> l = new ArrayList<>(n.boxListSorted);
        Collections.sort(n.boxListSorted);
        /*if (!l.equals(n.boxListSorted)) {
            System.err.println("ERR");
            System.err.println(i);
            System.err.println(lll);
            System.err.println(n.boxListSorted);
            System.err.println(l);
            System.err.println(index);
        }*/
        //System.err.println("B: " + n.boxListSorted);
        //System.err.println(index);
    }

    public ArrayList<Node> getExpandedNodes() {
        return getExpandedNodes(null, null, null);
    }

    public ArrayList<Node> getExpandedNodes(int[][] penaltyMap, Set<Position> illegalPositions, Position endPos) {
        ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);

        HashSet<SearchState> statesOfInterest = new HashSet<>();

        char[][] map = BDI.recreateMap(this, true, true, false);

        Set<SearchState> seen = new HashSet<>();
        Queue<SearchState> queue = new ArrayDeque<>();
        SearchState startState = new SearchState(agent);
        queue.add(startState);
        seen.add(startState);
        for (Box b : boxList) {
            int distanceSquared = (int) (Math.pow(b.row - agent.row, 2) + Math.pow(b.col - agent.col, 2));
            if (distanceSquared == 1 && b.color == agent.color) {
                startState = new SearchState(agent);
                startState.setBoxPosition(new Position(b));
                startState.setBox(b);
                startState.setPulling(true);
                queue.add(startState);
                seen.add(startState);

                startState = startState.copy();
                startState.swapPullPush();
                queue.add(startState);
                seen.add(startState);
            }
        }

        int[][] deltas = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            SearchState ss = queue.poll();
            seen.add(ss);
            Position p = ss.getPosition();

            if (endPos != null && p.row == endPos.row && p.col == endPos.col) {
                statesOfInterest.add(ss);
            }

            // Collections.shuffle(Arrays.asList(deltas), RND);

            if (!IS_SINGLE && penaltyMap != null && penaltyMap[p.row][p.col] <= 0) {
                statesOfInterest.add(ss);
            }

            if (ss.getBox() == null) {
                if (illegalPositions != null) {
                    for (Position illegalPos : illegalPositions) {
                        for (int i = 0; i < BDI.deltas.length; i++) {
                            int dr = BDI.deltas[i][0]; // delta row
                            int dc = BDI.deltas[i][1]; // delta col
                            if (illegalPos.row + dr < 0 || illegalPos.col + dc < 0 || illegalPos.row + dr >= Node.MAX_ROW || illegalPos.col + dc >= Node.MAX_COL) {
                                continue;
                            }
                            Position possiblePos = new Position(illegalPos.row + dr, illegalPos.col + dc);
                            if (!illegalPositions.contains(possiblePos)) {
                                statesOfInterest.add(ss);
                            }
                        }
                    }
                }

                for (int i = 0; i < BDI.deltas.length; i++) {
                    int dr = BDI.deltas[i][0]; // delta row
                    int dc = BDI.deltas[i][1]; // delta col
                    if (p.row + dr < 0 || p.col + dc < 0 || p.row + dr >= Node.MAX_ROW || p.col + dc >= Node.MAX_COL) {
                        continue;
                    }
                    if (Character.isAlphabetic(map[p.row + dr][p.col + dc])) { // box
                        statesOfInterest.add(ss);
                    }
                    SearchState newState =new SearchState(
                            new Position(p.row + dr, p.col + dc), ss, new Command(BDI.deltasDirection[i]));
                    if (!seen.contains(newState) && map[p.row + dr][p.col + dc] == ' ') {
                        seen.add(newState);
                        queue.add(newState);
                    }
                }
            } else {
                if (goals[ss.getBoxPosition().row][ss.getBoxPosition().col] == Character.toLowerCase(ss.getBox().letter)) {
                    statesOfInterest.add(ss);
                } else if (penaltyMap != null) {
                    if (penaltyMap[ss.getBoxPosition().row][ss.getBoxPosition().col] <= 0) {
                        statesOfInterest.add(ss);
                    }
                }

                int numberOfWallsAroundBox = 0;
                for (int i = 0; i < BDI.deltas.length; i++) {
                    int dr = BDI.deltas[i][0]; // delta row
                    int dc = BDI.deltas[i][1]; // delta col
                    if (ss.getBoxPosition().row + dr < 0 || ss.getBoxPosition().col + dc < 0 || ss.getBoxPosition().row + dr >= Node.MAX_ROW || ss.getBoxPosition().col + dc >= Node.MAX_COL) {
                        continue;
                    }
                    if (map[ss.getBoxPosition().row + dr][ss.getBoxPosition().col + dc] == '+') {
                        numberOfWallsAroundBox += 1;
                    }
                }
                if (numberOfWallsAroundBox >= 3) {
                    statesOfInterest.add(ss); // TODO: Can we just continue after this?
                }

                int numberOfWallsAroundAgent = 0;
                for (int i = 0; i < BDI.deltas.length; i++) {
                    int dr = BDI.deltas[i][0]; // delta row
                    int dc = BDI.deltas[i][1]; // delta col
                    if (p.row + dr < 0 || p.col + dc < 0 || p.row + dr >= Node.MAX_ROW || p.col + dc >= Node.MAX_COL) {
                        continue;
                    }
                    if (map[p.row + dr][p.col + dc] == '+') {
                        numberOfWallsAroundAgent += 1;
                    }
                }
                if (numberOfWallsAroundAgent >= 3) {
                    statesOfInterest.add(ss);
                }

                if (Node.walls[ss.getBoxPosition().row][ss.getBoxPosition().col])
                map[ss.getBox().row][ss.getBox().col] = ' ';
                boolean canTurnAround = false;
                int countClearSpotsAround = 0;
                for (int i = 0; i < BDI.deltas.length; i++) {
                    int dr = BDI.deltas[i][0]; // delta row
                    int dc = BDI.deltas[i][1]; // delta col
                    if (p.row + dr < 0 || p.col + dc < 0 || p.row + dr >= Node.MAX_ROW || p.col + dc >= Node.MAX_COL) {
                        continue;
                    }
                    if (map[p.row + dr][p.col + dc] == ' ') {
                        countClearSpotsAround += 1;
                    }
                }
                if (countClearSpotsAround >= 3) { // one of them is the box
                    canTurnAround = true;
                }
                for (int i = 0; i < BDI.deltas.length; i++) {
                    int dr = BDI.deltas[i][0]; // delta row
                    int dc = BDI.deltas[i][1]; // delta col
                    if (ss.isPulling()) {
                        if (p.row + dr < 0 || p.col + dc < 0 || p.row + dr >= Node.MAX_ROW || p.col + dc >= Node.MAX_COL) {
                            continue;
                        }
                        Position newP = new Position(p.row + dr, p.col + dc);
                        if (map[p.row + dr][p.col + dc] == ' ' && !ss.getBoxPosition().equals(newP)) {
                            // we can pull this way
                            SearchState newState = new SearchState(newP, ss, new Command(Type.Pull, BDI.deltasDirection[i], getDirectionFromPositions(p, ss.getBoxPosition())));
                            newState.setBoxPosition(p);
                            addToQueue(canTurnAround, newState, queue, seen, ss, statesOfInterest);
                        }
                    } else {
                        // pushing
                        Position bp = ss.getBoxPosition();
                        if (bp.row + dr < 0 || bp.col + dc < 0 || bp.row + dr >= Node.MAX_ROW || bp.col + dc >= Node.MAX_COL) {
                            continue;
                        }
                        Position newP = new Position(bp.row + dr, bp.col + dc);
                        if (map[bp.row + dr][bp.col + dc] == ' ' && !newP.equals(p)) {
                            SearchState newState = new SearchState(bp, ss, new Command(Type.Push, getDirectionFromPositions(p, bp), BDI.deltasDirection[i]));
                            newState.setBoxPosition(newP);
                            addToQueue(canTurnAround, newState, queue, seen, ss, statesOfInterest);
                        }
                    }
                }
                map[ss.getBox().row][ss.getBox().col] = ss.getBox().letter;
            }
        }
        for (SearchState ss : statesOfInterest) {
            Node n = this.ChildNode();
            quickApplyCommands(n, ss.backTrack(), ss);
            expandedNodes.add(n);
        }
        Collections.shuffle(expandedNodes, RND);
//        t1 += System.nanoTime() - t;
        return expandedNodes;
        /*
        for (Command c : Command.EVERY) {
            Node n = getNodeFromCommand(c);
            if (n != null) {
                expandedNodes.add(n);
            }
        }

        Collections.shuffle(expandedNodes, RND);
        return expandedNodes;*/
    }

    private Command.Dir getDirectionFromPositions(Position p1, Position p2) {
        if (p1.row > p2.row) {
            return Command.Dir.N;
        } else if (p1.row < p2.row) {
            return Command.Dir.S;
        } else if (p1.col > p2.col) {
            return Command.Dir.W;
        } else {
            return Command.Dir.E;
        }
    }

    private void addToQueue(boolean canTurnAround, SearchState newState, Queue<SearchState> queue, Set<SearchState> seen, SearchState ss, Set<SearchState> statesOfInterest) {
        if (canTurnAround) {
            newState.setHasTurnedAround();
        }
        if (seen.add(newState)) {
            queue.add(newState);
            if (canTurnAround && !ss.hasTurnedAround()) {
                statesOfInterest.add(newState);
            }
        }
        if (canTurnAround) {
            newState = newState.copy();
            newState.swapPullPush();
            if (seen.add(newState)) {
                queue.add(newState);
            }
        }
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
        copy.boxListSorted = this.boxListSorted;
        copy.boxListSortedHashCode = boxListSortedHashCode;
        return copy;
    }

    public List<Command> extractPlanNew() {
        if (actions == null || parent == null) {
            return new ArrayList<>();
        } else {
            List<Command> plan = parent.extractPlanNew();
            plan.addAll(actions);
            return plan;
        }
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
        Node other = (Node) obj;
        if (!this.agent.equals(other.agent))
            return false;
        //if (!boxList.equals(other.boxList))
        if (!boxListSorted.equals(other.boxListSorted))
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
                } else if (Node.walls[row][col] && goal != null) {
                    s.append("@");
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
