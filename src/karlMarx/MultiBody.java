package karlMarx;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MultiBody {
    Agent[] agents;
    MultiBodyNode mbn;
    MultiBody(Node n) {
        mbn = new MultiBodyNode(n);
    }

    public Command[][] Search(String strategyArg, List<Node> initialStates) throws IOException {

        agents = new Agent[initialStates.size()];
        for (int i = 0; i < initialStates.size(); i ++) {
            agents[i] = initialStates.get(i).agent;
        }
        mbn.setAgents(agents);

        Strategy strategy;

        switch (strategyArg) {
            case "-astar": strategy = new StrategyBestFirst(new AStar(initialStates.get(0), initialStates.get(0).getGoalSet(), null, null, null, null)); break;
            case "-wastar": strategy = new StrategyBestFirst(new WeightedAStar(initialStates.get(0), 5, initialStates.get(0).getGoalSet(), null, null, null, null)); break;
            case "-greedy": /* Fall-through */
            default: strategy = new StrategyBestFirst(new Greedy(initialStates.get(0), initialStates.get(0).getGoalSet(), null, null, null, null));

        }

        strategy.addToFrontier(mbn);

        int iterations = 0;
        while (true) {
            if (iterations == 10000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            MultiBodyNode leafNode = (MultiBodyNode)strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState()) {
                LinkedList<Pair<Command, Integer>> plan = leafNode.extractPlan();
                Command[][] finalPlan = new Command[plan.size()][agents.length];
                for (int i = 0; i < plan.size(); i++) {
                    for (int j = 0; j < agents.length; j++) {
                        finalPlan[i][j] = new Command();
                        if (j == plan.get(i).b) {
                            finalPlan[i][j] = plan.get(i).a;
                        }
                    }
                }
                System.err.println(leafNode);
                System.err.println(strategy.searchStatus());
                return finalPlan;
            }
            strategy.addToExplored(leafNode);

            for (MultiBodyNode n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }

            iterations++;
        }
    }
}