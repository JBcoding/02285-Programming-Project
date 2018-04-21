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
import java.util.Arrays;

public class Test {
    
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
        
        long before = System.currentTimeMillis();
        try (DirectoryStream<Path> stream =
           Files.newDirectoryStream(currPath, saOrMa + "SA*.lvl")) {
               for (Path entry: stream) {
                   System.out.println(entry);
                   String[] copy = Arrays.copyOf(COMMAND, COMMAND.length);
                   copy[4] = String.format(copy[4], entry.toAbsolutePath());
                   String s = runJob(copy);
                   System.err.println(s);
                   try {
                       int solutionLength = Integer.parseInt(s);
                       totalSolved++;
                       totalSolutionLength += solutionLength;
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
        System.out.println("Total length of all solutions is: " + totalSolutionLength);
        System.out.println("Total time used (ms): " + (after - before));
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

}
