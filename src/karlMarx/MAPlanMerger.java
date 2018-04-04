package karlMarx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class MAPlanMerger {
    private Node initState;
    private boolean[][] initWalls;
    private Position[] initPositions;
    private int numberOfAgents;
    private MasterPlan masterPlan;
    private Agent[] agents;

    public MAPlanMerger(Node initState, int numberOfAgents, List<Node> initialStates) {
        this.initState = initState.ChildNode();
        initWalls = new boolean[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            System.arraycopy(Node.walls[i], 0, initWalls[i], 0, Node.walls[i].length);
        }

        this.numberOfAgents = numberOfAgents;
        masterPlan = new MasterPlan(numberOfAgents);

        initPositions = new Position[numberOfAgents];
        agents = new Agent[numberOfAgents];
        for (int i = 0; i < numberOfAgents; i++) {
            initPositions[i] = new Position(initialStates.get(i).agent);
            initWalls[initPositions[i].row][initPositions[i].col] = false;
            agents[i] = initialStates.get(i).agent;
        }
    }

    public void mergePlan(int agent, Deque<Node> plan) {
        boolean[][] initWallsBackup = new boolean[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            System.arraycopy(Node.walls[i], 0, initWallsBackup[i], 0, Node.walls[i].length);
            System.arraycopy(initWalls[i], 0, Node.walls[i], 0, Node.walls[i].length);
        }

        Command[] arr = new Command[plan.size()];
        int i = 0;
        for (Node n : plan) {
            arr[i] = n.action;
            i += 1;
        }
        MasterPlan backup = masterPlan.copy();
        int offset = 0;
        while (true) {
            masterPlan.insertPlanAtTime(agent, masterPlan.getNextMoveTimeFromAgent(agent) + offset, arr);
            try {
                masterPlan.simulateFromNode(initState, initPositions, agents);
                break;
            } catch (conflictException e) {
                masterPlan = backup.copy();
            }
            offset ++;
        }

        for (int j = 0; j < Node.walls.length; j++) {
            System.arraycopy(initWallsBackup[j], 0, Node.walls[j], 0, Node.walls[j].length);
        }
    }

    public Command[][] getPlan() {
        return masterPlan.getPlan();
    }
}
