package karlMarx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.io.*;

import karlMarx.Memory;
import karlMarx.Strategy.*;
import karlMarx.Heuristic.*;

public class SearchClient {
    ArrayList<Node> initialStates = new ArrayList<Node>();
    public SearchClient(BufferedReader serverMessages) throws Exception {
        ArrayList<Box> boxList = new ArrayList<Box>();

        HashMap<Character, Color> colors = new HashMap<>();
        String line, color;

        // Read lines specifying colors
        while ((line = serverMessages.readLine()).matches("^[a-z]+:\\s*[0-9A-Z](,\\s*[0-9A-Z])*\\s*$")) {
            line = line.replaceAll("\\s", "");
            color = line.split(":")[0];

            for (String id : line.split(":")[1].split(","))
                colors.put(id.charAt(0), Color.toColor(color));
        }

        // Part of solution.
        ArrayList<String> lines = new ArrayList<String>();
        int cols = 0;
        while (!line.equals("")) {
            if (line.length() > cols)
                cols = line.length();
            lines.add(line);
            line = serverMessages.readLine();
        }
        Node.setSize(lines.size(), cols);

        for (int row = 0; row < lines.size(); row++) {
            line = lines.get(row);
            for (int col = 0; col < line.length(); col++) {
                char chr = line.charAt(col);

                if (chr == '+') { // Wall.
                    Node.walls[row][col] = true;
                } else if ('0' <= chr && chr <= '9') { // Agent.
                    Agent agent = new Agent(row, col, chr, colors.getOrDefault(chr, Color.BLUE));
                    Node state = new Node(agent);
                    initialStates.add(state);
                } else if ('A' <= chr && chr <= 'Z') { // Box.
                    boxList.add(new Box(new Position(row, col), chr, colors.getOrDefault(chr, Color.BLUE)));
                } else if ('a' <= chr && chr <= 'z') { // Goal
                    Node.goals[row][col] = chr;
                    Goal goal = new Goal(new Position(row, col), chr);
                    Node.goalList.add(goal);
                    if (!Node.goalMap.containsKey(chr)) {
                        Node.goalMap.put(chr, new ArrayList<Goal>());
                    }
                    Node.goalMap.get(chr).add(goal);
                } else if (chr == ' ') {
                    // Free space.
                } else {
                    System.err.println("Error, read invalid level character: " + (int) chr);
                    System.exit(1);
                }
            }
        }
        for (Node state : initialStates) {
            ArrayList<Box> boxListCopy = new ArrayList<Box>();
            for (Box box : boxList) {
                boxListCopy.add(box.copy());
            }
            state.boxList = boxListCopy;
        }
    }

    public LinkedList<Node> Search(Strategy strategy, Node initialState) throws IOException {
        System.err.format("Search starting with strategy %s.\n", strategy.toString());
        strategy.addToFrontier(initialState);

        int iterations = 0;
        while (true) {
            if (iterations == 10000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

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

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Read level and create the initial state of the problem
        SearchClient client = new SearchClient(serverMessages);

        Node initialState = client.initialStates.get(0); // temp TODO
        
        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                    strategy = new StrategyBFS();
                    break;
                case "-dfs":
                    strategy = new StrategyDFS();
                    break;
                case "-astar":
                    strategy = new StrategyBestFirst(new AStar(initialState));
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                    strategy = new StrategyBestFirst(new WeightedAStar(initialState, 5));
                    break;
                case "-greedy":
                    strategy = new StrategyBestFirst(new Greedy(initialState));
                    break;
                default:
                    strategy = new StrategyBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new StrategyBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

        LinkedList<Node> solution;
        try {
            solution = client.Search(strategy, initialState); // ezpzlmnsqz
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            solution = null;
        }

        if (solution == null) {
            System.err.println(strategy.searchStatus());
            System.err.println("Unable to solve level.");
            System.exit(0);
        } else {
            System.err.println("\nSummary for " + strategy.toString());
            System.err.println("Found solution of length " + solution.size());
            System.err.println(strategy.searchStatus());

            for (Node n : solution) {
                String act = n.action.toString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n.toString());
                    break;
                }
            }
        }
    }
}
