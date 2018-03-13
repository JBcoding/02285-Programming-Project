package karlMarx;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public abstract class SearchClient {

    protected Strategy[] strategies;
    
    public abstract LinkedList<Node> Search(String strategyArg, List<Node> initialStates) throws IOException;

    public String searchStatus() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < strategies.length; i++) {
            Strategy strategy = strategies[i];
            s.append("Status for agent ");
            s.append(i);
            s.append(": ");
            s.append(strategy.searchStatus());
        }
        return s.toString();
    }
}
