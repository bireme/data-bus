package org.bireme.barr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;

/**
 *
 * @author Heitor Barbieri
 * date 20130315
 */
public class BarrClient {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = BarrMultiServer.DEFAULT_PORT;
    
    private static void usage() {
        System.err.println("usage: BarrClient <infile> [host=<host>] " +
                                                               "[port=<port>]");
        System.exit(1);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
        }
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        for (int counter = 1; counter < args.length; counter++) {
            if (args[counter].startsWith("host=")) {
                host = args[counter].substring(5);
            } else if (args[counter].startsWith("port=")) {
                port = Integer.parseInt(args[counter].substring(5));
            } else {
                usage();
            }
        }
        final BufferedReader reader = new BufferedReader(
                                                       new FileReader(args[0]));
        final ArrayDeque<Long> ids = new ArrayDeque<>();
        
        while (true) {
            final String cmd = reader.readLine();
            
            if (cmd == null) {
                break;
            }
            if (!cmd.trim().isEmpty()) {
                try (Socket socket = new Socket(host, port)) {
                    if (!socket.isConnected()) {
                        throw new IOException("socket is not connected");
                    }
                    try (PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(),
                                                             "UTF-8"), true); 
                         final BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(),
                                                              "UTF-8"))) {
                        //System.out.println(cmd);
                        out.println(cmd);
                        final String line = in.readLine();
//System.out.println("line=" + line);
                        final String linet = line.trim();
                        
                        if (linet.startsWith("id=")) {
                            ids.add(Long.valueOf(linet.substring(3)));
                        } else {
                            System.out.println(linet);
                            System.out.println();   
                        }                        
                    }
                }
            }
        }
        
        while (!ids.isEmpty()) {
            try (Socket socket = new Socket(host, port); 
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(),
                                                         "UTF-8"), true); 
                 BufferedReader in = new BufferedReader(
                           new InputStreamReader(socket.getInputStream(),
                                                         "UTF-8"))) {
                final StringBuilder builder = new StringBuilder();
                final Long id = ids.pollFirst();
                boolean finished = false;

                out.println("id=" + id);

                while (true) {
                    final String line = in.readLine();
                    if (line == null) {
                        finished = true;
                        break;
                    }
                    if (line.contains("status:processing")) {
                        break;
                    }
                    builder.append(line);
                    builder.append("\n");
                }

                if (finished) {
                    //System.out.println("id=" + id);
                    System.out.println(builder.toString());            
                    System.out.println(
                            "--------------------------------------------");
                    System.out.println();
                } else {
                    ids.addLast(id);
                }
            }
//System.out.println("ids = " + ids);
        }
        System.out.println("FINISHED!!!");
    }    
}
