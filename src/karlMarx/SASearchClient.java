package karlMarx;

import java.io.IOException;
import java.util.*;

import util.Pair;

public class SASearchClient extends SearchClient {

    private String strategyArg;
    private Strategy strategy;

    public List<Node> Search(String strategyArg, List<Node> initialStates) throws IOException {
        Node.IS_SINGLE = true;

        if (initialStates.size() != 1) {
            throw new IllegalArgumentException("There can only be one initial state in single agent levels.");
        }
        
        this.strategyArg = strategyArg;
        
        System.err.format("Search single agent starting with strategy %s.\n", strategyArg.toString());
        
        Node currentState = initialStates.get(0);
        Goal currentGoal;
        Set<Goal> currentGoals = new HashSet<Goal>();
        
        List<Node> solution = new LinkedList<Node>();
        while (!currentState.isGoalState()) {
            System.err.println(currentState);

            currentGoal = BDI.getGoal(currentState);
            System.err.println("NEXT GOAL: " + currentGoal);
            
            List<Box> boxesForCurrentGoal = BDI.getBoxesForGoal(currentGoal, currentState);
            
            final Position currentGoalPos = currentGoal.copy();
            
            boxesForCurrentGoal.sort(new Comparator<Box>() {
                @Override
                public int compare(Box b1, Box b2) {
                    int distB1 = Heuristic.shortestDistance[b1.row][b1.col][currentGoalPos.row][currentGoalPos.col];
                    int distB2 = Heuristic.shortestDistance[b2.row][b2.col][currentGoalPos.row][currentGoalPos.col];
                    if (distB1 == -1 && distB2 != -1) {
                        return Integer.MAX_VALUE;
                    }
                    if (distB2 == -1 && distB1 != -1) {
                        return Integer.MIN_VALUE;
                    }
                    return distB1 - distB2;
                }
            });
            Deque<Box> boxesForCurrentGoalDeque = new ArrayDeque<Box>(boxesForCurrentGoal);
                            
            while (!boxesForCurrentGoalDeque.isEmpty()) {
                Map<Position, Pair<Set<Position>, int[][]>> penaltyMaps = new HashMap<Position, Pair<Set<Position>, int[][]>>();
                Box boxForGoal = boxesForCurrentGoalDeque.poll();
                Deque<Node> plan = getPlan(currentState, currentGoals, penaltyMaps, boxForGoal, currentGoal);
                if (plan != null) {
                    solution.addAll(plan);
                    currentGoals.add(currentGoal);
                    currentState = plan.getLast();
                    // This is a new initialState so it is not allowed to have a parent for isInitialState method to work
                    currentState.parent = null;
                    break;
                }
            }
            
            if (currentState.parent != null) { // i.e. no solution found
                return null;
            }
        }
        
        return solution;
    }

    private Deque<Node> getPlan(Node state, Set<Goal> currentGoals, Map<Position, Pair<Set<Position>, int[][]>> penaltyMaps, Box box, Goal currentGoal) {
        switch (strategyArg) {
        case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, penaltyMaps, box, currentGoal)); break;
        case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, penaltyMaps, box, currentGoal)); break;
        case "-greedy": /* Fall-through */
        default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, penaltyMaps, box, currentGoal));
        }
        
        strategy.addToFrontier(state);

        int iterations = 0;
        while (true) {
            if (iterations == 10000) {
                System.err.println(searchStatus());
                iterations = 0;
            }
            
            if (strategy.frontierIsEmpty()) {
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState(currentGoals, currentGoal, penaltyMaps, box.id)) {
                return leafNode.extractPlan();
            }

            strategy.addToExplored(leafNode);
            for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }
            
            iterations++;
        }
    }

    @Override
    public String searchStatus() {
        return strategy.searchStatus();
    }
}
