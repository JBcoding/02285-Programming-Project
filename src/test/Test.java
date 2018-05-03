package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;


import karlMarx.Driver;
import karlMarx.Pair;

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
            52, 100000, 214, 4, 1108, 100000, 188, 41, 1563, 107, 53, 80, 12, 31, 27,
            32, 231, 579, 1004, 192, 69, 99, 842, 128, 214
    };

    private static final int[] MA_SOLUTION_TIMES = { // TODO: not added
            46, 100000, 526, 15, 1714, 100000, 1168, 71, 2073, 264, 71, 281, 106, 128, 114,
            104, 216, 658, 700, 253, 151, 385, 902, 265, 530
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
            "java -cp src karlMarx.Driver -wastar", 
            //"-g",
            //"-30",
            "-t", 
            "10" 
        };
    
    private static void testAllLevels(String saOrMa) throws Exception {
        String topDir = new java.io.File( "." ).getCanonicalPath();
        COMMAND[2] = String.format(COMMAND[2], topDir);

        String levelDir = "/environment/comp_levels_2017/";
        Path currPath = Paths.get(topDir + levelDir);
        
        int totalSolved = 0;
        int totalSolutionLength = 0;
        long totalSolutionTime = 0;

        double lengthPoints = 0;
        double timePoints = 0;

        List<Integer> solutionLengths = new ArrayList<Integer>();
        List<Integer> solutionTimes = new ArrayList<Integer>();
        int[] prevSolutionLengths = saOrMa.equalsIgnoreCase("sa") ? SA_SOLUTION_LENGTHS : MA_SOLUTION_LENGTHS;
        int[] prevSolutionTimes = saOrMa.equalsIgnoreCase("sa") ? SA_SOLUTION_TIMES : MA_SOLUTION_TIMES;
        int i = 0;
        try (DirectoryStream<Path> stream =
           Files.newDirectoryStream(currPath, saOrMa + "*.lvl")) {
               for (Path entry: stream) {
                   System.out.print(entry.getFileName());
                   String[] copy = Arrays.copyOf(COMMAND, COMMAND.length);
                   copy[4] = String.format(copy[4], entry.toAbsolutePath());
                   long b = System.currentTimeMillis();
                   System.err.println(entry.getFileName());
                   Pair<String, String> s = runJob(copy);
                   System.err.println(s);
                   try {
                       int solutionLength = Integer.parseInt(s.a);
                       int solutionTime = Integer.parseInt(s.b);
                       totalSolved++;
                       totalSolutionLength += solutionLength;
                       totalSolutionTime += solutionTime;
                       solutionLengths.add(solutionLength);
                       solutionTimes.add(solutionTime);

                       double lengthPoint = (prevSolutionLengths[i] / (double)solutionLength);
                       System.out.printf(" Number of steps to solve: " + solutionLength + " (%.2f points)", lengthPoint);
                       double timePoint = (prevSolutionTimes[i] / (double)solutionTime);
                       System.out.printf(" Time to solve: " + solutionTime + " (%.2f points)\n", timePoint);
                       lengthPoints += lengthPoint;
                       timePoints += timePoint;
                   } catch (NumberFormatException | NullPointerException e) {
                       System.out.println(" Could not solve this level");
                   }
                   i++;
               }
           } catch (IOException x) {
               // IOException can never be thrown by the iteration.
               // In this snippet, it can // only be thrown by newDirectoryStream.
               System.err.println(x);
               System.exit(0);
           }
        long after = System.currentTimeMillis();
        
        System.out.println("Solved " + totalSolved + " levels");
        System.out.printf("Total length of all solutions is: " + totalSolutionLength +
                " (%.2f points)\n", lengthPoints);
        System.out.printf("Total time used (ms): " + totalSolutionTime +
                " (%.2f points)\n", timePoints);
    }

    private static Pair<String, String> runJob(String... job) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(job);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        BufferedWriter stdOutput = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        try {

            String s = null;
            String next = null;
            while ((next = stdError.readLine()) != null) {
                if (next.contains("Unable to read next action from client")) return null;
                s = next;
                s = s.substring("[Client said] ".length());
                try {
                    if (s.charAt(0) == Driver.NO_SOLUTION) {
                        return null;
                    }
                    Integer.parseInt(s);
                    String time = stdError.readLine();
                    return new Pair<String, String>(s, time.substring("[Client said] ".length()));
                    
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {}
            }
        } finally {
            stdInput.close();
            stdError.close();
            stdOutput.close();
        }
        proc.destroy();
        proc.destroyForcibly();
        return null;
    }
}
