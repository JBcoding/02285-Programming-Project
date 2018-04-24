package karlMarx;

import java.util.*;

public class BDI {
        /*
            Agent Control Loop Version 2
            1. B := B0                 // initial beliefs
            2. while true do
            3.   get next percept rho;
            4.   B := brf(B,rho);        // brf (B, rho) is your updated beliefs based on current beliefs B and percept rho.
            5.   I := deliberate(B);   // deliberate(B) is the set of intentions chosen based on current beliefs B.
            6.   pi := plan(B,I);       // plan(B,I) is the plan chosen based on current beliefs B and current intentions I.
            7.   execute(pi)            // the procedure executing pi
            8. end while
        */
        /*
            1. load level
            2. while true do
            5.   choose box and goal
            6.   plan a way from box to goal
            7.   execute plan
            8. end while
        */
        /*
        State currentState = loadLevel();
        while (!currentState.isSolved()) {
            BoxGoalPair pair = chooseBestBoxGoalPair();
            Plan plan = getPlan(currentState, pair);
            currentState.execute(plan);
        }
        */

    public static final int[][] deltas = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    public static final Command.Dir[] deltasDirection = new Command.Dir[]{Command.Dir.S, Command.Dir.N, Command.Dir.E, Command.Dir.W};

    public static char[][] recreateMap(Node n, boolean differentiateBoxesAndGoals, boolean ignoreGoals, boolean makeCorrectlyPlacedGoalsToWalls) {
//        System.err.println("Node in recreateMap: " + n);
        char[][] map = new char[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                map[i][j] = (Node.walls[i][j] ? '+' : ' ');
            }
        }
        for (Goal g : Node.goalSet) {
            map[g.row][g.col] = g.letter;
        }
//        for (int i = 0; i < map.length; i++) {
//            System.err.println(Arrays.toString(map[i]));
//        }
        for (Box b : n.boxList) {
//            System.err.println(b);
//            System.err.println(map[b.row][b.col]);
//            System.err.println(Character.toLowerCase(b.letter) == map[b.row][b.col]);
//            System.err.println(makeCorrectlyPlacedGoalsToWalls);
            if (Character.toLowerCase(b.letter) == map[b.row][b.col] && makeCorrectlyPlacedGoalsToWalls) { // goal already fulfilled
//                System.err.println("Changing to +");
                map[b.row][b.col] = '+';
            } else {
                map[b.row][b.col] = 'o';
                if (differentiateBoxesAndGoals) {
                    map[b.row][b.col] = b.letter;
                }
            }
        }
        if (!differentiateBoxesAndGoals) {
            for (Goal g : Node.goalSet) {
                if (map[g.row][g.col] == g.letter) {
                    map[g.row][g.col] = 'o';
                }
            }
        }
        if (ignoreGoals) {
            for (Goal g : Node.goalSet) {
                if (map[g.row][g.col] != '+' && !Character.isUpperCase(map[g.row][g.col])) {
                    map[g.row][g.col] = ' ';
                }
            }
        }
        return map;
    }

    public static Pair<List<Box>, int[][]> boxToMove(Node n, Goal g) {
        List<Box> boxes = getBoxesToGoal(g, n);
        Box box;

        if (boxes.size() == 1) {
            box = boxes.get(0);
        } else {
            Box closestBox = null;
            int distance = Integer.MAX_VALUE;

            for (Box b : boxes) {
                if (Character.toLowerCase(b.letter) != Node.goals[b.row][b.col]) {
                    int dist = Heuristic.shortestDistance[b.row][b.col][g.row][g.col];
                    if (dist > -1 && dist < distance) {
                        closestBox = b;
                        distance = dist;
                    }
                }
            }

            if (closestBox == null) {
//                for (int i = 0; i < tempMap.length; i++) {
//                    System.err.println(Arrays.toString(tempMap[i]));
//                }
//                System.err.println(tempNode);
//                System.err.println(g);
//                System.err.println(boxes);
//                System.err.println(n);
//                throw new IllegalStateException("TODO: No closest box.");
                return null;
            }

            box = closestBox;
        }

        Pair<List<Box>, Set<Position>> data = boxesOnThePathToGoal(g, box, n);
        List<Box> boxesToMove = data.a;
        Set<Position> IllegalPositions = data.b;
        int[][] penaltyMap = calculatePenaltyMap(n, IllegalPositions, boxesToMove.size());
        return new Pair<>(boxesToMove, penaltyMap);
    }

    private static char[][] getWallsWithExtra(Node n) {
        char[][] map = new char[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                map[i][j] = (Node.walls[i][j] ? '+' : ' ');
                for (Box b : n.boxList) {
                    if (b.row == i && b.col == j && Character.toLowerCase(b.letter) == Node.goals[i][j]) {
                        map[i][j] = '+';
                    }
                }
            }
        }
        // all completed goals are now walls
        // now make all unreachable fields walls
        Queue<Position> queue = new ArrayDeque<>();
        queue.add(new Position(n.agent.row, n.agent.col));
        Set<Position> boxes = new HashSet<>();
        for (Box b : n.boxList) {
            boxes.add(new Position(b.row, b.col));
        }
        return null;
    }

    public static int[][] calculatePenaltyMap(Node n, Set<Position> illegalPositions, int numberOfBoxesToMove) {
        char[][] map = new char[Node.walls.length][Node.walls[0].length];
        int[][] penaltyMap = new int[Node.walls.length][Node.walls[0].length];
        Queue<Position> queue = new ArrayDeque<>();
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                map[i][j] = (Node.walls[i][j] ? '+' : ' ');
                for (Box b : n.boxList) {
                    if (b.row == i && b.col == j && Character.toLowerCase(b.letter) == Node.goals[i][j]) {
                        map[i][j] = '+';
                    }
                }
                if (map[i][j] == ' ' && !illegalPositions.contains(new Position(i, j))) {
                    if (i == 0 || j == 0 || i == Node.walls.length - 1 || j == Node.walls[i].length - 1) {
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        int dr = deltas[k][0]; // delta row
                        int dc = deltas[k][1]; // delta col
                        if (illegalPositions.contains(new Position(i + dr, j + dc))) {
                            queue.add(new Position(i, j));
                            map[i][j] = '0';
                            penaltyMap[i][j] = 0;
                            break;
                        }
                    }
                }
            }
        }
        Position nullPos = new Position(-1, -1);
        queue.add(nullPos);
        int penalty = 1;
        while (!queue.isEmpty()) {
            Position pos = queue.poll();
            if (pos == nullPos && queue.isEmpty()) {
                break;
            }
            if (pos == nullPos) {
                penalty ++;
                queue.add(nullPos);
                continue;
            }
            for (int j = 0; j < 4; j++) {
                int dr = deltas[j][0]; // delta row
                int dc = deltas[j][1]; // delta col
                if (map[pos.row + dr][pos.col + dc] == ' ') {
                    penaltyMap[pos.row + dr][pos.col + dc] = penalty;
                    map[pos.row + dr][pos.col + dc] = '0';
                    queue.add(new Position(pos.row + dr, pos.col + dc));
                }
            }
        }
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                if (!illegalPositions.contains(new Position(i, j))) {
                    penaltyMap[i][j] *= -1;
                    if (penaltyMap[i][j] < -numberOfBoxesToMove) {
                        penaltyMap[i][j] = 0;
                    }
                    //penaltyMap[i][j] = Math.max(-numberOfBoxesToMove, penaltyMap[i][j]);
                }
            }
        }

        boolean[][] updatedPositions = new boolean[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                if (!updatedPositions[i][j] && penaltyMap[i][j] < 0) {
                    queue.clear();
                    queue.add(new Position(i, j));
                    List<Integer> numbersInBlob = new ArrayList<>();
                    List<Position> positionList = new ArrayList<>();
                    positionList.add(new Position(i, j));
                    while (!queue.isEmpty()) {
                        Position p = queue.poll();
                        updatedPositions[p.row][p.col] = true;
                        positionList.add(new Position(p.row, p.col));
                        numbersInBlob.add(penaltyMap[p.row][p.col]);
                        for (int k = 0; k < 4; k++) {
                            int dr = deltas[k][0]; // delta row
                            int dc = deltas[k][1]; // delta col
                            if (penaltyMap[p.row + dr][p.col + dc] < 0 && !updatedPositions[p.row + dr][p.col + dc]) {
                                queue.add(new Position(p.row + dr, p.col + dc));
                                updatedPositions[p.row + dr][p.col + dc] = true;
                            }
                        }
                    }
                    if (numbersInBlob.size() > numberOfBoxesToMove) {
                        Collections.sort(numbersInBlob, Collections.reverseOrder());
                        int cutOffValue = numbersInBlob.get(numberOfBoxesToMove);
                        for (int k = 0; k < positionList.size(); k++) {
                            if (penaltyMap[positionList.get(k).row][positionList.get(k).col] < cutOffValue) {
                                penaltyMap[positionList.get(k).row][positionList.get(k).col] = 0;
                            }
                        }
                    }
                }
            }
        }

        return penaltyMap;
    }

    public static Pair<List<Box>, Set<Position>> boxesOnThePathToGoal(Position g, Position start, Node n) {
        char[][] map = recreateMap(n, true, true, false);

        Queue<Position>[] queues = new Queue[n.boxList.size() + 1];
        queues[0] = new ArrayDeque<>();
        queues[0].add(new Position(g));
        for (int i = 0; i < n.boxList.size(); i++) {
            if (queues[i] == null) {
                break;
            }
            while (!queues[i].isEmpty()) {
                Position pos = queues[i].poll();
                for (int j = 0; j < 4; j++) {
                    int dr = deltas[j][0]; // delta row
                    int dc = deltas[j][1]; // delta col
                    int queueIndex = i;
                    if (Character.isAlphabetic(map[pos.row + dr][pos.col + dc])) {
                        queueIndex ++;
                        map[pos.row + dr][pos.col + dc] = ' ';
                    }
                    if (map[pos.row + dr][pos.col + dc] == ' ') {
                        if (queues[queueIndex] == null) {
                            queues[queueIndex] = new ArrayDeque<>();
                        }
                        map[pos.row + dr][pos.col + dc] = (char) (48 + (dr + 1) + 3 * (dc + 1));
                        queues[queueIndex].add(new Position(pos.row + dr, pos.col + dc));
                    }
                }
            }
        }
        // backtrack route
        Position p = new Position(start);
        List<Box> boxesOnThePath = new ArrayList<>();
        char[][] originalMap = recreateMap(n, true, true, false);
        Set<Position> IllegalPositions = new HashSet<>();
        IllegalPositions.add(new Position(p));
        while (!p.equals(g)) {
            int direction = map[p.row][p.col];
            int dr = ((direction - 48) % 3) - 1;
            int dc = ((direction - 48) / 3) - 1;
            p.row -= dr;
            p.col -= dc;
            if (Character.isAlphabetic(originalMap[p.row][p.col])) {
                for (Box box : n.boxList) {
                    if (p.equals(box)) {
                        boxesOnThePath.add(box);
                    }
                }
            }
            IllegalPositions.add(new Position(p));
        }

        return new Pair<>(boxesOnThePath, IllegalPositions);
    }

    public static List<Box> getBoxesToGoal(Goal g, Node n) {
        List<Box> boxes = new ArrayList<>();
        for (Box b : n.boxList) {
            if (Character.toLowerCase(b.letter) == g.letter) {
                boxes.add(b);
            }
        }
        return boxes;
    }

    public static Goal getGoal(Node n) {
        return getGoal(n, Node.goalSet);
    }

    public static char[][] tempMap;
    public static Node tempNode;

    public static Goal getGoal(Node n, Set<Goal> goalSet) {
        char[][] map = recreateMap(n, false, false, true);
        tempMap = map;
        tempNode = n.ChildNode();
        char[][] originalMap = new char[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            System.arraycopy(map[i], 0, originalMap[i], 0, map[i].length);
        }
        Goal bestGoal = null;
        double bestGoalScore = 0;
        for (Goal g : goalSet) {
            // Run BFS from g
            if (originalMap[g.row][g.col] == '+') {
                continue;
            }
            map = new char[originalMap.length][originalMap[0].length];
            for (int i = 0; i < map.length; i++) {
                System.arraycopy(originalMap[i], 0, map[i], 0, originalMap[i].length);
            }
            map[g.row][g.col] = 'X';
            int[] sidesCount = new int[4];
            Position[] sidesPositions = new Position[] {
                    new Position(g.row + 1, g.col),
                    new Position(g.row, g.col + 1),
                    new Position(g.row - 1, g.col),
                    new Position(g.row, g.col - 1)};
            for (int i = 0; i < 4; i++) {
                Queue<Position> queue = new ArrayDeque<>();
                if (map[sidesPositions[i].row][sidesPositions[i].col] == 'o') {
                    map[sidesPositions[i].row][sidesPositions[i].col] = ' ';
                    sidesCount[i] ++;
                }
                if (map[sidesPositions[i].row][sidesPositions[i].col] == ' ') {
                    queue.add(sidesPositions[i]);
                }
                while (!queue.isEmpty()) {
                    Position pos = queue.poll();
                    for (int j = 0; j < 4; j++) {
                        int dr = deltas[j][0]; // delta row
                        int dc = deltas[j][1]; // delta col
                        if (map[pos.row + dr][pos.col + dc] == 'o') {
                            sidesCount[i] ++;
                            map[pos.row + dr][pos.col + dc] = ' ';
                        }
                        if (map[pos.row + dr][pos.col + dc] == ' ') {
                            queue.add(new Position(pos.row + dr, pos.col + dc));
                            map[pos.row + dr][pos.col + dc] = '0';
                        }
                    }
                }
            }
            int largestSide = sidesCount[0], secondLargestSide = 0;
            for (int i = 1; i < 4; i++) {
                if (sidesCount[i] > largestSide) {
                    secondLargestSide = largestSide;
                    largestSide = sidesCount[i];
                } else if (sidesCount[i] > secondLargestSide) {
                    secondLargestSide = sidesCount[i];
                }
            }
            double goalScore = largestSide - secondLargestSide;
            for (int i = 0; i < 4; i++) {
                goalScore += (map[g.row + deltas[i][0]][g.col + deltas[i][1]] == '+') ? .2 : 0;
            }
            goalScore += .2 - ((double)(Heuristic.shortestDistance[g.row][g.col][n.agent.row][n.agent.col]) / 5000.);
            if (goalScore > bestGoalScore) {
                bestGoalScore = goalScore;
                bestGoal = g;
            }
        }
        
        return bestGoal;
    }

    public static void removeUnreachableBoxesFromBoxlist(Node currentState) {
        return;
//        Agent agent = currentState.agent;
//        List<Box> list = currentState.boxList;
//        for (int i = list.size(); i >= 0; i--) {
//            Box box = list.get(i);
//            if (Heuristic.shortestDistance[box.row][box.col][agent.row][agent.col] < 0) {
//                list.remove(i);
//            }
//        }
    }
}
