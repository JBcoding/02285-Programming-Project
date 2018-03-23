package karlMarx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class Driver {

    public static final char NO_SOLUTION = '\u0000';

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Read level and create the initial state of the problem
        ArrayList<Node> initialStates = LevelReader.readLevel(serverMessages);
        
        String strategy = "";
        if (args.length > 0) {
            strategy = args[0];
        }

        SearchClient searchClient;
        if (initialStates.size() == 1) {
            searchClient = new SASearchClient();            
        } else {
            searchClient = new MASearchClient();                    
        }
        
        List<Node> solution;
        
        try {
            solution = searchClient.Search(strategy, initialStates); // ezpzlmnsqz
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            solution = null;
        }

        if (solution == null) {
            System.err.println(searchClient.searchStatus());
            System.err.println("Unable to solve level.");
            System.out.println(NO_SOLUTION);
        } else {
            System.err.println("\nSummary for " + strategy.toString());
            System.err.println("Found solution of length " + solution.size());
            System.err.println(searchClient.searchStatus());
            System.err.println(solution.size());

            for (Node n : solution) {
                String act = n.action.toString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n.toString());
                    break;
                }
            }
        }
    }
}
