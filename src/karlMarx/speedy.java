package karlMarx;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class speedy {
    /**
     * A TCP server that runs on port 9090.  When a client connects, it
     * sends the client the current date and time, then closes the
     * connection with that client.  Arguably just about the simplest
     * server you can write.
     */

    /**
     * Runs the server.
     */
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(9090);
        try {
            String[] COMMAND = new String[] {
                    "java",
                    "-Xms6g",
                    "-cp",
                    "src/out/",
                    "karlMarx.Driver",
                    "-wastar"
            };

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(COMMAND);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            PrintWriter stdOutput = new PrintWriter(new OutputStreamWriter(proc.getOutputStream()));

            Socket socket = listener.accept();
            try {
                BufferedReader input =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output =
                        new PrintWriter(socket.getOutputStream(), true);


                final boolean[] active = {true};

                Thread threadInput = new Thread(){
                    public void run(){
                        while (true) {
                            try {
                                String s = input.readLine();
                                if (s != null) {
                                    //System.out.println("c -> java: " + s);
                                    stdOutput.println(s);
                                    stdOutput.flush();
                                } else {
                                    active[0] = false;
                                    return;
                                }
                            } catch (IOException e) {}
                        }
                    }
                };
                threadInput.start();


                Thread threadOutput = new Thread(){
                    public void run(){
                        Scanner in = new Scanner(stdInput);
                        while (active[0]) {
                            String s = in.nextLine();
                            //System.out.println("java -> c:" + s);
                            output.println(s);
                            output.flush();
                        }
                    }
                };

                threadOutput.start();


                Thread threadOutputErr = new Thread(){
                    public void run(){
                        Scanner in = new Scanner(stdError);
                        while (active[0]) {
                            String s = in.nextLine();
                            System.out.println("Error    :" + s);
                        }
                    }
                };

                threadOutputErr.start();

                threadInput.join();

                System.out.println("Done");

                System.exit(0);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                stdInput.close();
                stdError.close();
                stdOutput.close();

                proc.destroy();
                proc.destroyForcibly();

                socket.close();
            }
        } finally {
            listener.close();
        }
    }
}
