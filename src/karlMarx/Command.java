package karlMarx;

import java.util.LinkedList;

public class Command {
    // Order of enum important for determining opposites.
    public static enum Dir {
        N, W, E, S
    }
    ;

    public static enum Type {
        Move, Push, Pull, Help, NoOp;

    }
    ;

    public static final Command[] EVERY;

    static {
        LinkedList<Command> cmds = new LinkedList<Command>();
        for (Dir d1 : Dir.values()) {
            cmds.add(new Command(d1));
        }

        for (Dir d1 : Dir.values()) {
            for (Dir d2 : Dir.values()) {
                if (!isOpposite(d1, d2)) {
                    cmds.add(new Command(Type.Push, d1, d2));
                }
                if (d1 != d2) {
                    cmds.add(new Command(Type.Pull, d1, d2));
                }
            }
        }

        EVERY = cmds.toArray(new Command[0]);
    }

    public static boolean isOpposite(Dir d1, Dir d2) {
        return d1.ordinal() + d2.ordinal() == 3;
    }

    public static int dirToRowChange(Dir d) {
        if (d == null) {return 0;}
        // South is down one row (1), north is up one row (-1).
        switch (d) {
            case S:
                return 1;
            case N:
                return -1;
            default:
                return 0;
        }
    }

    public static int dirToColChange(Dir d) {
        if (d == null) {return 0;}
        // East is right one column (1), west is left one column (-1).
        switch (d) {
            case E:
                return 1;
            case W:
                return -1;
            default:
                return 0;
        }
    }

    public final Type actionType;
    public final Dir dir1;
    public final Dir dir2;
    public final Box box;

    public Command(Dir d) {
        this.actionType = Type.Move;
        this.dir1 = d;
        this.dir2 = null;
        this.box = null;
    }

    public Command(Type t, Dir d1, Dir d2) {
        this.actionType = t;
        this.dir1 = d1;
        this.dir2 = d2;
        this.box = null;
    }

    public Command(Box box) {
        this.actionType = Type.Help;
        this.dir1 = null;
        this.dir2 = null;
        this.box = box;
    }

    public Command() {
        this.actionType = Type.NoOp;
        this.dir1 = null;
        this.dir2 = null;
        this.box = null;
    }

    @Override
    public String toString() {
        switch (this.actionType) {
            case Move:
                return String.format("%s(%s)", this.actionType.toString(), this.dir1.toString());
            case Push:
            case Pull:
                return String.format("%s(%s,%s)", this.actionType.toString(), this.dir1.toString(), this.dir2.toString());
            case Help:
                return String.format("%s(%s)", this.actionType.toString(), this.box.toString());
            case NoOp:
                return this.actionType.toString();
            default:
                return null;
        }
    }
}
