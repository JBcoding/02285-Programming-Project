package karlMarx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasterPlan {
    private static final int INITIAL_LENGTH = 010; // 0x08 // 0b00001000
    private Command[][] plan;
    private int length;
    private int numberOfAgents;
    private int[] lastMoveStep;

    public MasterPlan(int numberOfAgents) {
        length = 0;
        plan = new Command[INITIAL_LENGTH][numberOfAgents];
        this.numberOfAgents = numberOfAgents;
        lastMoveStep = new int[numberOfAgents];
        for (int i = 0; i < numberOfAgents; i++) {
            lastMoveStep[i] = -1;
        }
    }

    private void doublePlanLength() {
        Command[][] newPlan = new Command[plan.length * 2][plan[0].length];
        System.arraycopy(plan, 0, newPlan, 0, plan.length);
        plan = newPlan;
    }

    public void addStepToPlan(int agent, Command cmd) {
        Command[] arr = new Command[numberOfAgents];
        arr[agent] = cmd;
        addStepToPlan(arr);
    }

    public void addStepToPlan(Command[] arr) {
        length ++;
        if (length >= plan.length) {
            doublePlanLength();
        }
        System.arraycopy(arr, 0, plan[length - 1], 0, numberOfAgents);

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i].actionType != Command.Type.NoOp) {
                lastMoveStep[i] = Math.max(lastMoveStep[i], length - 1);
            }
        }
    }

    public Pair<Node, Position[]> simulateFromNode(Node n, Position[] initPositions, Agent[] agents) throws conflictException {
        return simulateFromNode(n, initPositions, agents, -1);
    }

    public Pair<Node, Position[]> simulateFromNode(Node n, Position[] initPositions, Agent[] agents, int stepsBack) throws conflictException {
        Position[] positions = new Position[numberOfAgents];
        List<Position> oldPositions = new ArrayList<>();
        for (int i = 0; i < numberOfAgents; i++) {
            positions[i] = new Position(initPositions[i]);
        }
        List<Pair<Integer, Position>> tempBoxPositions = new ArrayList<>();
        int stepsTaken = 0;
        while (stepsBack != 0 && stepsTaken < length) {
            stepsBack --;
            for (int i = 0; i < numberOfAgents; i++) {
                n.agent = new Agent(positions[i].row, positions[i].col, agents[i].id, agents[i].color);
                // simulate command plan[stepsTaken][i]
                Command c = plan[stepsTaken][i];
                if (c == null || c.actionType == Command.Type.NoOp) {
                    continue;
                }
                for (int j = 0; j < numberOfAgents; j++) {
                    if (j != i) {
                        Node.walls[positions[j].row][positions[j].col] = true;
                    }
                }
                for (Position p : oldPositions) {
                    Node.walls[p.row][p.col] = true;
                }
                Node oldN = n;
                n = n.getNodeFromCommand(c);
                
                for (int j = 0; j < numberOfAgents; j++) {
                    if (j != i) {
                        Node.walls[positions[j].row][positions[j].col] = false;
                    }
                }
                for (Position p : oldPositions) {
                    Node.walls[p.row][p.col] = false;
                }
                if (n == null) {
                    throw new conflictException(stepsTaken);
                }
                for (int j = 0; j < oldN.boxList.size(); j++) {
                    if (oldN.boxList.get(j).col != n.boxList.get(j).col || oldN.boxList.get(j).row != n.boxList.get(j).row) {
                        oldPositions.add(new Position(oldN.boxList.get(j)));
                        oldPositions.add(new Position(n.boxList.get(j)));
                        tempBoxPositions.add(new Pair<>(j, new Position(n.boxList.get(j))));
                        n.boxList.get(j).row = -10;
                    }
                }
                oldPositions.add(positions[i]);
                positions[i] = new Position(n.agent);
            }
            
            oldPositions.clear();
            for (Pair<Integer, Position> p : tempBoxPositions) {
                n.boxList.get(p.a).row = p.b.row;
                n.boxList.get(p.a).col = p.b.col;
            }
            tempBoxPositions.clear();
            stepsTaken ++;
        }
        
        return new Pair<>(n, positions);
    }

    public Command[][] getPlan() {
        Command[][] returnPlan = new Command[length][numberOfAgents];
        System.arraycopy(plan, 0, returnPlan, 0, length);
        return replaceNullWithNoOp(returnPlan);
    }

    public Command[][] replaceNullWithNoOp(Command[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                arr[i][j] = (arr[i][j] == null) ? new Command() : arr[i][j];
            }
        }
        return arr;
    }

    public MasterPlan copy() {
        MasterPlan mp = new MasterPlan(numberOfAgents);
        mp.length = this.length;
        mp.plan = new Command[this.plan.length][numberOfAgents];
        for (int i = 0; i < this.plan.length; i++) {
            Command[] arr = new Command[numberOfAgents];
            System.arraycopy(this.plan[i], 0, arr, 0, numberOfAgents);
            mp.plan[i] = arr;
        }
        System.arraycopy(this.lastMoveStep, 0, mp.lastMoveStep, 0, numberOfAgents);
        return mp;
    }

    public int getNextMoveTimeFromAgent(int agent) {
        return lastMoveStep[agent] + 1;
    }

    public void insertPlanAtTime(int agent, int startStep, Command[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (startStep + i >= length) {
                addStepToPlan(agent, arr[i]);
            } else {
                plan[startStep + i][agent] = arr[i];
            }
        }
        lastMoveStep[agent] = Math.max(lastMoveStep[agent], startStep + arr.length - 1);
    }
    
    public void removeRepetitiveStates(Node initState, Position[] initPositions, Agent[] oldAgents, boolean[][] initWalls) {
        // if (true) return;
        for (int i  = 0; i < initWalls.length; i++) {
            for (int j = 0; j < initWalls[i].length; j++) {
                Node.walls[i][j] = initWalls[i][j];
            }
        }
        Agent[] agents = new Agent[oldAgents.length];
        for (int i = 0; i < numberOfAgents; i++) {
            agents[i] = new Agent(initPositions[i].row, initPositions[i].col, oldAgents[i].id, oldAgents[i].color);
        }
        MultiNode setNode = new MultiNode(initState, oldAgents);
        int stepsTaken = 0;
        Map<MultiNode, Integer> observedNodes = new HashMap<MultiNode, Integer>(); // Nodes and at which step they were observed
        observedNodes.put(setNode, stepsTaken);

        Position[] positions = new Position[numberOfAgents];
        List<Position> oldPositions = new ArrayList<>();
        for (int i = 0; i < numberOfAgents; i++) {
            positions[i] = new Position(initPositions[i]);
        }

        Node n = initState.ChildNode();
        
        while (stepsTaken < length) {
            agents = new Agent[oldAgents.length];
            for (int i = 0; i < numberOfAgents; i++) {
                n.agent = new Agent(positions[i].row, positions[i].col, oldAgents[i].id, oldAgents[i].color);
                // simulate command plan[stepsTaken][i]
                Command c = plan[stepsTaken][i];
                if (c == null || c.actionType == Command.Type.NoOp) {
                    continue;
                }
                for (int j = 0; j < numberOfAgents; j++) {
                    if (j != i) {
                        Node.walls[positions[j].row][positions[j].col] = true;
                    }
                }
                for (Position p : oldPositions) {
                    Node.walls[p.row][p.col] = true;
                }
                Node oldN = n;
                n = n.getNodeFromCommand(c);
                if (n == null) {
                    return;
                }
                agents[i] = n.agent;
                
                for (int j = 0; j < numberOfAgents; j++) {
                    if (j != i) {
                        Node.walls[positions[j].row][positions[j].col] = false;
                    }
                }
                for (Position p : oldPositions) {
                    Node.walls[p.row][p.col] = false;
                }

                for (int j = 0; j < oldN.boxList.size(); j++) {
                    if (oldN.boxList.get(j).col != n.boxList.get(j).col || oldN.boxList.get(j).row != n.boxList.get(j).row) {
                        oldPositions.add(new Position(oldN.boxList.get(j)));
                    }
                }
                oldPositions.add(positions[i]);
                positions[i] = new Position(n.agent);
            }            
            
            oldPositions.clear();
            stepsTaken += 2;

            setNode = new MultiNode(n, agents);
            if (observedNodes.containsKey(setNode)) {
                int startOfSlice = observedNodes.get(setNode);
                int lengthRemoved = stepsTaken - startOfSlice;
                length = length - lengthRemoved;
                for (int i = 0; i < numberOfAgents; i++) {
                    for (int j = startOfSlice; j < length; j++) {
                        plan[j][i] = plan[j+lengthRemoved][i];
                    }
                }
            } else {
                observedNodes.put(setNode, stepsTaken);
            }
        }
    }
}