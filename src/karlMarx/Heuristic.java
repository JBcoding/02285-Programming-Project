package karlMarx;

import java.util.*;

public abstract class Heuristic implements Comparator<GeneralNode> {

    public static int[][][][] shortestDistance;
    protected static int maxdist = 0;
    protected int[] isgoalletter = new int[26];
    protected HashMap<Goal, HashSet<Color>> solvableByColor;

    protected Set<Goal> currentGoals;
    protected HashSet<Integer> boxesToMove;
    protected int[][] penaltyMap;
    protected List<Box> boxesNotToMoveMuch;
    protected List<Integer> possibleActiveBoxIndices = new ArrayList<Integer>();
    protected Set<Goal> masterActivegoals = new HashSet<Goal>();
    protected Set<Position> illegalPositions;

    protected boolean clearPathForOtherAgent;

    static {

        shortestDistance = new int[Node.MAX_ROW][Node.MAX_COL][Node.MAX_ROW][Node.MAX_COL];
        Cell[][] cells = new Cell[Node.MAX_ROW][Node.MAX_COL];
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                if (!Node.walls[row][col])
                    cells[row][col] = new Cell();
            }
        }
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                Cell c = cells[row][col];
                if (c != null) {
                    if (row - 1 >= 0) c.up = cells[row - 1][col];
                    if (row + 1 < Node.MAX_ROW) c.down = cells[row + 1][col];
                    if (col - 1 >= 0) c.left = cells[row][col - 1];
                    if (col + 1 < Node.MAX_COL) c.right = cells[row][col + 1];
                }
            }
        }
        for (int root_row = 0; root_row < Node.MAX_ROW; root_row++) {
            for (int root_col = 0; root_col < Node.MAX_COL; root_col++) {
                Cell root = cells[root_row][root_col];
                if (root != null) {
                    ArrayDeque<Cell> queue = new ArrayDeque<>();

                    for (int row = 0; row < Node.MAX_ROW; row++) {
                        for (int col = 0; col < Node.MAX_COL; col++) {
                            Cell c = cells[row][col];
                            if (c != null) c.dist = -1;
                        }
                    }

                    root.dist = 0;
                    queue.add(root);

                    while (!queue.isEmpty()) {
                        Cell c = queue.poll();

                        if (c.up != null && c.up.dist == -1) {
                            c.up.dist = c.dist + 1;
                            queue.add(c.up);
                        }
                        if (c.down != null && c.down.dist == -1) {
                            c.down.dist = c.dist + 1;
                            queue.add(c.down);
                        }
                        if (c.left != null && c.left.dist == -1) {
                            c.left.dist = c.dist + 1;
                            queue.add(c.left);
                        }
                        if (c.right != null && c.right.dist == -1) {
                            c.right.dist = c.dist + 1;
                            queue.add(c.right);
                        }
                    }

                    for (int row = 0; row < Node.MAX_ROW; row++) {
                        for (int col = 0; col < Node.MAX_COL; col++) {
                            Cell c = cells[row][col];
                            if (c != null) {
                                shortestDistance[root_row][root_col][row][col] = c.dist;
                                maxdist = Math.max(maxdist, c.dist);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void initMasterActiveGoalsBoxes(Node initialState) {
        if (currentGoals == null) {
            masterActivegoals.addAll(Node.goalSet);
        } else {
            masterActivegoals.addAll(currentGoals);
        }

        for (int i = 0; i < initialState.boxList.size(); i++) {
            Box b = initialState.boxList.get(i);
            char letter = Character.toLowerCase(b.letter);
            if (b.color == initialState.agent.color) {
                for (Goal goal : masterActivegoals) {
                    if (goal.letter == letter) {
                        possibleActiveBoxIndices.add(i);
                        break;
                    }
                }
            }
        }
    }

    public Heuristic(Node initialState) {

        ArrayList<Goal> goalcells = new ArrayList<>();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                if (Node.goals[row][col] >= 'a' && Node.goals[row][col] <= 'z') {
                    goalcells.add(new Goal(row, col, Node.goals[row][col]));

                    isgoalletter[Node.goals[row][col] - 'a'] = 1;

                }
            }
        }

        solvableByColor = new HashMap<>();

        for (Goal g : Node.goalSet) {
            HashSet<Color> colors = solvableByColor.get(g);
            if (colors == null) {
                colors = new HashSet<>();
            }

            for (Box b : initialState.boxList) {
                if (Character.toLowerCase(b.letter) == g.letter) {
                    colors.add(b.color);
                }
            }

            solvableByColor.put(g, colors);
        }

    }

    public int h1(Node n) {
        if (n.h != -1) {
            return n.h;
        }

        n.h = 1;

        // start searching from the agent position
        int currentRow = n.agent.row;
        int currentCol = n.agent.col;


        HashSet<Box> activeboxes = new HashSet<Box>();
        HashSet<Goal> activegoals = new HashSet<Goal>(masterActivegoals);
        for (int index : possibleActiveBoxIndices) {
            Box b = n.boxList.get(index);
            char letter = Character.toLowerCase(b.letter);
            if (letter == Node.goals[b.row][b.col]) {
                activegoals.remove(new Goal(b.row, b.col, letter));
            } else {
                activeboxes.add(b);
            }
        }

        boolean[] goalsMissingCompletion = new boolean[26];
        for (Goal g : activegoals) {
            goalsMissingCompletion[g.letter - 'a'] = true;
        }
        Iterator<Box> i = activeboxes.iterator();
        while (i.hasNext()) {
            Box b = i.next();
            if (!goalsMissingCompletion[b.letter - 'A']) {
                i.remove();
            }
        }

        activegoals.removeIf(goal -> !solvableByColor.get(goal).contains(n.agent.color));

        n.h += 2 * activegoals.size();

        Set<Goal> tempActiveGoals = activegoals;
        Set<Box> tempActiveBoxes = activeboxes;

        while (!tempActiveGoals.isEmpty()) {
            Box nearestBox = null;
            Goal nearestGoal = null;
            while (nearestGoal == null) {

                int distToBox = Integer.MAX_VALUE;
                for (Box b : tempActiveBoxes) {
                    if (shortestDistance[b.row][b.col][currentRow][currentCol] < distToBox) {
                        nearestBox = b;
                        distToBox = shortestDistance[b.row][b.col][currentRow][currentCol];
                    }
                }


                tempActiveBoxes.remove(nearestBox);

                int distToGoal = Integer.MAX_VALUE;
                for (Goal g : tempActiveGoals) {
                    if (Character.toLowerCase(nearestBox.letter) == g.letter
                            && nearestBox.color == n.agent.color
                            && shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col] < distToGoal) {
                        nearestGoal = g;
                        distToGoal = shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col];
                    }
                }
            }

            tempActiveGoals.remove(nearestGoal);
            //System.err.println(nearestBox);
            //System.err.println(nearestGoal);
            //System.err.println(shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col]);
            //System.err.println();
            n.h = n.h
                    + shortestDistance[currentRow][currentCol][nearestBox.row][nearestBox.col]
                    + shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col] - 2;
            // System.err.println(nearestGoal + " " + nearestBox);
            currentRow = nearestGoal.row;
            currentCol = nearestGoal.col;
        }
        n.h *= 2; // TODO: Is this nice? // yes it is, do not remove - MOB // Is this still nice?

        if (illegalPositions != null && illegalPositions.size() > 0) {
            for (Box b : n.boxList) {
                if (illegalPositions.contains(new Position(b))) {
                    n.h -= 25;
                }
            }
        }

        if (boxesToMove != null) {
            for (Box b1 : n.boxList) {
                if (boxesToMove.contains(b1.id)) {
                    n.h += 5 * penaltyMap[b1.row][b1.col];

                    if (clearPathForOtherAgent && penaltyMap[b1.row][b1.col] > 0) {
                        n.h += shortestDistance[n.agent.row][n.agent.col][b1.row][b1.col] * 100;
                    }
                }
            }
        }


//        n.h += uselessCellsMap[n.agent.row][n.agent.col] * 10;
//        for (Box box : n.boxList) {
//            n.h += uselessCellsMap[box.row][box.col];
//        }

        // Add weight for getting any box closer to its goal
        /*activeboxes = new HashSet<Box>();
        for (Box b : n.boxList) {
            if (Character.toLowerCase(b.letter) == Node.goals[b.row][b.col]) {
                activeboxes.remove(b);
            }
        }
        for (Box b : activeboxes) {
            Node.goalSet.stream()
                .filter(g -> Character.toLowerCase(b.letter) == g.letter)
                .forEach(g -> n.h += this.shortestDistance[b.row][b.col][g.row][g.col]);
        }*/

        return n.h;
    }

    public int h1(MultiBodyNode n) {
        if (n.h != -1) {
            return n.h;
        }

        n.h = 1;
        HashSet<Box> activeboxes = new HashSet<Box>();
        HashSet<Goal> activegoals = new HashSet<Goal>(masterActivegoals);
        for (int index : possibleActiveBoxIndices) {
            Box b = n.boxList.get(index);
            char letter = Character.toLowerCase(b.letter);
            if (letter == Node.goals[b.row][b.col]) {
                activegoals.remove(new Goal(b.row, b.col, letter));
            } else {
                activeboxes.add(b);
            }
        }

        boolean[] goalsMissingCompletion = new boolean[26];
        for (Goal g : activegoals) {
            goalsMissingCompletion[g.letter - 'a'] = true;
        }
        Iterator<Box> i = activeboxes.iterator();
        while (i.hasNext()) {
            Box b = i.next();
            if (!goalsMissingCompletion[b.letter - 'A']) {
                i.remove();
            }
        }

        n.h += 2 * activegoals.size();

        Set<Goal> tempActiveGoals = activegoals;
        Set<Box> tempActiveBoxes = activeboxes;

        while (!tempActiveGoals.isEmpty()) {
            Box nearestBox;
            Goal nearestGoal = null;
            int distToBox = Integer.MAX_VALUE;
            if (tempActiveBoxes.isEmpty()) {
                break;
            }
            nearestBox = tempActiveBoxes.iterator().next();

            tempActiveBoxes.remove(nearestBox);
            int distToGoal = Integer.MAX_VALUE;
            for (Goal g : tempActiveGoals) {
                if (Character.toLowerCase(nearestBox.letter) == g.letter
                        && shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col] < distToGoal) {
                    nearestGoal = g;
                    distToGoal = shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col];
                }
            }
            tempActiveGoals.remove(nearestGoal);
            n.h = n.h
                    + shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col] - 2;
        }
        return n.h;
    }

    public int h(GeneralNode n) {
        //return 0;
        if (n.getClass() == Node.class) {
            return h1((Node)n);
        } else {
            return h1((MultiBodyNode) n);
        }
    }

    public abstract int f(GeneralNode n);

    @Override
    public int compare(GeneralNode n1, GeneralNode n2) {
        return this.f(n1) - this.f(n2);
    }

}

class AStar extends Heuristic {
    public AStar(Node initialState, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap,
                 List<Box> boxesNotToMoveMuch, Set<Position> illegalPositions, boolean clearPathForOtherAgent) {
        this(initialState);
        this.currentGoals = currentGoals;
        this.boxesToMove = new HashSet<>();
        if (boxesToMove != null) {
            for (Box b : boxesToMove) {
                this.boxesToMove.add(b.id);
            }
        }
        this.penaltyMap = penaltyMap;
        this.boxesNotToMoveMuch = boxesNotToMoveMuch;
        this.illegalPositions = illegalPositions;
        this.clearPathForOtherAgent = clearPathForOtherAgent;
        initMasterActiveGoalsBoxes(initialState);
    }

    public AStar(Node initialState) {
        super(initialState);
    }

    @Override
    public int f(GeneralNode n) {
        return n.g() + this.h(n);
    }

    @Override
    public String toString() {
        return "A* evaluation";
    }
}

class WeightedAStar extends Heuristic {
    private int W;

    public WeightedAStar(Node initialState, int W, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap,
                         List<Box> boxesNotToMoveMuch, Set<Position> illegalPositions, boolean clearPathForOtherAgent) {
        this(initialState, W);
        this.currentGoals = currentGoals;
        this.boxesToMove = new HashSet<>();
        if (boxesToMove != null) {
            for (Box b : boxesToMove) {
                this.boxesToMove.add(b.id);
            }
        }
        this.penaltyMap = penaltyMap;
        this.boxesNotToMoveMuch = boxesNotToMoveMuch;
        this.illegalPositions = illegalPositions;
        this.clearPathForOtherAgent = clearPathForOtherAgent;
        initMasterActiveGoalsBoxes(initialState);
    }

    public WeightedAStar(Node initialState, int W) {
        super(initialState);
        this.W = W;
    }

    @Override
    public int f(GeneralNode n) {
        return n.g() + this.W * this.h(n);
    }

    @Override
    public String toString() {
        return String.format("WA*(%d) evaluation", this.W);
    }
}

class Greedy extends Heuristic {
    public Greedy(Node initialState, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap,
                  List<Box> boxesNotToMoveMuch, Set<Position> illegalPositions, boolean clearPathForOtherAgent) {
        this(initialState);
        this.currentGoals = currentGoals;
        this.boxesToMove = new HashSet<>();
        if (boxesToMove != null) {
            for (Box b : boxesToMove) {
                this.boxesToMove.add(b.id);
            }
        }
        this.penaltyMap = penaltyMap;
        this.boxesNotToMoveMuch = boxesNotToMoveMuch;
        this.illegalPositions = illegalPositions;
        this.clearPathForOtherAgent = clearPathForOtherAgent;
        initMasterActiveGoalsBoxes(initialState);
    }

    public Greedy(Node initialState) {
        super(initialState);
    }

    @Override
    public int f(GeneralNode n) {
        return this.h(n);
    }

    @Override
    public String toString() {
        return "Greedy evaluation";
    }
}