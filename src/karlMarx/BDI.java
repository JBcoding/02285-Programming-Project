package karlMarx;

import java.util.*;

public class BDI {
    public static void main(String[] args) {
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
        /*

         */
        Node n = new Node(new Agent(3, 6, 0, Color.BLUE));
        Node.walls = new boolean[][]{{false, true, true, true, true, true, false, true, true, true, true, true, true, false, false},
                {false, true, false, false, false, true, true, true, false, false, false, false, true, false, false},
                {false, true, true, false, false, false, false, false, true, false, false, true, false, true, false},
                {false, true, false, false, false, false, false, false, false, false, false, false, false, true, true},
                {false, true, false, true, false, false, true, true, false, true, false, false, false, false, true},
                {false, true, false, false, true, true, false, false, false, true, false, true, true, false, true},
                {false, true, true, false, false, false, false, false, false, false, false, false, false, false, true},
                {false, true, false, false, false, false, true, false, true, false, false, false, false, true, false},
                {false, true, false, false, false, true, false, false, false, true, false, false, true, true, false},
                {false, true, true, true, true, true, false, false, false, false, false, false, true, false, false},
                {false, false, false, false, false, true, true, true, true, false, false, true, true, false, false},
                {false, false, false, false, false, false, false, false, true, true, true, true, false, false, false}};
        Node.goalSet = new HashSet<>();
        Node.goalSet.add(new Goal(7, 9, 's'));
        Node.goalSet.add(new Goal(7, 12, 's'));
        Node.goalSet.add(new Goal(6, 12, 's'));
        Node.goalSet.add(new Goal(4, 11, 's'));
        Node.goalSet.add(new Goal(6, 13, 's'));
        Node.goalSet.add(new Goal(4, 12, 's'));
        Node.goalSet.add(new Goal(5, 13, 's'));
        Node.goalSet.add(new Goal(4, 13, 's'));
        n.boxList = new ArrayList<>();
        n.boxList.add(new Box(2, 4, 'S', Color.BLUE));
        n.boxList.add(new Box(2, 6, 'S', Color.BLUE));
        n.boxList.add(new Box(2, 9, 'S', Color.BLUE));
        n.boxList.add(new Box(2, 12, 'S', Color.BLUE));
        n.boxList.add(new Box(3, 4, 'S', Color.BLUE));
        n.boxList.add(new Box(3, 8, 'S', Color.BLUE));
        n.boxList.add(new Box(3, 11, 'S', Color.BLUE));
        n.boxList.add(new Box(5, 7, 'S', Color.BLUE));
        n.boxList.add(new Box(6, 5, 'S', Color.BLUE));
        n.boxList.add(new Box(7, 3, 'S', Color.BLUE));
        n.boxList.add(new Box(7, 4, 'S', Color.BLUE));
        n.boxList.add(new Box(7, 7, 'S', Color.BLUE));
        n.boxList.add(new Box(8, 10, 'S', Color.BLUE));
        n.boxList.add(new Box(9, 7, 'S', Color.BLUE));


        //n.boxList.add(new Box(7, 9, 'S', Color.BLUE));
        //n.boxList.add(new Box(7, 12, 'S', Color.BLUE));
        //n.boxList.add(new Box(6, 12, 'S', Color.BLUE));
        //n.boxList.add(new Box(4, 11, 'S', Color.BLUE));
        //n.boxList.add(new Box(6, 13, 'S', Color.BLUE));
        //n.boxList.add(new Box(4, 12, 'S', Color.BLUE));
        //n.boxList.add(new Box(5, 13, 'S', Color.BLUE));
        //n.boxList.add(new Box(4, 13, 'S', Color.BLUE));
        System.out.println(getGoal(n));
    }

    public static char[][] recreateMap(Node n) {
        char[][] map = new char[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            for (int j = 0; j < Node.walls[i].length; j++) {
                map[i][j] = (Node.walls[i][j] ? '+' : ' ');
            }
        }
        for (Goal g : Node.goalSet) {
            map[g.row][g.col] = g.letter;
        }
        for (Box b : n.boxList) {
            if (Character.toLowerCase(b.letter) == map[b.row][b.col]) { // goal already fulfilled
                map[b.row][b.col] = '+';
            } else {
                map[b.row][b.col] = 'o';
            }
        }
        for (Goal g : Node.goalSet) {
            if (map[g.row][g.col] == g.letter) {
                map[g.row][g.col] = 'o';
            }
        }
        return map;
    }

    public static int boxToMove(Node n) {
        char[][] map = recreateMap(n);

        return -1;
    }

    public static Goal getGoal(Node n) {
        char[][] map = recreateMap(n);
        char[][] originalMap = new char[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            System.arraycopy(map[i], 0, originalMap[i], 0, map[i].length);
        }
        int[][] deltas = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        Goal bestGoal = null;
        double bestGoalScore = 0;
        for (Goal g : Node.goalSet) {
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
                    map[pos.row][pos.col] = '0';
                    for (int j = 0; j < 4; j++) {
                        int dr = deltas[j][0]; // delta row
                        int dc = deltas[j][1]; // delta col
                        if (map[pos.row + dr][pos.col + dc] == 'o') {
                            sidesCount[i] ++;
                            map[pos.row + dr][pos.col + dc] = ' ';
                        }
                        if (map[pos.row + dr][pos.col + dc] == ' ') {
                            queue.add(new Position(pos.row + dr, pos.col + dc));
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
            if (goalScore > bestGoalScore) {
                bestGoalScore = goalScore;
                bestGoal = g;
            }
        }
        
        return bestGoal;
    }
}
