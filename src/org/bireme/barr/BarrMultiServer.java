package org.bireme.barr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * date 20130311
 */
public class BarrMultiServer {
    public static final int DEFAULT_PORT = 5050;
    public static final String DBNAME = "ProcessDB";
    private static final Logger logger = Logger.getLogger("BarrMultiServer");
    
    private static void usage() {
        System.err.println(
                "usage: BarrMultiServer [-port=<port>] [-ipfile=<file path>]");
        System.exit(1);
    }
    
    public static void main(String[] args) {        
        try {
            final boolean listening = true;
            final ProcessDAOLucene dao = new ProcessDAOLucene(DBNAME);
            
            int port = DEFAULT_PORT;            
            List<String> ips = null;
            ServerSocket serverSocket = null;
            boolean securityon = false;

            for (int idx = 1; idx < args.length; idx++) {
                if (args[idx].startsWith("-port=")) {
                    port = Integer.parseInt(args[idx].substring(6));
                } else if (args[idx].startsWith("-ipfile=")) {
                    ips = readAllowedIPs(args[idx].substring(8));
                    securityon = true;
                } else {
                    usage();
                }
            }
            
            try {
                serverSocket = new ServerSocket(port);
                logger.info("Start listening on port: " + port);
                if (securityon) {
                    logger.info("Security mode is: ON");
                } else {
                    logger.info("Security mode is: OFF");
                }
            } catch (IOException e) {
                logger.warning("Could not listen on port: " + port);
                System.exit(-1);
            }

            while (listening) {
                final Socket client = serverSocket.accept();
                new BarrMultiServerThread(client, dao, ips).start();
            }

            // serverSocket.close();
        } catch(NumberFormatException | IOException ex) {
            logger.warning(ex.toString());
        }
    }
    
    private static List<String> readAllowedIPs(final String inFile) 
                                                            throws IOException {
        if (inFile == null) {
            throw new NullPointerException("inFile");
        }
        final List<String> ips = new ArrayList<>();
        try (BufferedReader reader = 
                                   new BufferedReader(new FileReader(inFile))) {
        
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final String ip = line.trim();
                if (!ip.isEmpty()) {
                    ips.add(ip);
                }
            }
        }
        
        return ips;
    }
}
