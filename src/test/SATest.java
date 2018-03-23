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

public class SATest {
    
    public static void main(String[] args) {
        String arg = "";
        if (args.length > 0) {
            switch (args[1].toLowerCase()) {
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

    //private final String[] INIT = new String[] { "javac", "%s" };
    private static final String[] COMMAND = new String[] {
            "java", 
            "-jar", 
            "environment\\server.jar", 
            "-l", 
            "\"%s\"", 
            "-c", 
            "\"java -Xmx4g -Xms4g -cp src karlMarx.Driver\"", 
            "-g",
            "-30",
            "-t", 
            "300" 
        };
    
    private static void testAllLevels(String saOrMa) throws Exception {
        String topDir = new java.io.File( "." ).getCanonicalPath();
        COMMAND[2] = String.format(COMMAND[2], topDir);
        
        String levelDir = "\\environment\\levels\\";
        Path currPath = Paths.get(topDir + levelDir);
        
        int totalSolved = 0;
        int totalSolutionLength = 0;
        
        long before = System.currentTimeMillis();
        try (DirectoryStream<Path> stream =
           Files.newDirectoryStream(currPath, saOrMa + "*.lvl")) {
               for (Path entry: stream) {
                   String[] copy = Arrays.copyOf(COMMAND, COMMAND.length);
                   copy[4] = String.format(copy[4], entry.toAbsolutePath());
                   String s = runJob(copy);
                   try {
                       int solutionLength = Integer.parseInt(s);
                       totalSolved++;
                       totalSolutionLength += solutionLength;
                   } catch (NumberFormatException e) {
                       System.out.println("Could not parse: " + s);
                   }
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

        BufferedReader stdInput = new BufferedReader(new 
             InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new 
             InputStreamReader(proc.getErrorStream()));
        BufferedWriter stdOutput = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        try {

            String s = null;
            String next = null;
            while ((next = stdError.readLine()) != null) {
                s = next;
                System.out.println(s);
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
        return "";
    }

}
