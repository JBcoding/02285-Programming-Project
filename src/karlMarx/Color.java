package karlMarx;

public enum Color {
    BLUE, RED, GREEN, MAGENTA, ORANGE, PINK, YELLOW;

    public static Color toColor(String color) {
        switch (color.toLowerCase()) {
        case "blue":
            return BLUE;
        case "red":
            return RED;
        case "green":
            return GREEN;
        case "magenta":
            return MAGENTA;
        case "orange":
            return ORANGE;
        case "pink":
            return PINK;
        case "yellow":
            return YELLOW;
        }

        throw new IllegalArgumentException("Wrong color: " + color);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
