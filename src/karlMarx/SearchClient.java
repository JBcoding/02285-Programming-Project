package karlMarx;

import java.io.IOException;
import java.util.List;

public abstract class SearchClient {
    
    public abstract List<Command> Search(String strategyArg, List<Node> initialStates) throws IOException;

    public abstract String searchStatus();
    
}
