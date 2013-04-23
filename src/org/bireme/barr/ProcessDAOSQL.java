package org.bireme.barr;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Heitor Barbieri
 * date 20130411
 */
public class ProcessDAOSQL implements ProcessDAO {
    
    public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    static final int POOL_SIZE = 30;
    
    final String dbName;
    final String tableName;
    final String connectionURL;
    final ArrayBlockingQueue<Connection> connections;
    
    public ProcessDAOSQL(final String dbName,
                          final String tableName) throws ClassNotFoundException, 
                                                      SQLException,
                                                      InterruptedException {
        if (dbName == null) {
            throw new NullPointerException("data base name");
        }
        if (tableName == null) {
            throw new NullPointerException("table name");
        }
        Class.forName(DRIVER);
        this.dbName = dbName.toUpperCase();
        this.tableName = tableName.toUpperCase();
        this.connectionURL = "jdbc:derby:" + dbName + ";create=true";
        this.connections = new ArrayBlockingQueue<>(POOL_SIZE);
        
        createConnections();
        createTable();
    }
        
    @Override
    public void close() throws SQLException {
        for (Connection conn : connections) {
            conn.close();
        }
        // ????
    }
    
    private void createConnections() throws SQLException {
        for (int counter = 0; counter < POOL_SIZE; counter++) {
            final Connection conn = DriverManager.getConnection(connectionURL);
            connections.add(conn);
        }
    }
    
    private Connection getConnection() throws InterruptedException {
        return connections.take();
    }
    
    private void releaseConnection(final Connection conn) 
                                                  throws InterruptedException {
        assert conn != null;
        
        connections.put(conn);
    }
    
    private void createTable() throws SQLException, InterruptedException {
        final Connection conn = getConnection();
        final DatabaseMetaData dbm = conn.getMetaData();
        final ResultSet tables = dbm.getTables(null, null, tableName, null);
        
        if (tables.next()) {
            // Table exists
            /*try (Statement stmt = conn.createStatement()) {
                final String sql = "DROP TABLE " + tableName;

                stmt.executeUpdate(sql);
            }*/
            
        } else {
            try (Statement stmt = conn.createStatement()) {
                final String sql = "CREATE TABLE " + 
                    tableName +
                    "(id BIGINT NOT NULL, " +
                    " command VARCHAR(255) NOT NULL, " +
                    " status VARCHAR(16) NOT NULL, " +
                    " exitcode SMALLINT, " + 
                    " coutput VARCHAR(255), " + 
                    " PRIMARY KEY ( id ))"; 

                stmt.executeUpdate(sql);
            }
        }        
        releaseConnection(conn);        
    }
    
    @Override
    public void insertResult(final long id,
                              final String command,
                              final String status,
                              final int exitCode,
                              final String output) throws SQLException, 
                                                          InterruptedException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (status == null) {
            throw new NullPointerException("status");
        }
        
        final String out = (output == null) ? "" 
                          : output.substring(0, Math.min(255, output.length()));
        final Connection conn = getConnection();
        final String sql = "INSERT INTO " + tableName + " VALUES (" + id + ", '"
            + command + "', '" + status + "', " + exitCode + ", '" + out + "')";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        releaseConnection(conn);  
    }
    
    @Override
    public void updateResult(final long id,
                              final String command,
                              final String status,
                              final int exitCode,
                              final String output) throws SQLException, 
                                                          InterruptedException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (status == null) {
            throw new NullPointerException("status");
        }
        
        final String out = (output == null) ? "" 
                          : output.substring(0, Math.min(255, output.length()));
        final Connection conn = getConnection();
        final String sql = "UPDATE " + tableName + " SET command='" + command + 
            "', status='" + status + "', exitcode=" + exitCode
            + ", coutput='" + out + "' WHERE id=" + id;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        releaseConnection(conn);  
    }
    
    @Override
    public ProcessResult retrieveResult(final long id) throws SQLException, 
                                                          InterruptedException {
        final ProcessResult ret;
        
        final String query = "SELECT * FROM "  + tableName + " where id=" + id;
        final Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                ret = new ProcessResult(rs.getLong("id"),
                                        rs.getString("command"),
                                        rs.getString("status"),
                                        rs.getInt("exitcode"),
                                        rs.getString("coutput"));
            } else {
                ret = null;
            }
        }
        releaseConnection(conn);
        
        return ret;
    } 
}
