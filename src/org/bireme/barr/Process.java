package org.bireme.barr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 *
 * @author Heitor Barbieri
 * date 20130411
 */
public class Process {
    public static final int TIMEOUT = 15 * 60000; // 15 minutes
    
    public static int exec(final String cmd) throws ExecuteException, 
                                                    IOException {
        return exec(cmd, TIMEOUT, null);
    }
    
    public static int exec(final String cmd,
                           final int timeout) throws ExecuteException, 
                                                     IOException {
        return exec(cmd, timeout, null);
    }
    
    public static int exec(final String cmd,
                           final OutputStream os) throws ExecuteException, 
                                                         IOException {
        return exec(cmd, TIMEOUT, os);
    }
    
    public static int exec(final String cmd,
                           final int timeout,
                           final OutputStream os) throws ExecuteException, 
                                                         IOException {
        if (cmd == null) {
            throw new NullPointerException("cmd");
        }
        
        final CommandLine cmdLine = CommandLine.parse(cmd);
        final DefaultExecutor executor = new DefaultExecutor();        
        final int exitValue;
        final OutputStream outs = (os == null) ? new NullOutputStream() : os;
        
        //executor.setExitValue(1);
        if (timeout > 0) {
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
            executor.setWatchdog(watchdog);
        }
        executor.setStreamHandler(new PumpStreamHandler(outs));
        exitValue = executor.execute(cmdLine);
        outs.close();
        
        return exitValue;
    }
    
    public static void main(final String[] args) throws ExecuteException, 
                                                        IOException {
        final StringBuilder builder = new StringBuilder();
        
        for (String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        final String cmd = builder.toString();        
        System.out.println("executing: " + cmd);
        final int exitCode = Process.exec(cmd, baos);
        System.out.println("exit code = " + exitCode);
        
        System.out.println("### " + baos.toString());
    }
}

class NullOutputStream extends OutputStream {
    @Override
    public void write(int i) throws IOException {
        // do nothing
    }    
}
