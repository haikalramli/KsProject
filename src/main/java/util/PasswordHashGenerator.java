package util;

/**
 * Utility to generate BCrypt hashes for test passwords
 * Run this to get hashed passwords for database migration
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        System.out.println("Generating BCrypt hashes for test passwords...\n");
        
        // Hash for admin123
        String adminHash = PasswordUtil.hashPassword("admin123");
        System.out.println("admin123 -> " + adminHash);
        
        // Hash for junior123
        String juniorHash = PasswordUtil.hashPassword("junior123");
        System.out.println("junior123 -> " + juniorHash);
        
        // Hash for intern123
        String internHash = PasswordUtil.hashPassword("intern123");
        System.out.println("intern123 -> " + internHash);
        
        // Hash for client123
        String clientHash = PasswordUtil.hashPassword("client123");
        System.out.println("client123 -> " + clientHash);
        
        System.out.println("\nUse these in your database UPDATE statements:");
        System.out.println("\nUPDATE photographer SET pgpass = '" + adminHash + "' WHERE pgemail = 'senior@ksstudio.com';");
        System.out.println("UPDATE photographer SET pgpass = '" + juniorHash + "' WHERE pgemail = 'junior@ksstudio.com';");
        System.out.println("UPDATE photographer SET pgpass = '" + internHash + "' WHERE pgemail = 'intern@ksstudio.com';");
        System.out.println("\nFor clients:");
        System.out.println("UPDATE client SET clpass = '" + clientHash + "' WHERE clemail = 'ali@email.com';");
    }
}
