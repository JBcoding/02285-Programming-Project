package karlMarx;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MASearchClient {

//    static boolean debug = false;

    private Strategy[] strategies;
    private String strategyArg;
    private HashMap<Goal, HashSet<Color>> solvableByColor;

    private static final int MAX_ROUNDS = 3;

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

        //System.err.format("Search multi agent starting with strategy %s.\n", strategyArg);

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
        // ArrayList<Box> lastBoxList = initialStates.get(0).boxList;

        for (Node initialState : initialStates) {
            Node.walls[initialState.agent.row][initialState.agent.col] = true;
        }

        MAPlanMerger pm = new MAPlanMerger(initialStates.get(0), initialStates.size(), initialStates);

        // TODO: MAEvilCorp shows that we actually need a stack of these or similar.
        // Note: illegalBoxes can also contain boxes with color AGENT
        Set<Position> illegalPositions = new HashSet<>();
        int illegalByAgent = -1;
        int rounds = 0;

        Agent[] agents = new Agent[initialStates.size()];

        for (int i = 0; i < initialStates.size(); i++) {
            agents[i] = initialStates.get(i).agent;
        }

        ArrayList<Box> currentBoxList = new ArrayList<>(initialStates.get(0).boxList);
        Node currentState = initialStates.get(0);

        while (true) {
            agentLoop:
            for (int i = 0; i < agents.length; i++) {
                if (illegalByAgent == i) {
                    illegalPositions.clear();
                    illegalByAgent = -1;
                }

                currentState.boxList = new ArrayList<>(currentBoxList);
                currentState.agent = agents[i];

                if (currentState.isGoalState()) {
                    return pm.getPlan();
                }

                Node.walls[currentState.agent.row][currentState.agent.col] = false;

                Pair<HashSet<Goal>, ArrayList<Box>> pruneData = pruneBoxList(currentState, agents, solvedGoals);
                HashSet<Goal> solvableGoals = pruneData.a;
                ArrayList<Box> removed = pruneData.b;

                // Prune the current goals
                Set<Goal> currentGoals = new HashSet<>();
                for (Goal goal : solvedGoals) {
                    if (solvableByColor.get(goal).contains(currentState.agent.color) &&
                            removed.stream().noneMatch(box -> box.isOn(goal))) {
                        currentGoals.add(goal);
                    }
                }

//                System.err.println();
//                System.err.println("Agent: " + currentState.agent);
//                System.err.println("STARTING FROM:");
//                System.err.println(currentState);

                Pair<HashSet<Position>, HashSet<Box>> reachableData = reachable(currentState, agents);

                HashSet<Position> clearableIllegalPositions = new HashSet<>(illegalPositions);
                clearableIllegalPositions.retainAll(reachableData.a);

                HashSet<Box> illegalBoxes = new HashSet<>();
                for (Position pos : illegalPositions) {
                    Box maybeBox = currentState.findBox(pos.row, pos.col);
                    if (maybeBox != null) {
                        illegalBoxes.add(maybeBox);
                    }
                }

                HashSet<Box> reachableBoxes = new HashSet<>(illegalBoxes);
                reachableBoxes.retainAll(reachableData.b);

                HashSet<Box> remainingIllegalBoxes = new HashSet<>(illegalBoxes);
                remainingIllegalBoxes.removeAll(reachableData.b);

                try {
                    if ((!reachableBoxes.isEmpty()
                            || clearableIllegalPositions.contains(new Position(currentState.agent)))) {
//                        System.err.println("Trying to clear path.");

//                        System.err.println(reachableBoxes);

                        ArrayList<Pair<Position, Character>> removedGoals = new ArrayList<>();

                        for (Position pos : clearableIllegalPositions) {
                            if (Node.goals[pos.row][pos.col] >= 'a' && Node.goals[pos.row][pos.col] <= 'z') {
                                removedGoals.add(new Pair<>(pos, Node.goals[pos.row][pos.col]));
                                Node.goals[pos.row][pos.col] = ' ';
                            }
                        }

                        int[][] penaltyMap = BDI.calculatePenaltyMap(currentState, clearableIllegalPositions, reachableBoxes.size());

                        for (Pair<Position, Character> posGoal : removedGoals) {
                            Node.goals[posGoal.a.row][posGoal.a.col] = posGoal.b;
                        }

                        debug = true;
                        Node lastNode = getPlan(currentState, currentGoals, new ArrayList<>(reachableBoxes), penaltyMap, true, null, clearableIllegalPositions, true);
                        debug = false;
                        if (lastNode == null) {
//                            System.err.println("Unable to clear path.");
                            continue;
                        }

                        List<Command> plan = lastNode.extractPlanNew();
                        if (plan.isEmpty()) {
                            continue;
                        }

                        currentState = lastNode;
                        // This is a new initialState so it must not have a parent for isInitialState method to work
                        currentState.parent = null;

                        Pair<Node, Position[]> mergeData = pm.mergePlan(currentState.agent.id, plan);

                        currentBoxList = new ArrayList<>(mergeData.a.boxList);

                        for (int idx = 0; idx < mergeData.b.length; idx++) {
                            Position pos = mergeData.b[idx];
                            agents[idx].row = pos.row;
                            agents[idx].col = pos.col;
                        }

                        rounds = 0;
                    } else if (!remainingIllegalBoxes.isEmpty() || illegalPositions.contains(new Position(currentState.agent))) {
                        // TODO: Take rounds into consideration
                        HashSet<Position> newIllegalPositions = new HashSet<>();

                        for (Box box : remainingIllegalBoxes) {
                            Pair<Set<Box>, Set<Position>> illegalData = illegalsToPosition(
                                    currentState, box, currentState.agent, agents, removed
                            );
                            newIllegalPositions.addAll(illegalData.b);
                        }

                        illegalPositions = newIllegalPositions;
                        illegalByAgent = i;
                    } else if (illegalByAgent == -1) {
                        Pair<Goal, Position> goalInfo = BDI.getGoal(currentState, solvableGoals).a;
                        Goal currentGoal = goalInfo.a;
                        Position endPosition = goalInfo.b;

                        if (currentGoal == null) {
//                            System.err.println("NO GOAL");
                            continue;
                        }

//                        System.err.println("NEXT GOAL: " + currentGoal);


                        // Try to find the easiest accessible box matching the goal

                        Pair<Box, Pair<List<Box>, Set<Position>>> bestBoxData =
                                bestBox(currentState, agents, removed, currentGoal);

                        Box bestBox = bestBoxData.a;
//                        System.err.println("Best box: " + bestBox);

                        // And the path from that to the current goal

                        Pair<Set<Box>, Set<Position>> boxToGoalData =
                                illegalsToPosition(currentState, currentGoal, bestBox, agents, removed);

                        illegalBoxes = new HashSet<>(bestBoxData.b.a);
                        illegalBoxes.addAll(boxToGoalData.a);

                        final Color agentColor = currentState.agent.color;
                        illegalBoxes.removeIf(box -> box.color == agentColor);
                        illegalPositions = new HashSet<>(bestBoxData.b.b);
                        illegalPositions.addAll(boxToGoalData.b);
                        illegalPositions.add(new Position(currentGoal));

//                        System.err.println("ILLEGAL BOXES: " + illegalBoxes);

                        if (!illegalBoxes.isEmpty()) {
                            illegalByAgent = i;

                            final HashSet<Position> finalIllegalPositions = new HashSet<>(illegalPositions);
                            solvedGoals.removeIf(goal -> finalIllegalPositions.contains(new Position(goal.row, goal.col)));

                            rounds = 0;
                            continue;
                        }

                        illegalPositions.clear();

                        List<Box> boxesToMove = null;
                        int[][] penaltyMap = null;

                        while (true) {
                            Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);

                            if (data.a.size() > 0) {
                                boxesToMove = data.a;
                                penaltyMap = data.b;
                                penaltyMap[currentGoal.row][currentGoal.col] = 1; // TODO: hack
//                                System.err.println("MOVE BOXES: " + boxesToMove);


                                Node leafNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, false, endPosition, null, false);
                                if (leafNode == null) {
//                                    System.err.println("UNABLE TO MOVE BOXES: " + boxesToMove);
                                    continue agentLoop;
                                }
                                List<Command> plan = leafNode.extractPlanNew();
                                currentState = leafNode;
                                // This is a new initialState so it must not have a parent for isInitialState method to work
                                currentState.parent = null;

                                Pair<Node, Position[]> mergeData = pm.mergePlan(currentState.agent.id, plan);

                                currentBoxList = new ArrayList<>(mergeData.a.boxList);
                                currentState.boxList = new ArrayList<>(currentBoxList);
                                pruneBoxList(currentState, agents, solvedGoals);

                                for (int idx = 0; idx < mergeData.b.length; idx++) {
                                    Position pos = mergeData.b[idx];
                                    agents[idx].row = pos.row;
                                    agents[idx].col = pos.col;
                                }

                                currentState.agent.row = mergeData.b[i].row;
                                currentState.agent.col = mergeData.b[i].col;
                            } else {
                                break;
                            }
                        }

//                        System.err.println("SOLVE GOAL: " + currentGoal);

                        currentGoals.add(currentGoal);

//                        System.err.println(currentState);
                        Node leafNode = getPlan(currentState, currentGoals, boxesToMove, penaltyMap, false, endPosition, null, false);

                        if (leafNode == null) {
//                            System.err.println("UNABLE TO SOLVE GOAL: " + currentGoal);
                            continue;
                        }

                        solvedGoals.add(currentGoal);

                        List<Command> plan = leafNode.extractPlanNew();

                        currentState = leafNode;
                        // This is a new initialState so it must not have a parent for isInitialState method to work
                        currentState.parent = null;

                        Pair<Node, Position[]> mergeData = pm.mergePlan(currentState.agent.id, plan);

                        currentBoxList = new ArrayList<>(mergeData.a.boxList);

                        for (int idx = 0; idx < mergeData.b.length; idx++) {
                            Position pos = mergeData.b[idx];
                            agents[idx].row = pos.row;
                            agents[idx].col = pos.col;
                        }

                        rounds = 0;
                    }
                } finally {
                    Node.walls[currentState.agent.row][currentState.agent.col] = true;

                    agents[i] = currentState.agent;

                    for (Box box : removed) {
                        Node.walls[box.row][box.col] = false;
                    }
                }
            }

            rounds += 1;

            if (rounds > MAX_ROUNDS) {
                return null;
            }
        }
    }

    private Pair<Set<Box>, Set<Position>>
    illegalsToPosition(Node currentState, Position to, Position from, Agent[] agents, List<Box> removed) {
        // Remove walls from agent positions to enable BFS
        HashSet<Position> agentPositions = new HashSet<>();
        HashSet<Box> agentsAsBoxes = new HashSet<>();
        for (Agent agent : agents) {
            if (agent.id == currentState.agent.id) {
                continue;
            }

            agentPositions.add(new Position(agent));
            // Boxes representing agents have color AGENT
            agentsAsBoxes.add(new Box(agent.row, agent.col, 'A', Color.AGENT));
            Node.walls[agent.row][agent.col] = false;
        }

        ArrayList<Box> oldBoxes = new ArrayList<>(currentState.boxList);

        currentState.boxList.addAll(agentsAsBoxes);
        currentState.boxList.addAll(removed);

        for (Box box : removed) {
            Node.walls[box.row][box.col] = false;
        }

        Pair<List<Box>, Set<Position>> hereToPosData = BDI.boxesOnThePathToGoal(
                new Position(to),
                new Position(from),
                currentState
        );

        HashSet<Box> illegalBoxes = new HashSet<>(hereToPosData.a);

        final Color agentColor = currentState.agent.color;
        illegalBoxes.removeIf(box -> box.color == agentColor);
        HashSet<Position> illegalPositions = new HashSet<>(hereToPosData.b);
        illegalPositions.add(new Position(to));

        currentState.boxList = oldBoxes;

        for (Position pos : agentPositions) {
            Node.walls[pos.row][pos.col] = true;
        }

        for (Box box : removed) {
            Node.walls[box.row][box.col] = true;
        }

        return new Pair<>(illegalBoxes, illegalPositions);
    }

    private Pair<Box, Pair<List<Box>, Set<Position>>> bestBox(Node currentState, Agent[] agents, List<Box> removed, Goal goal) {
        // Remove walls from agent positions to enable BFS
        HashSet<Position> agentPositions = new HashSet<>();
        HashSet<Box> agentsAsBoxes = new HashSet<>();
        for (Agent agent : agents) {
            if (agent.id == currentState.agent.id) {
                continue;
            }

            agentPositions.add(new Position(agent));
            // Boxes representing agents have color AGENT
            agentsAsBoxes.add(new Box(agent.row, agent.col, 'A', Color.AGENT));
            Node.walls[agent.row][agent.col] = false;
        }

        ArrayList<Box> oldBoxes = new ArrayList<>(currentState.boxList);

        currentState.boxList.addAll(agentsAsBoxes);
        currentState.boxList.addAll(removed);

        for (Box box : removed) {
            Node.walls[box.row][box.col] = false;
        }

        Queue<Position> queue = new ArrayDeque<>();
        queue.add(currentState.agent);

        HashSet<Position> seen = new HashSet<>();
        ArrayList<Box> reachableBoxes = new ArrayList<>();

        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            Box maybeBox = currentState.findBox(curr.row, curr.col);
            if (maybeBox != null &&
                    Character.toLowerCase(maybeBox.letter) == goal.letter &&
                    Node.goals[maybeBox.row][maybeBox.col] != goal.letter &&
                    maybeBox.color == currentState.agent.color) {
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

        currentState.boxList = oldBoxes;

        for (Position pos : agentPositions) {
            Node.walls[pos.row][pos.col] = true;
        }

        for (Box box : removed) {
            Node.walls[box.row][box.col] = true;
        }

        return new Pair<>(bestBox, bestData);
    }

    private Pair<HashSet<Goal>, ArrayList<Box>>
    pruneBoxList(Node currentState, Agent[] agents, HashSet<Goal> solvedGoals) {
        HashSet<Position> agentPositions = new HashSet<>();
        for (Agent agent : agents) {
            if (agent.id == currentState.agent.id) {
                continue;
            }

            agentPositions.add(new Position(agent));
            Node.walls[agent.row][agent.col] = false;
        }

        HashSet<Goal> solvableGoals = new HashSet<>();
        HashSet<Box> boxes = new HashSet<>();

        Queue<Position> queue = new ArrayDeque<>();
        queue.add(currentState.agent);

        HashSet<Position> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            Box maybeBox = currentState.findBox(curr.row, curr.col);
            if (maybeBox != null && maybeBox.color == currentState.agent.color) {
                boxes.add(maybeBox);
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
            if (!boxes.contains(box)) {
                removed.add(box);
            }
        }

        currentState.boxList = new ArrayList<>(boxes);

        for (Position pos : agentPositions) {
            Node.walls[pos.row][pos.col] = true;
        }

        for (Box box : removed) {
            Node.walls[box.row][box.col] = true;
        }

        return new Pair<>(solvableGoals, removed);
    }

    private Pair<HashSet<Position>, HashSet<Box>>
    reachable(Node currentState, Agent[] agents) {
        HashSet<Position> agentPositions = new HashSet<>();
        for (Agent agent : agents) {
            if (agent.id == currentState.agent.id) {
                continue;
            }

            agentPositions.add(new Position(agent));
            Node.walls[agent.row][agent.col] = false;
        }

        HashSet<Position> positions = new HashSet<>();
        HashSet<Box> boxes = new HashSet<>();

        Queue<Position> queue = new ArrayDeque<>();
        queue.add(currentState.agent);

        HashSet<Position> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            Position curr = queue.poll();
            positions.add(curr);

            Box maybeBox = currentState.findBox(curr.row, curr.col);
            if (maybeBox != null) {
                boxes.add(maybeBox);
            }

            for (Position pos : curr.getNeighbours()) {
                if (Node.inBounds(pos) && !Node.walls[pos.row][pos.col] && !seen.contains(pos)) {
                    Box posBox = currentState.findBox(pos.row, pos.col);
                    if (posBox == null || posBox.color == currentState.agent.color) {
                        queue.add(pos);
                        seen.add(pos);
                    }
                }
            }
        }

        for (Position pos : agentPositions) {
            Node.walls[pos.row][pos.col] = true;
        }

        return new Pair<>(positions, boxes);
    }

    private Node
    getPlan(Node state, Set<Goal> currentGoals, List<Box> boxesToMove, int[][] penaltyMap, boolean moveAgent,
            Position endPosition, Set<Position> illegalPositions, boolean clearPathForOtherAgent) {
        Strategy strategy;

        switch (strategyArg) {
            case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals, boxesToMove, penaltyMap, null, null, clearPathForOtherAgent)); break;
            case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals, boxesToMove, penaltyMap, null, null, clearPathForOtherAgent)); break;
            case "-greedy": /* Fall-through */
            default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals, boxesToMove, penaltyMap, null, null, clearPathForOtherAgent));
        }

        // System.err.println(currentGoals);
        if (illegalPositions != null) {
            //System.err.println(state.toString(illegalPositions));
        }

        strategy.addToFrontier(state);

        // TODO: Ugly problem with MAsimple5 where we don't end on the right endPosition
        // TODO: Same problem in some cases with MABeliebers
        // TODO: HACK does not always work
        if (endPosition != null && Node.walls[endPosition.row][endPosition.col]) {
            //System.err.println("Illegal endPosition: " + endPosition);
            endPosition = null;
        }

//        int iterations = 0;
        while (true) {
//            if (iterations == 1000) {
//                System.err.println(searchStatus(strategy));
//                iterations = 0;
//            }

            if (strategy.frontierIsEmpty()) {
                //System.err.println(searchStatus(strategy));
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState(currentGoals, boxesToMove, penaltyMap, endPosition, null) &&
                    (!moveAgent || penaltyMap[leafNode.agent.row][leafNode.agent.col] <= 0)) {
                //System.err.println(searchStatus(strategy));
                return leafNode;
            }

            strategy.addToExplored(leafNode);
            for (Node n : leafNode.getExpandedNodes(penaltyMap, illegalPositions, endPosition, boxesToMove)) {
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    /*
                    if (n.isGoalState(currentGoals, boxesToMove, penaltyMap, endPosition) &&
                            (!moveAgent || penaltyMap[n.agent.row][n.agent.col] <= 0)) {
                        System.err.println(strategy.searchStatus());
                        return n;
                    }
                    */
                    strategy.addToFrontier(n);
                }
            }

//            iterations++;
        }
    }

    public String searchStatus(Strategy strategy) {
        StringBuilder s = new StringBuilder();
        s.append(strategy.searchStatus() + " --- ");
        return s.toString();
    }

}
