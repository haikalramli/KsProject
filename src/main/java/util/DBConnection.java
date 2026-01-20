package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connection Utility
 * Shared by both Photographer and Client portals
 */
public class DBConnection {
    
    // ============================================================
    // IMPORTANT: Update these to match your PostgreSQL database
    // ============================================================
    private static final String URL = "jdbc:postgresql://localhost:5432/ksapp"; // Default Postgres DB
    private static final String USERNAME = "postgres";  // Your Postgres username
    private static final String PASSWORD = "postgres123";  // Your Postgres password
    
    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL JDBC Driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() {
        try {
            Connection conn = null;
            String dbUrl = System.getenv("JDBC_DATABASE_URL");
            
            if (dbUrl != null && !dbUrl.isEmpty()) {
                // Use Heroku environment variable
                conn = DriverManager.getConnection(dbUrl);
            } else {
                // Fallback to local configuration
                conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
            
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
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
