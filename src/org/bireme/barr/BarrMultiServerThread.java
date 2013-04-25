package org.bireme.barr;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * date 20130411
 */
public class BarrMultiServerThread extends Thread {
    private final Socket socket;
    private final ProcessDAO dao;
    private final List<String> ips;
    private static final Logger logger = 
                                      Logger.getLogger("BarrMultiServerThread");
 
    public BarrMultiServerThread(final Socket socket,
                                   final ProcessDAO dao,
                                   final List<String> ips) {
        super("BarMultiServerThread");
        this.socket = socket;
        this.dao = dao;        
        this.ips = ips;
    }
 
    @Override
    public void run() {
        
        try {
            
            final long id = System.currentTimeMillis();

            String cmd = null;                
            boolean show = false;
            final String outStr;        
            final String ip = socket.getInetAddress().getHostAddress();
            
            try (final PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(),
                                                   "UTF-8"), true)) {
                if ((ips == null) || (ips.contains(ip)) 
                                  || (ip.equals("127.0.0.1"))) {
                    final BufferedReader in = new BufferedReader(
                                                  new InputStreamReader(
                                                      socket.getInputStream(),
                                                      "UTF-8"));
                    final String inputLine = in.readLine();                    

                    if (inputLine == null) {
                        outStr = "Error: null operation";
                    } else {
                        final String inStr = inputLine.trim();
                        if (inStr.startsWith("cmd=")) {
                            outStr = "id: " + id;
                            cmd = inStr.substring(4);
                        } else if (inStr.startsWith("ocmd=")) {                        
                            outStr = "id: " + id;
                            cmd = inStr.substring(5);
                            show = true;
                        } else if (inStr.startsWith("id=")) {
                            outStr = getIdInfo(
                                        Long.parseLong(inStr.substring(3)));
                        } else {
                            outStr = "Error: Invalid operation";
                        }                    
                    }                    
                    if (cmd != null) {
                        dao.insertResult(id, cmd, "processing", -1, null);
                    }
                } else {
                    outStr = "Error: Unauthorized IP";                    
                }                
                out.println(outStr);                
                if (cmd != null) {          
                    execCommand(id, cmd, show);
                }
            }
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.warning(ex.toString());
        }                            
    }
    
    private void execCommand(final long id,
                              final String command,
                              final boolean showOutput) throws Exception {
        assert command != null;
        
        int exitCode;
                        
        if (showOutput) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        
            try {
                exitCode = Process.exec(command, baos);
                dao.updateResult(id, command, "finished", exitCode, 
                                                      baos.toString());
            } catch (Exception ex) {
                exitCode = -1;
                dao.updateResult(id, command, "finished", exitCode, 
                                                       ex.getMessage());
            }                        
        } else {
            try {
                exitCode = Process.exec(command);
            } catch (Exception ex) {
                exitCode = -1;
            }
            dao.updateResult(id, command, "finished", exitCode,  null);            
        }    
    }
    
    private String getIdInfo(final long id) throws Exception {
        final ProcessResult result = dao.retrieveResult(id);
        
        return (result == null) ? "Error: id[" + id + "] not found" 
                                : result.toString();
    }
}
