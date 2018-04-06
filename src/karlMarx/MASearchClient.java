package karlMarx;

import java.io.IOException;
import java.util.*;

import util.Pair;

public class MASearchClient {

    private Strategy[] strategies;
    private String strategyArg;

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

        HashMap<Goal, HashSet<Color>> solvableByColor = new HashMap<>();

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

        ArrayList<HashSet<Position>> illegalPositions = new ArrayList<>();
        for (int i = 0; i < initialStates.size(); i++) {
            illegalPositions.add(new HashSet<>());
        }

        while (solvedGoals.size() < Node.goalSet.size()) {
            boolean solvedSomething = false;

            for (int i = 0; i < initialStates.size(); i++) {
                Node currentState = initialStates.get(i);

                Node.walls[currentState.agent.row][currentState.agent.col] = false;
                currentState.boxList = lastBoxList;

                System.err.println("Agent: " + currentState.agent);
                System.err.println("STARTING FROM:");
                System.err.println(currentState);

                // Prune boxList based on solvable goals

                HashSet<Goal> solvableGoals = new HashSet<>();

                Queue<Position> queue = new ArrayDeque<>();
                queue.add(currentState.agent);

                HashSet<Position> seen = new HashSet<>();

                ArrayList<Box> boxList = new ArrayList<>();

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
                                System.err.println("Goal: " + goal + " blocked by agent.");
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

                if (solvableGoals.isEmpty()) {
                    System.err.println("No solvable goals for agent: " + currentState.agent.id);
                    Node.walls[currentState.agent.row][currentState.agent.col] = true;
                    continue;
                }

                ArrayList<Box> removed = new ArrayList<>();
                for (Box box : currentState.boxList) {
                    if (!boxList.contains(box)) {
                        removed.add(box);
                    }
                }

                currentState.boxList = boxList;

                Set<Goal> currentGoals = new HashSet<>();
                for (Goal goal : solvedGoals) {
                    if (solvableByColor.get(goal).contains(currentState.agent.color) && !removed.contains(goal)) {
                        currentGoals.add(goal);
                    }
                }

<<<<<<< d29ba6caa96fb39ceebacc4a8235c60d831a70c8
                HashSet<Position> illegals = illegalPositions.get(currentState.agent.id);
                if (!illegals.isEmpty()) {
                    Deque<Node> plan = getAwayPlan(currentState, currentGoals, illegals);
=======
            List<Node> solution = new LinkedList<Node>();
            while (!currentState.isGoalState(solvableGoals, null, null, -1, null)) {
                currentGoal = BDI.getGoal(currentState, solvableGoals);
                Pair<List<Box>, int[][]> data = BDI.boxesToRemoveFromPathToGoal(currentState, currentGoal);
                System.err.println("NEXT GOAL: " + currentGoal);
                if (data != null && data.a.size() > 0) {
                    List<Box> boxesToMove = data.a;
                    int[][] penaltyMap = data.b;
                    System.err.println("MOVE BOXES: " + boxesToMove);
                    Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap);
>>>>>>> Temp commit for several penalty maps
                    if (plan == null) {
                        System.err.println("UNABLE TO MOVE AWAY FROM: " + illegals);
                        return null;
                    }
                    currentState = plan.getLast();
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;

                    pm.mergePlan(currentState.agent.id, plan);
                    solvedSomething = true;
                } else {
                    Goal currentGoal = BDI.getGoal(currentState, solvableGoals);
                    System.err.println("NEXT GOAL: " + currentGoal);

                    List<Box> boxesToMove = null;
                    int[][] penaltyMap = null;
                    while (true) {
                        Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);
                        if (data != null && data.a.size() > 0) {
                            boxesToMove = data.a;
                            penaltyMap = data.b;
                            System.err.println(currentState);
                            for (int[] arr : penaltyMap) {
                                System.err.println(Arrays.toString(arr));
                            }
                            System.err.println("MOVE BOXES: " + boxesToMove);
                            Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap);
                            pm.mergePlan(currentState.agent.id, plan);
                            currentState = plan.getLast();
                            // This is a new initialState so it must not have a parent for isInitialState method to work
                            currentState.parent = null;
                        } else {
                            break;
                        }
                    }

                    System.err.println("SOLVE GOAL: " + currentGoal);

                    currentGoals.add(currentGoal);
                    Deque<Node> plan = getPlan(currentState, currentGoals, null, null);

                    if (plan == null) {
                        System.err.println("UNABLE TO SOLVE GOAL: " + currentGoal);
                        Node.walls[currentState.agent.row][currentState.agent.col] = true;
                        lastBoxList = currentState.boxList;
                        lastBoxList.addAll(removed);
                        continue;
                    }
                    pm.mergePlan(currentState.agent.id, plan);

                    currentState = plan.getLast();
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;

                    solvedGoals.add(currentGoal);
                    solvedSomething = true;
                }

                Node.walls[currentState.agent.row][currentState.agent.col] = true;

                lastBoxList = currentState.boxList;
                lastBoxList.addAll(removed);

                initialStates.set(i, currentState);
            }

            if (!solvedSomething) {
                return null;
            }

            System.err.println("Running agents again.");
        }


        return pm.getPlan();
    }

    private Deque<Node> getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap) {
        Strategy strategy;

        switch (strategyArg) {
            case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, null)); break;
            case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, null)); break;
            case "-greedy": /* Fall-through */
            default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, null));
        }

        System.err.println(currentGoals);

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

        System.err.println(currentGoals);

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

            if (leafNode.isGoalState(currentGoals, null, null) &&
                    !illegalPositions.contains(new Position(leafNode.agent.row, leafNode.agent.col))) {
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
