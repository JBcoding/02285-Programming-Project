package karlMarx;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SASearchClient extends SearchClient {

    public LinkedList<Node> Search(String strategyArg, List<Node> initialStates) throws IOException {
        if (initialStates.size() != 0) {
            throw new IllegalArgumentException("There can only be one initial state in single agent levels.");
        }
        
        strategies = new Strategy[1];
        Node initialState = initialStates.get(0);
        Strategy strategy = strategies[0];
        
        switch (strategyArg) {
        case "-astar": strategy = new StrategyBestFirst(new AStar(initialState)); break;
        case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(initialState, 5)); break;
        case "-greedy": /* Fall-through */
        default: strategy = new StrategyBestFirst(new Greedy(initialState));
        }
        
        System.err.format("Search single agent starting with strategy %s.\n", strategyArg.toString());
        
        strategy.addToFrontier(initialState);

        int iterations = 0;
        while (true) {
            if (iterations == 10000) {
                System.err.println(searchStatus());
                iterations = 0;
            }
            // TODO: Do really smart stuff with BDI and shit here
            
            if (strategy.frontierIsEmpty()) {
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState()) {
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

}
