package karlMarx;

import java.io.IOException;
import java.util.*;

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

        // TODO: MAEvilCorp shows that we actually need a stack of these or similar.
        // Note: illegalBoxes can also contain boxes with color AGENT
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

                System.err.println("Agent: " + currentState.agent);
                System.err.println("STARTING FROM:");
                System.err.println(currentState);

                Pair<HashSet<Goal>, ArrayList<Box>> pruneData = pruneBoxList(currentState, initialStates, solvedGoals);
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
                        System.err.println("Trying to clear path.");

                        HashSet<Position> clearableIllegalPositions = new HashSet<>();
                        for (Position pos : illegalPositions) {
                            Box maybeBox = currentState.findBox(pos.row, pos.col);
                            if (!Node.walls[pos.row][pos.col] &&
                                    (maybeBox == null || maybeBox.color == currentState.agent.color)) {
                                clearableIllegalPositions.add(pos);
                            }
                        }

                        System.err.println(clearableIllegalPositions);

                        ArrayList<Box> boxesToMove = new ArrayList<>();
                        for (Box box : currentState.boxList) {
                            if (illegalBoxes.contains(box) && box.color == currentState.agent.color) {
                                boxesToMove.add(box);
                            }
                        }

                        if (boxesToMove.isEmpty() && !illegalPositions.contains(new Position(currentState.agent))) {
                            System.err.println("NOTHING TO CLEAR");
                            solvedSomething = true;
                            continue;
                        }

                        System.err.println(boxesToMove);

                        ArrayList<Pair<Position, Character>> removedGoals = new ArrayList<>();

                        for (Position pos : clearableIllegalPositions) {
                            if (Node.goals[pos.row][pos.col] >= 'a' && Node.goals[pos.row][pos.col] <= 'z') {
                                removedGoals.add(new Pair<>(pos, Node.goals[pos.row][pos.col]));
                                Node.goals[pos.row][pos.col] = ' ';
                            }
                        }

                        int[][] penaltyMap = BDI.calculatePenaltyMap(currentState, clearableIllegalPositions,
                                boxesToMove.size() + 1);

                        for (Pair<Position, Character> posGoal : removedGoals) {
                            Node.goals[posGoal.a.row][posGoal.a.col] = posGoal.b;
                        }

                        Node lastNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, true, illegalPositions);
                        if (lastNode == null) {
                            System.err.println("Unable to clear path.");
                            continue;
                        }

                        List<Command> plan = lastNode.extractPlanNew();
                        if (plan.isEmpty()) {
                            continue;
                        }

                        currentState = lastNode;

                        // This is a new initialState so it must not have a parent for isInitialState method to work
                        currentState.parent = null;

                        pm.mergePlan(currentState.agent.id, plan);
                        solvedSomething = true;
                    } else if (solvableGoals.isEmpty()) {
//                        System.err.println("No solvable goals /for agent: " + currentState.agent.id);
                        continue;
                    } else {
                        Goal currentGoal = BDI.getGoal(currentState, solvableGoals);

                        if (currentGoal == null) {
                            continue;
                        }

                        System.err.println("NEXT GOAL: " + currentGoal);

                        // Remove walls from agent positions to enable BFS
                        HashSet<Position> agentPositions = new HashSet<>();
                        HashSet<Box> agentsAsBoxes = new HashSet<>();
                        for (Node s : initialStates) {
                            if (s.agent.id == currentState.agent.id) {
                                continue;
                            }

                            agentPositions.add(new Position(s.agent));
                            // Boxes representing agents have color AGENT
                            agentsAsBoxes.add(new Box(s.agent.row, s.agent.col, 'A', Color.AGENT));
                            Node.walls[s.agent.row][s.agent.col] = false;
                        }

                        ArrayList<Box> oldBoxes = new ArrayList<>(currentState.boxList);

                        currentState.boxList.addAll(agentsAsBoxes);
                        currentState.boxList.addAll(removed);

                        // Try to find the easiest accessible box matching the goal

                        Pair<Box, Pair<List<Box>, Set<Position>>> bestBoxData =
                                bestBox(currentState, agentsAsBoxes, currentGoal);

                        Box bestBox = bestBoxData.a;

                        Pair<List<Box>, Set<Position>> boxToGoalData = BDI.boxesOnThePathToGoal(
                                currentGoal,
                                new Position(bestBox.row, bestBox.col),
                                currentState
                        );

                        illegalBoxes = new HashSet<>(bestBoxData.b.a);
                        illegalBoxes.addAll(boxToGoalData.a);

                        final Color agentColor = currentState.agent.color;
                        illegalBoxes.removeIf(box -> box.color == agentColor);
                        illegalPositions = new HashSet<>(bestBoxData.b.b);
                        illegalPositions.addAll(boxToGoalData.b);
                        illegalPositions.add(new Position(currentGoal));

                        currentState.boxList = oldBoxes;

                        // Restore walls
                        for (Position pos : agentPositions) {
                            Node.walls[pos.row][pos.col] = true;
                        }

                        System.err.println("ILLEGAL BOXES: " + illegalBoxes);

                        if (!illegalBoxes.isEmpty()) {
                            illegalByAgent = i;

                            final HashSet<Position> finalIllegalPositions = new HashSet<>(illegalPositions);
                            solvedGoals.removeIf(goal -> finalIllegalPositions.contains(new Position(goal.row, goal.col)));
                            continue;
                        }


                        List<Box> boxesToMove = null;
                        int[][] penaltyMap = null;

                        while (true) {
                            Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);

                            if (data.a.size() > 0) {
                                boxesToMove = data.a;
                                penaltyMap = data.b;
                                System.err.println("MOVE BOXES: " + boxesToMove);
                                Node leafNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, false, null);
                                if (leafNode == null) {
                                    System.err.println("UNABLE TO MOVE BOXES: " + boxesToMove);
                                    continue;
                                }
//                                System.err.println(currentState);
                                List<Command> plan = leafNode.extractPlanNew();
                                currentState = leafNode;
                                // This is a new initialState so it must not have a parent for isInitialState method to work
                                currentState.parent = null;

                                pm.mergePlan(currentState.agent.id, plan);
                            } else {
                                break;
                            }
                        }

                        System.err.println("SOLVE GOAL: " + currentGoal);

                        currentGoals.add(currentGoal);
                        Node leafNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, false, null);

                        if (leafNode == null) {
                            System.err.println("UNABLE TO SOLVE GOAL: " + currentGoal);
                            continue;
                        }

                        List<Command> plan = leafNode.extractPlanNew();
                        pm.mergePlan(currentState.agent.id, plan);

                        currentState = leafNode;
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

    private Pair<Box, Pair<List<Box>, Set<Position>>>
    bestBox(Node currentState, HashSet<Box> agentsAsBoxes, Goal goal) {
        Queue<Position> queue = new ArrayDeque<>();
        queue.add(currentState.agent);

        HashSet<Position> seen = new HashSet<>();
        ArrayList<Box> reachableBoxes = new ArrayList<>();

        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            Box maybeBox = currentState.findBox(curr.row, curr.col);
            if (maybeBox != null &&
                    Character.toLowerCase(maybeBox.letter) == goal.letter &&
                    Node.goals[maybeBox.row][maybeBox.col] != ' ') {
                reachableBoxes.add(maybeBox);
            }

            for (Position pos : curr.getNeighbours()) {
                if (Node.inBounds(pos) && !Node.walls[pos.row][pos.col] && !seen.contains(pos)) {
                    queue.add(pos);
                    seen.add(pos);
                }
            }
        }

        Position start = new Position(currentState.agent.row, currentState.agent.col);

        int bestAgents = Integer.MAX_VALUE;
        int bestBoxes = Integer.MAX_VALUE;
        Box bestBox = null;
        Pair<List<Box>, Set<Position>> bestData = null;

        for (Box box : reachableBoxes) {
            Pair<List<Box>, Set<Position>> data = BDI.boxesOnThePathToGoal(box, start, currentState);

            int blockingAgents = 0, blockingBoxes = 0;

            for (Box blocking : data.a) {
                if (agentsAsBoxes.contains(blocking)) {
                    blockingAgents++;
                } else if (blocking.color != currentState.agent.color) {
                    blockingBoxes++;
                }
            }

            if (blockingBoxes < bestBoxes || (blockingBoxes == bestBoxes && blockingAgents < bestAgents)) {
                bestBox = box;
                bestData = data;
                bestAgents = blockingAgents;
                bestBoxes = blockingBoxes;
            }

        }

        return new Pair<>(bestBox, bestData);
    }

    private Pair<HashSet<Goal>, ArrayList<Box>>
    pruneBoxList(Node currentState, List<Node> initialStates, HashSet<Goal> solvedGoals) {
        // Remove walls
        HashSet<Position> agentPositions = new HashSet<>();
        for (Node s : initialStates) {
            if (s.agent.id == currentState.agent.id) {
                continue;
            }

            agentPositions.add(new Position(s.agent.row, s.agent.col));
            Node.walls[s.agent.row][s.agent.col] = false;
        }

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

        // Restore walls
        for (Position pos : agentPositions) {
            Node.walls[pos.row][pos.col] = true;
        }

        return new Pair<>(solvableGoals, removed);
    }

    private Node
    getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap, boolean moveAgent, Set<Position> illegalPositions) {
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
                System.err.println(searchStatus(strategy));
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState(currentGoals, boxesToMove, penaltyMap) &&
                    (!moveAgent || penaltyMap[leafNode.agent.row][leafNode.agent.col] <= 0)) {
                System.err.println(searchStatus(strategy));
                return leafNode;
            }

            strategy.addToExplored(leafNode);
            for (Node n : leafNode.getExpandedNodes(illegalPositions)) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }

            iterations++;
        }
    }

    public String searchStatus(Strategy strategy) {
        StringBuilder s = new StringBuilder();
        s.append(strategy.searchStatus() + " --- " + Node.t1);
        return s.toString();
    }

}
