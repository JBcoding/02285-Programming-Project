package karlMarx;

import java.io.IOException;
import java.util.*;

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
            while (true) {
                Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);
                if (data != null && data.a.size() > 0) {
                    List<Box> boxesToMove = data.a;
                    int[][] penaltyMap = data.b;
                    List<Box> boxesNotToMoveMuch = BDI.getBoxesToGoal(currentGoal, currentState);
                    System.err.println("MOVE BOXES: " + boxesToMove);
                    Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch);
                    solution.addAll(plan);
                    currentState = plan.getLast();
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;
                } else {
                    break;
                }
            }
            System.err.println(currentState);
            System.err.println("SOLVE GOAL: " + currentGoal);
            currentGoals.add(currentGoal);
            Deque<Node> plan = getPlan(currentState, currentGoals, null, null, null);
            solution.addAll(plan);
            currentState = plan.getLast();
            // This is a new initialState so it must not have a parent for isInitialState method to work
            currentState.parent = null;
        }
        
        return solution;
    }

    private Deque<Node> getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap, List<Box> boxesNotToMoveMuch) {
        switch (strategyArg) {
        case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch)); break;
        case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch)); break;
        case "-greedy": /* Fall-through */
        default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch));
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

            if (leafNode.isGoalState(currentGoals, boxesToMove, penaltyMap)) {
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
