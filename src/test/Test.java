package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;


import karlMarx.Driver;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    private static final int[] SA_SOLUTION_LENGTHS = {
            124, 927, 1930, 3592, 604, 100, 239, 202, 521, 238, 100000, 193, 316,
            262, 60, 212, 3262, 3687, 12662, 114, 3213, 228, 234, 95, 953
    };

    private static final int[] SA_SOLUTION_TIMES = {
            17, 608, 1416, 2307, 203, 3955, 142, 152, 287, 182, 100000, 3341, 252,
            205, 15, 268, 5970, 433, 1830, 118, 1054, 122, 269, 80, 484
    };

    private static final int[] MA_SOLUTION_LENGTHS = { // TODO: not added
            124, 927, 1930, 3592, 604, 100, 239, 202, 521, 238, 100000, 193, 316,
            262, 60, 212, 3262, 3687, 12662, 114, 3213, 228, 234, 95, 953
    };

    private static final int[] MA_SOLUTION_TIMES = { // TODO: not added
            124, 927, 1930, 3592, 604, 100, 239, 202, 521, 238, 100000, 193, 316,
            262, 60, 212, 3262, 3687, 12662, 114, 3213, 228, 234, 95, 953
    };
    
    public static void main(String[] args) {
        String arg = "";
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
            case "-sa": arg = "SA"; break;
            case "-ma": arg = "MA"; break;
            }
        }
        try {
            testAllLevels(arg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String[] COMMAND = new String[] {
            "java", 
            "-jar", 
            "environment/server.jar", 
            "-l", 
            "%s", 
            "-c", 
            "java -cp out karlMarx.Driver", 
            //"-g",
            //"-30",
            "-t", 
            "30"
        };
    
    private static void testAllLevels(String saOrMa) throws Exception {
        String topDir = new java.io.File( "." ).getCanonicalPath();
        COMMAND[2] = String.format(COMMAND[2], topDir);

        String levelDir = "/environment/comp_levels_2017/";
        Path currPath = Paths.get(topDir + levelDir);
        
        int totalSolved = 0;
        int totalSolutionLength = 0;
        long totalSolutionTime = 0;

        List<Integer> solutionLengths = new ArrayList<Integer>();
        List<Integer> solutionTimes = new ArrayList<Integer>();

        try (DirectoryStream<Path> stream =
           Files.newDirectoryStream(currPath, saOrMa + "SA*.lvl")) {
               for (Path entry: stream) {
                   System.out.println(entry.toAbsolutePath().toString().substring(0, entry.toAbsolutePath().toString().lastIndexOf("/")));
                   String[] copy = Arrays.copyOf(COMMAND, COMMAND.length);
                   copy[4] = String.format(copy[4], entry.toAbsolutePath());
                   long b = System.currentTimeMillis();
                   String s = runJob(copy);
                   System.err.println(s);
                   try {
                       int solutionLength = Integer.parseInt(s);
                       totalSolved++;
                       totalSolutionLength += solutionLength;
                       solutionLengths.add(solutionLength);
                       System.out.print(" Number of steps to solve: " + solutionLength);
                       long solutionTime = System.currentTimeMillis() - b;
                       totalSolutionTime += solutionTime;
                       solutionTimes.add((int)solutionTime);
                       System.out.println(" Time to solve: " + solutionTime);
                   } catch (NumberFormatException e) {}
               }
           } catch (IOException x) {
               // IOException can never be thrown by the iteration.
               // In this snippet, it can // only be thrown by newDirectoryStream.
               System.err.println(x);
               System.exit(0);
           }
        long after = System.currentTimeMillis();
        
        System.out.println("Solved " + totalSolved + " levels");
        System.out.println("Total length of all solutions is: " + totalSolutionLength +
                "(" + getPoints(solutionLengths, saOrMa.equalsIgnoreCase("sa") ? SA_SOLUTION_LENGTHS : MA_SOLUTION_LENGTHS) + " points)");
        System.out.println("Total time used (ms): " + totalSolutionTime +
                "(" + getPoints(solutionLengths, saOrMa.equalsIgnoreCase("sa") ? SA_SOLUTION_TIMES : MA_SOLUTION_TIMES) + " points)");
    }

    private static String runJob(String... job) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(job);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        BufferedWriter stdOutput = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        try {

            String s = null;
            String next = null;
            while ((next = stdError.readLine()) != null) {
                if (next.contains("Unable to read next action from client")) return "";
                s = next;
                //System.err.println(s);
                s = s.substring("[Client said] ".length());
                try {
                    if (s.charAt(0) == Driver.NO_SOLUTION) {
                        return s;
                    }
                    Integer.parseInt(s);
                    return s;
                    
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {}
            }
        } finally {
            stdInput.close();
            stdError.close();
            stdOutput.close();
        }
        proc.destroy();
        proc.destroyForcibly();
        return "";
    }


    private static double getPoints(List<Integer> results, int[] previousYears) {
        double sum = 0;
        for (int i = 0; i < results.size(); i++) {
            sum += previousYears[i] / (double)results.get(i);
        }
        return sum;
    }
}
