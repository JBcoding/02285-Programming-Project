package karlMarx;

import java.util.List;

public class MAPlanMerger {
    private Node initState;
    private boolean[][] initWalls;
    private Position[] initPositions;
    private int numberOfAgents;
    private MasterPlan masterPlan;
    private Agent[] agents;

    public MAPlanMerger(Node initState, int numberOfAgents, List<Node> initialStates) {
//        System.err.println("INITIAL STATE:\n" + initState);
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

    public Pair<Node, Position[]> mergePlan(int agent, List<Command> plan) {
        boolean[][] initWallsBackup = new boolean[Node.walls.length][Node.walls[0].length];
        for (int i = 0; i < Node.walls.length; i++) {
            System.arraycopy(Node.walls[i], 0, initWallsBackup[i], 0, Node.walls[i].length);
            System.arraycopy(initWalls[i], 0, Node.walls[i], 0, Node.walls[i].length);
        }

        Command[] arr = new Command[plan.size()];
        int i = 0;
        for (Command n : plan) {
            arr[i] = n;
            i += 1;
        }
        MasterPlan backup = masterPlan.copy();
        int offset = 0;

        Pair<Node, Position[]> res;

        while (true) {
            masterPlan.insertPlanAtTime(agent, masterPlan.getNextMoveTimeFromAgent(agent) + offset, arr);
            try {
                res = masterPlan.simulateFromNode(initState, initPositions, agents);
                break;
            } catch (conflictException e) {
                masterPlan = backup.copy();
                int workablePlanLengthMax = e.getConflictRound() - masterPlan.getNextMoveTimeFromAgent(agent) - offset;
                int workablePlanLengthMin = 1;
                if (workablePlanLengthMax > 0 && workablePlanLengthMax < arr.length) {
                    // Bin search here
                    boolean success = true;
                    int thisOffset = masterPlan.getNextMoveTimeFromAgent(agent) + offset;

                    Command[] restPlan = tryPlanOfLength(backup, arr, agent, workablePlanLengthMax, thisOffset, true);

                    if (restPlan != null) {
                        workablePlanLengthMin = workablePlanLengthMax;
                    } else {
                        restPlan = tryPlanOfLength(backup, arr, agent, 1, thisOffset, true);
                        if (restPlan == null) {
                            success = false;
                        }
                    }


                    while (success && workablePlanLengthMin < workablePlanLengthMax) {
                        int length = (int) Math.ceil((workablePlanLengthMax + workablePlanLengthMin) / 2.0);
                        restPlan = tryPlanOfLength(backup, arr, agent, length, thisOffset, true);
                        if (restPlan != null) {
                            workablePlanLengthMin = length;
                        } else {
                            workablePlanLengthMax = length - 1;
                        }
                    }

                    if (success) {
                        offset = 0;
                        restPlan = tryPlanOfLength(backup, arr, agent, workablePlanLengthMax, thisOffset, false);
                        arr = restPlan;                        
                        backup = masterPlan.copy();
                    }
                }
            }
            offset ++;
        }

        for (int j = 0; j < Node.walls.length; j++) {
            System.arraycopy(initWallsBackup[j], 0, Node.walls[j], 0, Node.walls[j].length);
        }

        return res;
    }

    public Command[] tryPlanOfLength(MasterPlan backup, Command[] plan, int agent, int length, int offset, boolean doRestore) {
        Command[] subPlan = new Command[length];
        Command[] restPlan = new Command[plan.length - subPlan.length];
        System.arraycopy(plan, 0, subPlan, 0, length);
        System.arraycopy(plan, length, restPlan, 0, restPlan.length);
        masterPlan.insertPlanAtTime(agent, offset, subPlan);
        try {
            masterPlan.simulateFromNode(initState, initPositions, agents);
        } catch (conflictException e1) {
            return null;
        } finally {
            if (doRestore) {
                masterPlan = backup.copy();
            }
        }
        return restPlan;
    }

    public Command[][] getPlan() {
        masterPlan.removeRepetitiveStates(initState.ChildNode(), initPositions, agents, initWalls);            
        return masterPlan.getPlan();            
    }
}
