package org.bireme.barr;

/**
 *
 * @author Heitor Barbieri
 * date 20130417
 */
public class ProcessResult {
    final long id;
    final String command;
    final String status;
    final int exitCode;
    final String output;

    ProcessResult(final long id, 
                   final String command, 
                   final String status, 
                   final int exitCode, 
                   final String output) {
        this.id = id;
        this.command = command;
        this.status = status;
        this.exitCode = exitCode;
        this.output = output;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("id:");
        builder.append(id);
        builder.append("\ncommand:");
        builder.append(command);
        builder.append("\nstatus:");
        builder.append(status);
        builder.append("\nexitcode:");
        builder.append(exitCode);
        builder.append("\noutput:");
        builder.append(output);
        builder.append("\n");
        return builder.toString();
    }    
}
