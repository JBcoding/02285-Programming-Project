package karlMarx;

import java.util.ArrayList;
import java.util.List;

public class SearchState {
    private Position position;
    private boolean pushing;
    private boolean pulling;
    private Box box; // Pulling Or Pushing
    private Position boxPosition; // We do and will not change position of the box object
    private SearchState parent;
    private Command action;
    private boolean hasTurnedAround;

    private int _hash = -1;

    public SearchState(Position position) {
        this.position = new Position(position);
        this.pushing = false;
        this.pulling = false;
        this.box = null;
        this.boxPosition = null;
        this.parent = null;
        this.action = null;
        this.hasTurnedAround = false;
    }

    public SearchState(Position position, SearchState parent, Command action) {
        this(position);
        this.pushing = parent.pushing;
        this.pulling = parent.pulling;
        this.box = parent.box;
        this.parent = parent;
        this.hasTurnedAround = parent.hasTurnedAround;
        this.action = action;
    }

    public Position getPosition() {
        return position;
    }


    @Override
    public int hashCode() {
        if (_hash != -1) {
            return _hash;
        }
        _hash = 1;
        int prime = 31;
        _hash = _hash * prime + position.hashCode();
        if (box != null) {
            _hash = _hash * prime + box.hashCode();
        }
        _hash = _hash * 2 + (pushing ? 1 : 0);
        _hash = _hash * 2 + (pulling ? 1 : 0);
        return _hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        SearchState other = (SearchState) obj;
        // omitted action and parent on purpose
        if (!(other.position.equals(position) && other.pushing == pushing && other.pulling == pulling)) {
            return false;
        }
        if (box == null && other.box == null) {
            return true;
        }
        if (!(box.equals(other.box) && boxPosition.equals(other.boxPosition))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (box != null) {
            return String.valueOf(position) + "  --  " + box.letter + " -- " + boxPosition + " -- " + pulling + " -- " + pushing;
        } else {
            return String.valueOf(position);
        }
    }

    public Box getBox() {
        return box;
    }

    public boolean isPushing() {
        return pushing;
    }

    public boolean isPulling() {
        return pulling;
    }

    public void setBoxPosition(Position boxPosition) {
        this.boxPosition = boxPosition;
    }

    public Position getBoxPosition() {
        return boxPosition;
    }

    public boolean hasTurnedAround() {
        return hasTurnedAround;
    }

    public void setHasTurnedAround() {
        hasTurnedAround = true;
    }

    public void swapPullPush() {
        boolean temp = pulling;
        pulling = pushing;
        pushing = temp;
    }

    public SearchState copy() {
        SearchState s = new SearchState(position);
        s.pushing = this.pushing;
        s.pulling = this.pulling;
        s.box = this.box;
        s.boxPosition = this.boxPosition;
        s.parent = this.parent;
        s.action = this.action;
        s.hasTurnedAround = this.hasTurnedAround;
        return s;
    }

    public void setBox(Box box) {
        this.box = box;
    }

    public void setPulling(boolean pulling) {
        this.pulling = pulling;
    }

    public List<Command> backTrack() {
        if (action == null) {
            return new ArrayList<>();
        } else {
            List<Command> path = parent.backTrack();
            path.add(action);
            return path;
        }
    }
}
