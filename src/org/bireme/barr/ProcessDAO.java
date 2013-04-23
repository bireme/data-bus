package org.bireme.barr;

/**
 *
 * @author Heitor Barbieri
 * date 20130416
 */
public interface ProcessDAO {

    void close() throws Exception;

    void insertResult(final long id, 
                       final String command, 
                       final String status, 
                       final int exitCode, 
                       final String output) throws Exception;

    ProcessResult retrieveResult(final long id) throws Exception;

    void updateResult(final long id, 
                       final String command, 
                       final String status, 
                       final int exitCode, 
                       final String output) throws Exception;    
}
