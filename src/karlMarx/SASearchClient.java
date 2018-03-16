package karlMarx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class SASearchClient extends SearchClient {

    private String strategyArg;
    private Strategy strategy;

    public List<Node> Search(String strategyArg, List<Node> initialStates) throws IOException {
        if (initialStates.size() != 1) {
            throw new IllegalArgumentException("There can only be one initial state in single agent levels.");
        }
        
        this.strategyArg = strategyArg;
        
        System.err.format("Search single agent starting with strategy %s.\n", strategyArg.toString());
        
        Node currentState = initialStates.get(0);
        Goal currentGoal;
        List<Goal> currentGoals = new ArrayList<Goal>();
        
        List<Node> solution = new LinkedList<Node>();
        while (!currentState.isGoalState()) {
            currentGoal = BDI.getGoal(currentState);
            System.err.println("####" + currentGoal);
            currentGoals.add(currentGoal);
            Deque<Node> plan = getPlan(currentState, currentGoals);
            solution.addAll(plan);
            currentState = plan.getLast();
            // This is a new initialState so it must not have a parent for isInitialState method to work
            currentState.parent = null;
        }
        
        return solution;
    }

    private Deque<Node> getPlan(Node state, List<Goal> currentGoals) {
        switch (strategyArg) {
        case "-astar": strategy = new StrategyBestFirst(new AStar(state, currentGoals)); break;
        case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(state, 5, currentGoals)); break;
        case "-greedy": /* Fall-through */
        default: strategy = new StrategyBestFirst(new Greedy(state, currentGoals));
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

            if (leafNode.isGoalState(currentGoals)) {
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
