package karlMarx;

import java.io.IOException;
import java.util.*;

import static karlMarx.BDI.deltas;

public class MASearchClient {

    private Strategy[] strategies;
    private String strategyArg;
    private HashMap<Goal, HashSet<Color>> solvableByColor;

    public Command[][] Search(String strategyArg, List<Node> initialStates) throws IOException {
        Node.IS_SINGLE = false;
        this.strategyArg = strategyArg;

        strategies = new Strategy[initialStates.size()];
        initialStates.sort( new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                return n1.agent.id - n2.agent.id;
            }
        });
        switch (strategyArg) {
            case "-astar":
                for (Node initialState : initialStates) {
                    strategies[initialState.agent.id] = new StrategyBestFirst(new AStar(initialState));
                }
                break;
            case "-wastar":
                for (Node initialState : initialStates) {
                    strategies[initialState.agent.id] = new StrategyBestFirst(new WeightedAStar(initialState, 5));
                }
                break;
            case "-greedy": // Fall-through
            default:
                for (Node initialState : initialStates) {
                    strategies[initialState.agent.id] = new StrategyBestFirst(new Greedy(initialState));
                }
        }

        System.err.format("Search multi agent starting with strategy %s.\n", strategyArg);

        solvableByColor = new HashMap<>();

        for (Goal g : Node.goalSet) {
            HashSet<Color> colors = solvableByColor.get(g);
            if (colors == null) {
                colors = new HashSet<>();
            }

            for (Box b : initialStates.get(0).boxList) {
                if (Character.toLowerCase(b.letter) == g.letter) {
                    colors.add(b.color);
                }
            }

            solvableByColor.put(g, colors);
        }

        HashSet<Goal> solvedGoals = new HashSet<>();
        ArrayList<Box> lastBoxList = initialStates.get(0).boxList;

        for (Node initialState : initialStates) {
            Node.walls[initialState.agent.row][initialState.agent.col] = true;
        }

        MAPlanMerger pm = new MAPlanMerger(initialStates.get(0), initialStates.size(), initialStates);

        HashSet<Box> illegalBoxes = new HashSet<>();
        Set<Position> illegalPositions = new HashSet<>();
        int illegalByAgent = -1;

        while (!initialStates.get(0).isGoalState()) {
            boolean solvedSomething = false;

            for (int i = 0; i < initialStates.size(); i++) {
                if (illegalByAgent == i) {
                    illegalPositions.clear();
                    illegalBoxes.clear();
                    illegalByAgent = -1;
                }

                Node currentState = initialStates.get(i);

                Node.walls[currentState.agent.row][currentState.agent.col] = false;
                currentState.boxList = lastBoxList;

//                System.err.println("Agent: " + currentState.agent);
//                System.err.println("STARTING FROM:");
//                System.err.println(currentState);

                Pair<HashSet<Goal>, ArrayList<Box>> pruneData = pruneBoxList(currentState, solvedGoals);
                HashSet<Goal> solvableGoals = pruneData.a;
                ArrayList<Box> removed = pruneData.b;

                Set<Goal> currentGoals = new HashSet<>();
                for (Goal goal : solvedGoals) {
                    boolean coveredByRemoved = false;

                    for (Box box : removed) {
                        if (box.isOn(goal)) {
                            coveredByRemoved = true;
                        }
                    }

                    if (solvableByColor.get(goal).contains(currentState.agent.color) && !coveredByRemoved) {
                        currentGoals.add(goal);
                    }
                }

                try {
                    if (!illegalBoxes.isEmpty()) {
//                        System.err.println("Trying to clear path.");
//                        System.err.println(illegalPositions);

                        HashSet<Position> clearableIllegalPositions = new HashSet<>();
                        for (Position pos : illegalPositions) {
                            Box maybeBox = currentState.findBox(pos.row, pos.col);
                            if (!Node.walls[pos.row][pos.col] &&
                                    (maybeBox == null || maybeBox.color == currentState.agent.color)) {
                                clearableIllegalPositions.add(pos);
                            }
                        }

                        // TODO: use penalty map
                        Deque<Node> plan = getAwayPlan(currentState, currentGoals, clearableIllegalPositions);
                        if (plan == null) {
//                            System.err.println("Unable to clear path.");
                            continue;
                        }

                        if (plan.isEmpty()) {
                            continue;
                        }

                        currentState = plan.getLast();

                        // This is a new initialState so it must not have a parent for isInitialState method to work
                        currentState.parent = null;

                        pm.mergePlan(currentState.agent.id, plan);
                        solvedSomething = true;
                    } else if (solvableGoals.isEmpty()) {
//                        System.err.println("No solvable goals /for agent: " + currentState.agent.id);
                        continue;
                    } else {
                        Goal currentGoal = BDI.getGoal(currentState, solvableGoals);
//                        System.err.println("NEXT GOAL: " + currentGoal);

                        // Remove walls from agent positions to enable BFS
                        HashSet<Position> agentPositions = new HashSet<>();
                        ArrayList<Box> agentsAsBoxes = new ArrayList<>();
                        for (Node s : initialStates) {
                            if (s.agent.id == currentState.agent.id) {
                                continue;
                            }

                            agentPositions.add(new Position(s.agent.row, s.agent.col));
                            // Boxes representing agents have letter 'A'
                            agentsAsBoxes.add(new Box(s.agent.row, s.agent.col, 'A', Color.BLUE));
                            Node.walls[s.agent.row][s.agent.col] = false;
                        }

                        int prevSize = currentState.boxList.size();
                        currentState.boxList.addAll(agentsAsBoxes);
                        currentState.boxList.addAll(removed);

                        // TODO: also look at whether we can actually get to a box that solves it
                        Pair<List<Box>, Set<Position>> blocking = BDI.boxesOnThePathToGoal(
                                currentGoal,
                                new Position(currentState.agent.row, currentState.agent.col),
                                currentState
                        );

                        currentState.boxList.subList(prevSize, currentState.boxList.size()).clear();

                        illegalBoxes = new HashSet<>(blocking.a);
                        final Color agentColor = currentState.agent.color;
                        illegalBoxes.removeIf(box -> box.color == agentColor);
                        illegalPositions = blocking.b;

                        // Restore walls
                        for (Position pos : agentPositions) {
                            Node.walls[pos.row][pos.col] = true;
                        }

//                        System.err.println("ILLEGAL BOXES: " + illegalBoxes);

                        if (!illegalBoxes.isEmpty()) {
                            illegalByAgent = i;

                            final HashSet<Position> finalIllegalPositions = new HashSet<>(illegalPositions);
                            solvedGoals.removeIf(goal -> finalIllegalPositions.contains(new Position(goal.row, goal.col)));
                            continue;
                        }

                        Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);

                        List<Box> boxesToMove = null;
                        int[][] penaltyMap = null;

                        while (true) {
                            if (data.a.size() > 0) {
                                boxesToMove = data.a;
                                penaltyMap = data.b;
//                                System.err.println("MOVE BOXES: " + boxesToMove);
                                Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap);
                                if (plan == null) {
//                                    System.err.println("UNABLE TO MOVE BOXES: " + boxesToMove);
                                    continue;
                                }
                                currentState = plan.getLast();
                                // This is a new initialState so it must not have a parent for isInitialState method to work
                                currentState.parent = null;

                                pm.mergePlan(currentState.agent.id, plan);
                            } else {
                                break;
                            }
                        }

//                        System.err.println("SOLVE GOAL: " + currentGoal);

                        currentGoals.add(currentGoal);
                        Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap);

                        if (plan == null) {
//                            System.err.println("UNABLE TO SOLVE GOAL: " + currentGoal);
                            continue;
                        }
                        pm.mergePlan(currentState.agent.id, plan);

                        currentState = plan.getLast();
                        // This is a new initialState so it must not have a parent for isInitialState method to work
                        currentState.parent = null;

                        solvedGoals.add(currentGoal);
                        solvedSomething = true;
                    }
                } finally {
                    Node.walls[currentState.agent.row][currentState.agent.col] = true;

                    lastBoxList = currentState.boxList;
                    lastBoxList.addAll(removed);
                }

                initialStates.set(i, currentState);
            }

            if (!solvedSomething && !initialStates.get(0).isGoalState()) {
                return null;
            }

//            System.err.println("Running agents again.");
        }

        return pm.getPlan();
    }

    Pair<HashSet<Goal>, ArrayList<Box>> pruneBoxList(Node currentState, HashSet<Goal> solvedGoals) {
        HashSet<Goal> solvableGoals = new HashSet<>();
        ArrayList<Box> boxList = new ArrayList<>();

        Queue<Position> queue = new ArrayDeque<>();
        queue.add(currentState.agent);

        HashSet<Position> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            Box maybeBox = currentState.findBox(curr.row, curr.col);
            if (maybeBox != null) {
                boxList.add(maybeBox);
            }

            if (Node.goals[curr.row][curr.col] >= 'a' && Node.goals[curr.row][curr.col] <= 'z') {
                Goal goal = Node.findGoal(curr.row, curr.col);

                if (solvableByColor.get(goal).contains(currentState.agent.color) &&
                        !solvedGoals.contains(goal)) {
                    if (Node.walls[goal.row][goal.col]) {
//                        System.err.println("Goal: " + goal + " blocked by agent.");
                    }
                    solvableGoals.add(goal);
                }
            }

            for (Position pos : curr.getNeighbours()) {
                if (Node.inBounds(pos) && !Node.walls[pos.row][pos.col] && !seen.contains(pos)) {
                    queue.add(pos);
                    seen.add(pos);
                }
            }
        }

        ArrayList<Box> removed = new ArrayList<>();
        for (Box box : currentState.boxList) {
            if (!boxList.contains(box)) {
                removed.add(box);
            }
        }

        currentState.boxList = boxList;

        return new Pair<>(solvableGoals, removed);
    }

    private Deque<Node> getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap) {
        Strategy strategy;

        switch (strategyArg) {
            case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, null)); break;
            case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, null)); break;
            case "-greedy": /* Fall-through */
            default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, null));
        }

//        System.err.println(currentGoals);

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

    private Deque<Node> getAwayPlan(Node state, Set<Goal> currentGoals, Set<Position> illegalPositions) {
        Strategy strategy;

        switch (strategyArg) {
            case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, null, null, null)); break;
            case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, null, null, null)); break;
            case "-greedy": /* Fall-through */
            default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, null, null, null));
        }

//        System.err.println(currentGoals);

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

            boolean illegalBox = false;
            for (Box box : leafNode.boxList) {
                if (box.color == leafNode.agent.color && illegalPositions.contains(new Position(box.row, box.col))) {
                    illegalBox = true;
                }
            }

            // TODO: getting no help from the heuristic right now
            // Can I just pass the penalty map?
            if (leafNode.isGoalState(currentGoals, null, null) &&
                    !illegalPositions.contains(new Position(leafNode.agent.row, leafNode.agent.col)) &&
                    !illegalBox) {
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

    public String searchStatus() { {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < strategies.length; i++) {
            Strategy strategy = strategies[i];
            s.append("Status for agent ");
            s.append(i);
            s.append(": ");
            s.append(strategy.searchStatus());
        }
        return s.toString();
    }
    }

}
