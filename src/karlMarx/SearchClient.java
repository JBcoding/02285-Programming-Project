package karlMarx;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public abstract class SearchClient {
    
    public abstract List<Node> Search(String strategyArg, List<Node> initialStates) throws IOException;

    public abstract String searchStatus();
    
}
