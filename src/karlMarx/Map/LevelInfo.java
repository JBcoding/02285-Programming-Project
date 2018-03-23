package karlMarx.Map;

import karlMarx.Agent;
import karlMarx.Color;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LevelInfo {
    private static Map<Integer, Color> boxIdToColors;
    private static Map<Color, Set<Agent>> colorToAgents;
    private static Map<DualKey, Character> goalPos;
    private static Map<Character, Integer> numberOfGoalsOfThatType;

    public static Color getBoxColorFromId(int BoxId) {
        return boxIdToColors.get(BoxId);
    }

    public static Set<Agent> getAgentsFromColor(Color color) {
        return (colorToAgents.containsKey(color))
                ? colorToAgents.get(color) : colorToAgents.put(color,new HashSet<>());
    }

    public static Map<Color, Set<Agent>> getColorToAgents() {
        return colorToAgents = (colorToAgents == null) ? new HashMap<>() : colorToAgents;
    }

    public static Map<Integer, Color> getBoxIdToColors() {
        return boxIdToColors = (boxIdToColors == null) ? new HashMap<>() : boxIdToColors;
    }

    public static boolean isGoalAtPos(int row, int col) {
        return (goalPos = (goalPos != null) ? goalPos : new HashMap<>(4)).containsKey(new DualKey(row,col));
    }

    public static void addGoal(int row, int col, char type) {
        (goalPos = (goalPos != null) ? goalPos : new HashMap<>(4)).put(new DualKey(row,col),type);
        numberOfGoalsOfThatType = (numberOfGoalsOfThatType != null) ? numberOfGoalsOfThatType : new HashMap<>();
        int amount = numberOfGoalsOfThatType.getOrDefault(type, 0);
        amount++;
        numberOfGoalsOfThatType.put(type, amount);
    }
}



class DualKey {

    private final int row;
    private final int col;

    public DualKey(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DualKey)) return false;
        DualKey key = (DualKey) o;
        return row == key.row && col == key.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

}