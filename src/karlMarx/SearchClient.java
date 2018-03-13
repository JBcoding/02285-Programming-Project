package karlMarx;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class SearchClient {

    private Strategy[] strategies;

    public LinkedList<Node> Search(String strategyArg, List<Node> initialStates) throws IOException {
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
            break;
        }
        
        System.err.format("Search starting with strategy %s.\n", strategyArg.toString());
        
        for (int i = 0; i < strategies.length; i++) {            
            strategies[i].addToFrontier(initialStates.get(i));
        }

        int iterations = 0;
        while (true) {
            if (iterations == 10000) {
                System.err.println(searchStatus());
                iterations = 0;
            }
            for (Strategy strategy : strategies) {
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
            }
            iterations++;
        }
    }

    public String searchStatus() {
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
