package karlMarx;

import karlMarx.Map.LevelInfo;

import java.io.BufferedReader;
import java.util.*;

public class LevelReader {



    public static ArrayList<Node> readLevel(BufferedReader serverMessages) throws Exception {
        ArrayList<Node> initialStates = new ArrayList<>();
        ArrayList<Box> boxList = new ArrayList<>();
        List<Agent> agentList = new LinkedList<>();

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
                    Agent agent = new Agent(row, col, chr - '0', colors.getOrDefault(chr, Color.BLUE));
                    agentList.add(agent);
                    Node state = new Node(agent);
                    initialStates.add(state);
                } else if ('A' <= chr && chr <= 'Z') { // Box.
                    boxList.add(new Box(new Position(row, col), chr, colors.getOrDefault(chr, Color.BLUE)));
                } else if ('a' <= chr && chr <= 'z') { // Goal
                    Node.goals[row][col] = chr;
                    LevelInfo.addGoal(row, col, chr);
                    Goal goal = new Goal(new Position(row, col), chr);
                    Node.goalSet.add(goal);
                    if (!Node.goalMap.containsKey(chr)) {
                        Node.goalMap.put(chr, new ArrayList<Goal>());
                    }
                    Node.goalMap.get(chr).add(goal);
                } else if (chr == ' ') {
                    // Free space.
                } else {
                    System.err.println("Error, read invalid LevelInfo character: " + (int) chr);
                    System.exit(1);
                }
            }
        }

//        boxList.forEach(box -> level.getBoxColors().put(box.id,box.color));
//        agentList.forEach(agent -> level.getColorToAgents().get(agent.color).add(agent));

        boxList.forEach(box -> LevelInfo.getBoxIdToColors().put(box.id,box.color));
        agentList.forEach(agent -> {
            LevelInfo.getColorToAgents().putIfAbsent(agent.color, new HashSet<>());
            LevelInfo.getColorToAgents().get(agent.color).add(agent);
        });

        for (Node state : initialStates) {
            ArrayList<Box> boxListCopy = new ArrayList<Box>();
            for (Box box : boxList) {
                boxListCopy.add(box.copy());
            }
            state.boxList = boxListCopy;
            state.boxListSorted = new ArrayList<>();
            state.boxListSorted.addAll(boxListCopy);
            Collections.sort(state.boxListSorted);
            state.boxListSortedHashCode = state.boxListSorted.hashCode();
        }
        
        return initialStates;
    }
}
