package karlMarx;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class SASearchClient extends SearchClient {

    private String strategyArg;

    private List<Command> solution = new ArrayList<Command>();

    public List<Command> Search(String strategyArg, List<Node> initialStates) throws IOException {
        Node.IS_SINGLE = true;

        if (initialStates.size() != 1) {
            throw new IllegalArgumentException("There can only be one initial state in single agent levels.");
        }

        this.strategyArg = strategyArg;

        //System.err.format("Search single agent starting with strategy %s.\n", strategyArg.toString());

        Node currentState = initialStates.get(0);
        BDI.removeUnreachableBoxesFromBoxlist(currentState);
        Goal currentGoal;
        Set<Goal> currentGoals = new HashSet<Goal>();

        goalStateLoop:
        while (!currentState.isGoalState()) {
            //System.err.println(currentState);

            Pair<Pair<Goal, Position>, Pair<List<Box>, Set<Position>>> goalInfo = BDI.getGoal(currentState);
            currentGoal = goalInfo.a.a;

            List<Box> boxesToMoveFirst = goalInfo.b.a;
            Set<Position> illegalPositionsFirst = goalInfo.b.b;
            int[][] penaltyMapFirst = new int[Node.MAX_ROW][Node.MAX_COL];
            if (boxesToMoveFirst.size() != 0) {
                penaltyMapFirst = BDI.calculatePenaltyMap(currentState, illegalPositionsFirst, boxesToMoveFirst.size(), true);

                //System.err.println("Moving boxes out of the way: " + boxesToMoveFirst);
                Node lastNode = getPlan(currentState, currentGoals, boxesToMoveFirst, penaltyMapFirst, null, null, false);
                List<Command> plan = lastNode.extractPlanNew();
                solution.addAll(plan);
                currentState = lastNode;
                // This is a new initialState so it must not have a parent for isInitialState method to work
                currentState.parent = null;
            }

            currentGoals.add(currentGoal);

            //System.err.println("NEXT GOAL: " + currentGoal + " (" + currentGoals.size() + " / " + Node.goalSet.size() + ")");
            List<Box> boxesToMove = null;
            int[][] penaltyMap = null;
            while (true) {
                Node lastNode = null;
                Pair<List<Box>, int[][]> data1 = BDI.boxToMove(currentState, currentGoal);
                Pair<List<Box>, int[][]> data2 = BDI.boxToMoveWithAgent(currentState, currentGoal, currentState.agent);
                if (data1 != null && data1.a.size() > 0 && data2 != null && data2.a.size() > 0) {
                    lastNode = getPlan(currentState, currentGoals, data1.a, data1.b, data2.a, data2.b, null, illegalPositionsFirst);
                } else if (data1 != null && data1.a.size() > 0) {
                    lastNode = getPlan(currentState, currentGoals, data1.a, data1.b, null, illegalPositionsFirst, false);
                } else if (data2 != null && data2.a.size() > 0) {
                    lastNode = getPlan(currentState, currentGoals, data2.a, data2.b, null, illegalPositionsFirst, true);
                }
                else {
                    break;
                }

                if (lastNode != null) {
                    List<Command> plan = lastNode.extractPlanNew();
                    if (plan.size() == 0) {
                        continue goalStateLoop;
                    }

                    solution.addAll(plan);
                    currentState = lastNode;
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;
                    if (currentState.isGoalState()) {
                        removeRepetitiveStates(initialStates.get(0));
                        return solution;
                    }
                }

            }

            //System.err.println(currentState);
            //System.err.println("SOLVE GOAL: " + currentGoal);
            Node lastNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, goalInfo.a.b, null, false);

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

    @Override
    public String searchStatus() {
        return null;
    }

    private Node getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove1, int[][] penaltyMap1, List<Box> boxesToMove2, int[][] penaltyMap2, Position endPosition, Set<Position> illegalPositions) {
        Semaphore semaphore = new Semaphore(0);
        final Node[] temp1 = {null};
        final Node[] temp2 = {null};
        Thread threadTrue = new Thread() {
            public void run() {
                try {
                    temp1[0] = getPlan(state, currentGoals, boxesToMove2, penaltyMap2, endPosition, illegalPositions, true);
                    semaphore.release();
                } catch (Exception e) {
                }
            }
        };
        Thread threadFalse = new Thread() {
            public void run() {
                try {
                    temp2[0] = getPlan(state, currentGoals, boxesToMove1, penaltyMap1, endPosition, illegalPositions, false);
                    semaphore.release();
                } catch (Exception e) {
                }
            }
        };

        threadTrue.start();
        threadFalse.start();

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
        }

        threadTrue.interrupt();
        threadFalse.interrupt();

        Node solve;
        if (temp1[0] != null) {
            solve = temp1[0];
        } else {
            solve = temp2[0];
        }

        return solve;
    }

    private Node getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap, Position endPosition, Set<Position> illegalPositions, boolean extra) {

        //System.err.println(state);


        Strategy strategy;
        switch (strategyArg) {
            case "-astar":
                strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, null, illegalPositions, extra));
                break;
            case "-wastar":
                strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, null, illegalPositions, extra));
                break;
            case "-greedy": /* Fall-through */
            default:
                strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, null, illegalPositions, extra));

        }
        if (!strategy.isExplored(state)) {
            strategy.addToFrontier(state);
        }

        int iterations = 0;
        while (true) {
            if (iterations == 100) {
                //System.err.println(searchStatus());
                iterations = 0;
            }

            if (Thread.interrupted()) {
                return null;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            Node leafNode = (Node) strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState(currentGoals, boxesToMove, penaltyMap, endPosition, illegalPositions)) {
                //System.err.println(searchStatus());
                return leafNode;
            }
            strategy.addToExplored(leafNode);

            for (Node n : leafNode.getExpandedNodes(penaltyMap, null, endPosition, boxesToMove)) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            /*
            System.err.println(n);
            System.err.println(n.h + " " + n.g() + " " + (n.h + n.g()));
            System.err.println(ff);
            System.err.println("\n");
            */
            }

            iterations++;
        }
    }

    private void removeRepetitiveStates(Node initialState) {
        Node n = initialState.ChildNode();
//        List<Pair<Integer, Integer>> allSlicesToRemove = new ArrayList<Pair<Integer, Integer>>();

        Map<Node, Integer> observedNodes = new HashMap<Node, Integer>(); // Nodes and at which step they were observed
        observedNodes.put(n, 0);

        for (int stepsTaken = 0; stepsTaken < solution.size(); ) {
            n = n.getNodeFromCommand(solution.get(stepsTaken));
            stepsTaken++;

            if (observedNodes.containsKey(n)) {
                int startOfSlice = observedNodes.get(n).intValue();
                for (int j = startOfSlice; j < stepsTaken; j++) {
                    solution.remove(startOfSlice); // Removed because we shouldn't have to create a new array every time. Replaced with code below this loop
                }
//                allSlicesToRemove.add(new Pair<Integer, Integer>(startOfSlice, stepsTaken));
                stepsTaken = startOfSlice;
            } else {
                observedNodes.put(n, stepsTaken);
            }
        }
//        boolean[] removedIndices = new boolean[solution.size()];
//        for (Pair<Integer, Integer> slice : allSlicesToRemove) {
//            for (int i = slice.a; i < slice.b; i++) {
//                removedIndices[i] = true;
//            }
//        }
//        List newSolution = new ArrayList<Command>();
//        for (int i = 0; i < solution.size(); i++) {
//            if (!removedIndices[i]) {
//                newSolution.add(solution.get(i));
//            }
//        }
//        solution = newSolution;
    }
}
