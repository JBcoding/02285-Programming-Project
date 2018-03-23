package karlMarx.Map;

import karlMarx.Agent;
import karlMarx.Color;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class levelInfo {
    private static Map<Integer, Color> boxIdToColors;
    private static Map<Color, Set<Agent>> colorToAgents;

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
}
