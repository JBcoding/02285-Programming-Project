package karlMarx;

import java.util.*;

public abstract class Heuristic implements Comparator<Node> {

    protected Goal[] prioritisedgoals;
    protected int[][][][] shortestDistance;
    protected int maxdist = 0;
    protected int[] isgoalletter = new int[26];
    protected HashMap<Goal, HashSet<Color>> solvableByColor;

    protected Set<Goal> currentGoals;
    protected List<Box> boxesToMove;
    protected int[][] penaltyMap;

    public Heuristic(Node initialState) {
        // Here's a chance to pre-process the static parts of the level.

        // Find all goals.
        ArrayList<Goal> goalcells = new ArrayList<>();
        ArrayList<Goal> prioritisedgoals = new ArrayList<>();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                if (Node.goals[row][col] >= 'a' && Node.goals[row][col] <= 'z') {
                    goalcells.add(new Goal(row, col, Node.goals[row][col]));
                    // isgoalletter is a simple array keeping track of which letters occur on goal cells,
                    // so that we can quickly discard the boxes having other letters
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

        // All pair shortest distances (by BFS).
        this.shortestDistance = new int[Node.MAX_ROW][Node.MAX_COL][Node.MAX_ROW][Node.MAX_COL];
        //int maxdist = 0;
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
                                this.shortestDistance[root_row][root_col][row][col] = c.dist;
                                maxdist = Math.max(maxdist, c.dist);
                            }
                        }
                    }
                }
            }
        }
    }

    /* Below are some heuristics:
     1) hPairingDistance sums the distance of the agent to the nearest relevant box,
     the distance from that box to the nearest relevant goal,
     the distance from that goal to the next nearest relevant box, etc.
     It hence computes a pairing of boxes with goals
     and computes the total distance required to put all boxes into the designated goals
     (assuming shortest distances are always available, that is, that no other objects block the shortest paths).
     It adds a goal count heuristics which either adds 1 for each unsatisfied goal or maxdist for each unsatisfied goal.
     Adding maxdist gives a very high penalty for moving a box away from a corresponding goal cell.
     It gives quicker and shorter solutions when there are no potentiel conflicts
     and all boxes can immediately be moved straight to their corresponding goals.
     However, it sometimes breaks down when there are conflicts
     and fulfilled goals can potentially block for other goals.
     Adding maxdist for every unfulfilled goal makes warmup/SALazarus unsolvable in 150s and 8GB,
     whereas adding 1 for every fulfilled goal makes it solvable in under 1s.
     With goal count factor 1, a level like SAsorting can however not be solved.
     Goal count factor 5 makes it solvable, but makes SAbispebjergHospital unsolvable.
     A goal count factor of 2 seems to be the sweet spot.

     2) hGoalCount is a simple goal count of unfulfilled goals.
     It is mainly there for pedagogical purposes.
     Running greedy on this heuristics makes the client do a random walk until getting a box into a goal
     and then it starts doing another random walk to find the next box
     (leaving the original one in its goal position).
     It works acceptably on small, relatively simple levels (like SACrunch)
     and levels that are not too big and have no conflicts (like SAsoko3_12 and SAanagram).
     It can't solve bigger soko3 levels or something like SALazarus.


     3) hGoalCountPlusNearest is a simplification of hPairingDistance
     where only the distance to the nearest relevant box and its distance to a relevant goal is added to the goal count.
     None of the heuristics are admissible,
     so it doesn't really make sense to compare them in terms of which one dominates the other.
     They each have strengths and weaknesses on different types of levels.
     hGoalCountPlusNearest easily solves SAsorting, that hPairingDistance with goalcount factor 1 can't.
     But hGoalCountPlusNearest can't solve SALazarus, no matter how the goalcount factor is set.
     */

    public int hPairingDistance(Node n) {
        /* to improve this further, I could e.g.:
         1) Look at actual shortest paths: make sure the all-pairs-shortest path algorithm output actual shortest paths,
         and then shortest paths can be checked for whether the contain other goal cells,
         e.g. The current heuristics work very well when there are no conflicts,
         but breaks down when there is a lot of conflicts between subgoals.

         2) Compute prioritised goals. A goal is non-prioritised if it blocks access of the agent to other goals.
         This can be computed by checking whether putting a box on the goal
         would make some unfulfilled goals be in a connected component distinct from the one containing the agent.
         This would however require computing connected components in each state,
         that is, run a BFS or DFS, which might turn out to be too computationally expensive.

         3) Choose more efficient data structures if possible,
         in particular the HashSet for active goals and active boxes.
         */
        n.h = 1;
        // start searching from the agent position
        int currentRow = n.agent.row;
        int currentCol = n.agent.col;
        // initialise activegoals with all unsatisfied goals
        HashSet<Goal> activegoals = new HashSet<Goal>();
        
        if (currentGoals == null) { // Multi agent
            activegoals.addAll(Node.goalSet);
        } else { // Single agent 
            activegoals.addAll(currentGoals);
        }
        for (Box b : n.boxList) {
            if (Character.toLowerCase(b.letter) == Node.goals[b.row][b.col]) {
                activegoals.remove(new Goal(b.row, b.col, Character.toLowerCase(b.letter)));
            }
        }
        for (Goal g : activegoals) {
            n.h += 2;
        }


        // initialise activeboxes with all boxes not on goal cells
        HashSet<Box> activeboxes = new HashSet<Box>();
        for (Box box : n.boxList) {
            if (Node.goals[box.row][box.col] != Character.toLowerCase(box.letter) &&
                    box.color == n.agent.color) {
                activeboxes.add(box);
            }
        }

        while (!activegoals.isEmpty()) {
            Box nearestBox = null;
            Goal nearestGoal = null;
            while (nearestGoal == null) {
                // find the nearest active box to coordinates (currentRow, currentCol) and find the distance to it
                int distToBox = Integer.MAX_VALUE;
                for (Box b : activeboxes) {
                    if (this.shortestDistance[b.row][b.col][currentRow][currentCol] < distToBox) {
                        nearestBox = b;
                        distToBox = this.shortestDistance[b.row][b.col][currentRow][currentCol];
                    }
                }

                // remove the chosen box from the list of active boxes
                activeboxes.remove(nearestBox);
                // find the nearest same-letter active goal to the chosen box (if exists)
                int distToGoal = Integer.MAX_VALUE;
                for (Goal g : activegoals) {
                    if (Character.toLowerCase(nearestBox.letter) == g.letter
                            && nearestBox.color == n.agent.color
                            && this.shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col] < distToGoal) {
                        nearestGoal = g;
                        distToGoal = this.shortestDistance[g.row][g.col][nearestBox.row][nearestBox.col];
                    }
                }
            }
            // remove the chosen goal from the list of active goals
            activegoals.remove(nearestGoal);
            // add to the heuristics the number of actions required to go from a
            // cell neighbouring (currentRow, currentCol) to the nearest box and push that box to nearest goal
            n.h = n.h
                    + this.shortestDistance[currentRow][currentCol][nearestBox.row][nearestBox.col]
                    + this.shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col] - 2;
            currentRow = nearestGoal.row;
            currentCol = nearestGoal.col;
        }

        if (boxesToMove != null) {
            for (Box b1 : boxesToMove) {
                for (Box b2 : n.boxList) {
                    if (b1.id == b2.id) {
                        n.h += penaltyMap[b2.row][b2.col];
                    }
                }
            }
        }

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

    // Not used atm
	/*
    public int hPairingDistanceDeadlockPenalty(Node n) {
        /* to improve this further, I could e.g.:
         1) Look at actual shortest paths: make sure the all-pairs-shortest path algorithm output actual shortest paths,
         and then shortest paths can be checked for whether the contain other goal cells,
         e.g. The current heuristics work very well when there are no conflicts,
         but breaks down when there is a lot of conflicts between subgoals.

         2) Compute prioritised goals. A goal is non-prioritised if it blocks access of the agent to other goals.
         This can be computed by checking whether putting a box on the goal
         would make some unfulfilled goals be in a connected component distinct from the one containing the agent.
         This would however require computing connected components in each state,
         that is, run a BFS or DFS, which might turn out to be too computationally expensive.

         3) Choose more efficient data structures if possible,
         in particular the HashSet for active goals and active boxes.
         *//*
        n.h = 1;
        // start searching from the agent position
        int currentRow = n.agentRow;
        int currentCol = n.agentCol;
        // initialise activegoals with all unsatisfied goals
        HashSet<Goal> activegoals = new HashSet<Goal>();
        
        for (Goal g: goalcells) {
            if (Character.toLowerCase(n.boxes[g.position.row][g.position.col]) != g.letter) {
                activegoals.add(g);
                // goal count heuristics: add maxdist for all unsatisfied goals
                n.h = n.h + 2; // add between 1 and maxdist;
            }
            
        }
         
        // initialise activeboxes with all boxes not on goal cells
        HashSet<Box> activeboxes = new HashSet<Box>();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                char letter = Character.toLowerCase(n.boxes[row][col]);
                if (letter > 0 && letter != Node.goals[row][col]) { // box at [row][col]
                    activeboxes.add(new Box(row,col,letter));
                    if ((Node.walls[row+1][col] && Node.walls[row][col+1]) ||
                       (Node.walls[row-1][col] && Node.walls[row][col-1]) ||
                        (Node.walls[row+1][col] && Node.walls[row][col-1]) ||
                        (Node.walls[row-1][col] && Node.walls[row][col+1]))
                    {
                        return Integer.MAX_VALUE;
                    }
                }
            }
        }
        while (!activegoals.isEmpty()) {
            Box nearestBox = null;
            Goal nearestGoal = null;
            while (nearestGoal==null) {
                // find the nearest active box to coordinates (currentRow, currentCol) and find the distance to it
                int distToBox = Integer.MAX_VALUE;
                for (Box b: activeboxes) {
                    if (this.shortestDistance[b.position.row][b.position.col][currentRow][currentCol] < distToBox) {
                        nearestBox = b;
                        distToBox = this.shortestDistance[b.position.row][b.position.col][currentRow][currentCol];
                    }
                }
                // remove the chosen box from the list of active boxes
                activeboxes.remove(nearestBox);
                // find the nearest same-letter active goal to the chosen box (if exists)
                int distToGoal = Integer.MAX_VALUE;
                for (Goal g: activegoals) {
                    if (nearestBox.letter == g.letter
                            && this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col] < distToGoal) {
                        nearestGoal = g;
                        distToGoal = this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col];
                    }
                }
            }
            // remove the chosen goal from the list of active goals
            activegoals.remove(nearestGoal);
            // add to the heuristics the number of actions required to go from a
            // cell neighbouring (currentRow, currentCol) to the nearest box and push that box to nearest goal
            n.h = n.h
                    + this.shortestDistance[currentRow][currentCol][nearestBox.row][nearestBox.col]
                    + this.shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col] - 2;
            currentRow = nearestGoal.row;
            currentCol = nearestGoal.col;
        }
        return n.h;
    }
    
    public int hPairingDistanceFurthestGoal(Node n) {
        n.h = 1;
        // start searching from the agent position
        int currentRow = n.agentRow;
        int currentCol = n.agentCol;
        // initialise activegoals with all unsatisfied goals
        HashSet<Goal> activegoals = new HashSet<Goal>();
        
        for (Goal g: goalcells) {
            if (Character.toLowerCase(n.boxes[g.position.row][g.position.col]) != g.letter) {
                activegoals.add(g);
                // goal count heuristics: add maxdist for all unsatisfied goals
                n.h = n.h + 2; // add between 1 and maxdist;
            }
            
        }
        
        // initialise activeboxes with all boxes not on goal cells
        HashSet<Box> activeboxes = new HashSet<Box>();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                char letter = Character.toLowerCase(n.boxes[row][col]);
                if (letter > 0 && letter != Node.goals[row][col]) { // box at [row][col]
                    activeboxes.add(new Box(row,col,letter));
                }
            }
        }
        while (!activegoals.isEmpty()) {
            Box nearestBox = null;
            Goal furthestGoal = null;
            while (furthestGoal==null) {
                // find the nearest active box to coordinates (currentRow, currentCol) and find the distance to it
                int distToBox = Integer.MAX_VALUE;
                for (Box b: activeboxes) {
                    if (this.shortestDistance[b.position.row][b.position.col][currentRow][currentCol] < distToBox) {
                        nearestBox = b;
                        distToBox = this.shortestDistance[b.position.row][b.position.col][currentRow][currentCol];
                    }
                }
                // remove the chosen box from the list of active boxes
                activeboxes.remove(nearestBox);
                // find the furtherst away same-letter active goal to the chosen box (if exists)
                int distToGoal = 0; // Integer.MAX_VALUE;
                for (Goal g: activegoals) {
                    if (nearestBox.letter == g.letter
                            && this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col] > distToGoal) {
                        furthestGoal = g;
                        distToGoal = this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col];
                    }
                }
            }
            // remove the chosen goal from the list of active goals
            activegoals.remove(furthestGoal);
            // add to the heuristics the number of actions required to go from a
            // cell neighbouring (currentRow, currentCol) to the nearest box and push that box to nearest goal
            n.h = n.h
                    + this.shortestDistance[currentRow][currentCol][nearestBox.row][nearestBox.col]
                    + this.shortestDistance[nearestBox.row][nearestBox.col][furthestGoal.row][furthestGoal.col] - 2;
            currentRow = furthestGoal.row;
            currentCol = furthestGoal.col;
        }
        return n.h;
    }
    
    public int hGoalCount(Node n) { // pure goal count heuristics
        n.h = 0;
        for (Goal g: goalcells) {
            if (Character.toLowerCase(n.boxes[g.position.row][g.position.col]) != g.letter) n.h = n.h + 1;
        }
        return n.h;
    }
    
    public int hGoalCountPlusNearest(Node n) {
        // this heuristics adds:
        // 1) a goal count multiplied by 2*maxdist,
        // the maximal number of actions required to take a box to a goal if the path is not blocked
        // 2) the distance to the nearest relevant box and its distance to the nearest relevant goal
        // It has not been optimised for performance (e.g. choice of data structures)
        if (n.isGoalState()) return n.h = 0;
        n.h = 0;
        // initialise activegoals with all unsatisfied goals
        HashSet<Goal> activegoals = new HashSet<Goal>();
        
        for (Goal g: goalcells) {
            if (Character.toLowerCase(n.boxes[g.position.row][g.position.col]) != g.letter) {
                activegoals.add(g);
                // goal count heuristics: add maxdist for all unsatisfied goals
                n.h = n.h + 2*maxdist;
            }
            
        }
        
        // initialise activeboxes with all boxes not on goal cells and for which a goal cell of that letter exists
        //System.err.println();
        HashSet<Box> activeboxes = new HashSet<Box>();
        for (int row = 0; row < Node.MAX_ROW; row++) {
            for (int col = 0; col < Node.MAX_COL; col++) {
                char letter = Character.toLowerCase(n.boxes[row][col]);
                if (letter > 0 && isgoalletter[letter-'a']==1 && letter != Node.goals[row][col]) { // box at [row][col]
                    activeboxes.add(new Box(row,col,letter));
                   // System.err.print("("+col+","+row+")");
                }
            }
        }
        Box nearestBox = null;
        Goal nearestGoal = null;
        while (nearestGoal==null) {
            // find the nearest active box to agent and find the distance to it
            int distToBox = Integer.MAX_VALUE;
            for (Box b: activeboxes) {
                if (this.shortestDistance[b.position.row][b.position.col][n.agentRow][n.agentCol] < distToBox) {
                        nearestBox = b;
                        distToBox = this.shortestDistance[b.position.row][b.position.col][n.agentRow][n.agentCol];
                    }
                }
                // remove the chosen box from the list of active boxes
                activeboxes.remove(nearestBox);
                // find the nearest same-letter active goal to the chosen box (if exists)
                int distToGoal = Integer.MAX_VALUE;
                for (Goal g: activegoals) {
                    if (nearestBox.letter == g.letter
                            && this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col] < distToGoal) {
                        nearestGoal = g;
                        distToGoal = this.shortestDistance[g.position.row][g.position.col][nearestBox.row][nearestBox.col];
                    }
                }
            }
            n.h = n.h
                    + this.shortestDistance[n.agentRow][n.agentCol][nearestBox.row][nearestBox.col]
                    + this.shortestDistance[nearestBox.row][nearestBox.col][nearestGoal.row][nearestGoal.col] - 1;
        return n.h;
    }
*/
    public int h(Node n) {
        //return 0;
        return hPairingDistance(n); // default heuristics. Best performing on warmup levels
        //return hPairingDistanceFurthestGoal(n);
        //return hPairingDistanceDeadlockPenalty(n);
        //return hGoalCount(n);
        // return hGoalCountPlusNearest(n);
    }


    public abstract int f(Node n);

    @Override
    public int compare(Node n1, Node n2) {
        return this.f(n1) - this.f(n2);
    }

}

class AStar extends Heuristic {
    public AStar(Node initialState, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap) {
        this(initialState);
        this.currentGoals = currentGoals;
        this.boxesToMove = boxesToMove;
        this.penaltyMap = penaltyMap;
    }

    public AStar(Node initialState) {
        super(initialState);
    }

    @Override
    public int f(Node n) {
        return n.g() + this.h(n);
    }

    @Override
    public String toString() {
        return "A* evaluation";
    }
}

class WeightedAStar extends Heuristic {
    private int W;

    public WeightedAStar(Node initialState, int W, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap) {
        this(initialState, W);
        this.currentGoals = currentGoals;
        this.boxesToMove = boxesToMove;
        this.penaltyMap = penaltyMap;
    }

    public WeightedAStar(Node initialState, int W) {
        super(initialState);
        this.W = W;
    }

    @Override
    public int f(Node n) {
        return n.g() + this.W * this.h(n);
    }

    @Override
    public String toString() {
        return String.format("WA*(%d) evaluation", this.W);
    }
}

class Greedy extends Heuristic {
    public Greedy(Node initialState, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap) {
        this(initialState);
        this.currentGoals = currentGoals;
        this.boxesToMove = boxesToMove;
        this.penaltyMap = penaltyMap;
    }

    public Greedy(Node initialState) {
        super(initialState);
    }

    @Override
    public int f(Node n) {
        return this.h(n);
    }

    @Override
    public String toString() {
        return "Greedy evaluation";
    }
}
