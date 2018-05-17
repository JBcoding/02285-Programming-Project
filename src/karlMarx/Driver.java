package karlMarx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Driver {

    public static final char NO_SOLUTION = '\u0000';

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Read level and create the initial state of the problem
        ArrayList<Node> initialStates = LevelReader.readLevel(serverMessages);
        
        String strategy = "";
        if (args.length > 0) {
            strategy = args[0];
        }

        if (initialStates.size() == 1) {
            SASearchClient searchClient = new SASearchClient();

            List<Command> solution;

            try {
                solution = searchClient.Search(strategy, initialStates); // ezpzlmnsqz
            } catch (OutOfMemoryError ex) {
                System.err.println("Maximum memory usage exceeded.");
                solution = null;
            }

            if (solution == null) {
                //System.err.println(searchClient.searchStatus());
                //System.err.println("Unable to solve level.");
                System.err.println(NO_SOLUTION);
                System.exit(0);
            } else {
                //System.err.println("\nSummary for " + strategy);
                //System.err.println("Found solution of length " + solution.size());
                System.err.println(solution.size());
                System.err.println(System.currentTimeMillis() - startTime);

                for (Command c : solution) {
                    String act = "[" + c + "]";
                    System.out.println(act);
                    //System.err.println(act);
                    String response = serverMessages.readLine();
                    if (response.contains("false")) {
                        System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                        System.err.format("%s was attempted in \n%s\n", act, c.toString());
                        break;
                    }
                }
            }
        } else {
            MASearchClient searchClient = new MASearchClient();

            Command[][] solution;

            try {
                solution = searchClient.Search(strategy, initialStates); // ezpzlmnsqz
            } catch (OutOfMemoryError ex) {
                System.err.println("Maximum memory usage exceeded.");
                solution = null;
            }

            if (solution == null) {
                // System.err.println(searchClient.searchStatus());
                //System.err.println("Unable to solve level.");
                System.out.println(NO_SOLUTION);
                System.exit(0);
            } else {
                //System.err.println("\nSummary for " + strategy);
                //System.err.println("Found solution of length " + solution.length);
                System.err.println(solution.length);
                System.err.println(System.currentTimeMillis() - startTime);

                for (Command[] arr : solution) {
                    String act = Arrays.toString(arr);

                    System.out.println(act);
                    //System.err.println(act);
                    String response = serverMessages.readLine();
                    if (response.contains("false")) {
                        System.err.format("Server responded with %s to the inapplicable action: %s\n", response, act);
                        System.err.format("%s was attempted\n", act);
                        break;
                    }
                }
            }

            // Print stuff finally for tests to read
            //System.err.println(solution == null ? NO_SOLUTION : solution.length);
            System.exit(0);
        }
    }
}
