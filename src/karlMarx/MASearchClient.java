package karlMarx;

import java.io.IOException;
import java.util.*;

public class MASearchClient {

    private Strategy[] strategies;
    private String strategyArg;

    public ArrayList<List<Node>> Search(String strategyArg, List<Node> initialStates) throws IOException {
        Node.IS_SINGLE = false;
        this.strategyArg = strategyArg;

        strategies = new Strategy[initialStates.size()];
        Collections.sort(initialStates, new Comparator<Node>() {
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

        ArrayList<List<Node>> solutions = new ArrayList<>();

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

        // TODO: hack part 0
        for (Node initialState : initialStates) {
            Node.walls[initialState.agent.row][initialState.agent.col] = true;
        }

        for (Node currentState : initialStates) {
            System.err.println("Agent color: " + currentState.agent.color);

            // TODO: hack part 1
            Node.walls[currentState.agent.row][currentState.agent.col] = false;

            currentState.boxList = lastBoxList;

            Goal currentGoal;
            Set<Goal> currentGoals = new HashSet<Goal>();

            HashSet<Goal> solvableGoals = new HashSet<>();

            // TODO: make generic BFS
            Queue<Position> queue = new ArrayDeque<>();

            queue.add(currentState.agent);

            HashSet<Position> seen = new HashSet<>();

            ArrayList<Box> boxList = new ArrayList<>();

            System.err.println("BEFORE LOOP");
            while (!queue.isEmpty()) {
                Position curr = queue.poll();

                Box maybeBox = currentState.findBox(curr.row, curr.col);
                if (maybeBox != null) {
                    boxList.add(maybeBox);
                }

                if (Node.goals[curr.row][curr.col] >= 'a' && Node.goals[curr.row][curr.col] <= 'z') {
                    Goal goal = Node.findGoal(curr.row, curr.col);

                    if (solvableByColor.get(goal).contains(currentState.agent.color) && !solvedGoals.contains(goal)) {
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

            System.err.println("BOX LIST: " + boxList);

            List<Node> solution = new LinkedList<Node>();
            while (!currentState.isGoalState(solvableGoals, null, null)) {
                currentGoal = BDI.getGoal(currentState, solvableGoals);
                Pair<List<Box>, int[][]> data = BDI.boxToMove(currentState, currentGoal);
                System.err.println("NEXT GOAL: " + currentGoal);
                if (data != null && data.a.size() > 0) {
                    List<Box> boxesToMove = data.a;
                    int[][] penaltyMap = data.b;
                    System.err.println("MOVE BOXES: " + boxesToMove);
                    Deque<Node> plan = getPlan(currentState, currentGoals, boxesToMove, penaltyMap);
                    if (plan == null) {
                        System.err.println("UNABLE TO MOVE BOXES: " + boxesToMove);
                        return null;
                    }
                    solution.addAll(plan);
                    currentState = plan.getLast();
                    // This is a new initialState so it must not have a parent for isInitialState method to work
                    currentState.parent = null;
                }

                System.err.println("SOLVE GOAL: " + currentGoal);
                currentGoals.add(currentGoal);
                Deque<Node> plan = getPlan(currentState, currentGoals, null, null);

                if (plan == null) {
                    System.err.println("UNABLE TO SOLVE GOAL: " + currentGoal);
                    return null;
                }
                solution.addAll(plan);

                currentState = plan.getLast();
                // This is a new initialState so it must not have a parent for isInitialState method to work
                currentState.parent = null;

                solvedGoals.add(currentGoal);
            }

            // TODO: hack part 2
            Node.walls[currentState.agent.row][currentState.agent.col] = true;

            solutions.add(solution);

            lastBoxList = currentState.boxList;
            lastBoxList.addAll(removed);
        }


        return solutions;
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
