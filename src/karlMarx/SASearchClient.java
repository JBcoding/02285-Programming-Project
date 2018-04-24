package karlMarx;

import java.io.IOException;
import java.util.*;

public class SASearchClient extends SearchClient {

    private String strategyArg;
    private Strategy strategy;

    private List<Command> solution = new ArrayList<Command>();

    public List<Command> Search(String strategyArg, List<Node> initialStates) throws IOException {
        Node.IS_SINGLE = true;

        if (initialStates.size() != 1) {
            throw new IllegalArgumentException("There can only be one initial state in single agent levels.");
        }
        
        this.strategyArg = strategyArg;
        
        System.err.format("Search single agent starting with strategy %s.\n", strategyArg.toString());
        
        Node currentState = initialStates.get(0);
        BDI.removeUnreachableBoxesFromBoxlist(currentState);
        Goal currentGoal;
        Set<Goal> currentGoals = new HashSet<Goal>();

        goalStateLoop:
        while (!currentState.isGoalState()) {
            //System.err.println(currentState);

            currentGoal = BDI.getGoal(currentState);
            currentGoals.add(currentGoal);
            System.err.println("NEXT GOAL: " + currentGoal);
            List<Box> boxesToMove = null;
            int[][] penaltyMap = null;
            while (true) {
                if (currentState.isGoalState()){
                    removeRepetitiveStates(initialStates.get(0));
                    return solution;
                }
                Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);
                if (data != null && data.a.size() > 0) {
                    boxesToMove = data.a;
                    penaltyMap = data.b;
                    //System.err.println(currentState);
                    //System.err.println("MOVE BOXES: " + boxesToMove);
                    Node lastNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, null);
                    List<Command> plan = lastNode.extractPlanNew();
                    if (plan.size() == 0) {
                        continue goalStateLoop;
                    }

                    solution.addAll(plan);
                    currentState = lastNode;
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;
                } else {
                    break;
                }
            }
            //System.err.println(currentState);
            //System.err.println("SOLVE GOAL: " + currentGoal);
            Node lastNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, null);
            List<Command> plan = lastNode.extractPlanNew();
            if (plan.size() == 0) {
                continue;
            }
            solution.addAll(plan);
            currentState = lastNode;
            // This is a new initialState so it must not have a parent for isInitialState method to work
            currentState.parent = null;
        }

        removeRepetitiveStates(initialStates.get(0));
        return solution;
    }

    private Node getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap, List<Box> boxesNotToMoveMuch) {
        switch (strategyArg) {
        case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch)); break;
        case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch)); break;
        case "-greedy": /* Fall-through */
        default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, boxesNotToMoveMuch));
        }
        if (!strategy.isExplored(state)) {
            strategy.addToFrontier(state);
        }
        
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
//            if (iterations == 0) {
//                System.err.println(leafNode);
//            }

            if (leafNode.isGoalState(currentGoals, boxesToMove, penaltyMap)) {
                return leafNode;
            }
            strategy.addToExplored(leafNode);
            for (Node n : leafNode.getExpandedNodes(penaltyMap)) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }
            
            iterations++;
        }
    }

    private void removeRepetitiveStates(Node initialState) {
        long t = System.currentTimeMillis();
        Node n = initialState.ChildNode();

        Map<Node, Integer> observedNodes = new HashMap<Node, Integer>(); // Nodes and at which step they were observed

        observedNodes.put(n, 0);

        List<Pair<Integer, Integer>> slicesToRemove = new ArrayList<Pair<Integer, Integer>>();

        for (int stepsTaken = 0; stepsTaken < solution.size(); ) {
            n = n.getNodeFromCommand(solution.get(stepsTaken));
            stepsTaken++;

            if (observedNodes.containsKey(n)) {
                int startOfSlice = observedNodes.get(n).intValue();
                for (int j = startOfSlice; j < stepsTaken; j++) {
                    solution.remove(startOfSlice); // TODO: Shouldn't have to created a new array every time. Do something clever here
                }
                stepsTaken = startOfSlice;
            } else {
                observedNodes.put(n, stepsTaken);
            }
        }
        System.err.println("Time: " + (System.currentTimeMillis() - t));
    }

    @Override
    public String searchStatus() {
        return strategy.searchStatus() + " --- " + Node.t1;
    }
}
