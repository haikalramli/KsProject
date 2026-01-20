package util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Connection Utility with Connection Pooling
 * Uses HikariCP to manage database connections efficiently
 * Shared by both Photographer and Client portals
 */
public class DBConnection {
    
    private static HikariDataSource dataSource;
    
    // Local development settings
    private static final String LOCAL_URL = "jdbc:postgresql://localhost:5432/ksapp";
    private static final String LOCAL_USERNAME = "postgres";
    private static final String LOCAL_PASSWORD = "postgres123";
    
    static {
        try {
            initializeDataSource();
        } catch (Exception e) {
            System.err.println("Failed to initialize connection pool: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        
        String jdbcDbUrl = System.getenv("JDBC_DATABASE_URL");
        String herokuUrl = System.getenv("DATABASE_URL");
        
        if (jdbcDbUrl != null && !jdbcDbUrl.isEmpty()) {
            // Use Heroku JDBC environment variable if available
            config.setJdbcUrl(jdbcDbUrl);
            System.out.println("Using JDBC_DATABASE_URL for connection");
        } else if (herokuUrl != null && !herokuUrl.isEmpty()) {
            // Parse standard Heroku DATABASE_URL: postgres://user:pass@host:port/db
            try {
                URI dbUri = new URI(herokuUrl);
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbJdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?sslmode=require";
                
                config.setJdbcUrl(dbJdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                System.out.println("Using DATABASE_URL for connection");
            } catch (URISyntaxException e) {
                System.err.println("Invalid DATABASE_URL syntax: " + e.getMessage());
                // Fall back to local
                config.setJdbcUrl(LOCAL_URL);
                config.setUsername(LOCAL_USERNAME);
                config.setPassword(LOCAL_PASSWORD);
            }
        } else {
            // Use local configuration
            config.setJdbcUrl(LOCAL_URL);
            config.setUsername(LOCAL_USERNAME);
            config.setPassword(LOCAL_PASSWORD);
            System.out.println("Using local database configuration");
        }
        
        // Connection pool settings - optimized for Heroku's limited connections
        config.setMaximumPoolSize(5);      // Heroku basic plan allows ~20 connections, keep pool small
        config.setMinimumIdle(2);           // Minimum idle connections
        config.setIdleTimeout(300000);      // 5 minutes idle timeout
        config.setConnectionTimeout(20000); // 20 seconds to get a connection
        config.setMaxLifetime(600000);      // 10 minutes max lifetime
        config.setLeakDetectionThreshold(60000); // Detect connection leaks after 60 seconds
        
        // PostgreSQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
        System.out.println("HikariCP Connection Pool initialized successfully");
    }
    
    public static Connection getConnection() {
        try {
            if (dataSource == null) {
                initializeDataSource();
            }
            Connection conn = dataSource.getConnection();
            if (conn != null) {
                conn.setAutoCommit(true);
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return null;
        }
    }
    
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try { 
                conn.close(); // Returns connection to pool
            } catch (SQLException e) { 
                e.printStackTrace(); 
            }
        }
    }
    
    /**
     * Shutdown the connection pool (call on application shutdown)
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP Connection Pool shutdown");
        }
    }
}
