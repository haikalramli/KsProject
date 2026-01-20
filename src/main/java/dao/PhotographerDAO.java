package dao;

import model.Photographer;
import util.DBConnection;
import util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PhotographerDAO {
    
    /**
     * Login validation for photographer
     */
    public Photographer login(String email, String password) {
        String sql = "SELECT * FROM photographer WHERE pgemail = ? AND pgstatus = 'active'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String hashedPassword = rs.getString("pgpass");
                // Verify password using BCrypt
                if (PasswordUtil.verifyPassword(password, hashedPassword)) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all photographers
     */
    public List<Photographer> getAll() {
        List<Photographer> list = new ArrayList<>();
        String sql = "SELECT * FROM photographer ORDER BY pgcreated DESC";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * Get photographer by ID
     */
    public Photographer getById(int pgId) {
        String sql = "SELECT * FROM photographer WHERE pgid = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, pgId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Create new photographer
     */
    public boolean create(Photographer pg) {
        String sql = "INSERT INTO photographer (pgsnr, pgname, pgph, pgemail, pgpass, pgrole, pgstatus) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (pg.getPgSnr() != null) {
                ps.setInt(1, pg.getPgSnr());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, pg.getPgName());
            ps.setString(3, pg.getPgPh());
            ps.setString(4, pg.getPgEmail());
            ps.setString(5, pg.getPgPass());
            ps.setString(6, pg.getPgRole() != null ? pg.getPgRole() : "junior");
            ps.setString(7, pg.getPgStatus() != null ? pg.getPgStatus() : "active");
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update photographer
     */
    public boolean update(Photographer pg) {
        String sql = "UPDATE photographer SET pgname = ?, pgph = ?, pgemail = ?, pgrole = ?, pgstatus = ? WHERE pgid = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, pg.getPgName());
            ps.setString(2, pg.getPgPh());
            ps.setString(3, pg.getPgEmail());
            ps.setString(4, pg.getPgRole());
            ps.setString(5, pg.getPgStatus());
            ps.setInt(6, pg.getPgId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Hard delete photographer from database
     * Only allows deletion of junior and intern photographers
     */
    public boolean delete(int pgId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            // First check if this photographer is a senior - don't allow deleting seniors
            Photographer pg = getById(pgId);
            if (pg != null && "senior".equalsIgnoreCase(pg.getPgRole())) {
                System.out.println("Cannot delete senior photographer");
                return false;
            }
            
            // Update bookings to remove photographer assignment (set to NULL)
            String sqlUpdateBookings = "UPDATE booking SET pgid = NULL WHERE pgid = ?";
            PreparedStatement psBookings = conn.prepareStatement(sqlUpdateBookings);
            psBookings.setInt(1, pgId);
            psBookings.executeUpdate();
            psBookings.close();
            
            // Update packages created by this photographer (set createdby to NULL or a default)
            String sqlUpdatePackages = "UPDATE package SET createdby = NULL WHERE createdby = ?";
            PreparedStatement psPackages = conn.prepareStatement(sqlUpdatePackages);
            psPackages.setInt(1, pgId);
            psPackages.executeUpdate();
            psPackages.close();
            
            // Update junior photographers supervised by this photographer
            String sqlUpdateJuniors = "UPDATE photographer SET pgsnr = NULL WHERE pgsnr = ?";
            PreparedStatement psJuniors = conn.prepareStatement(sqlUpdateJuniors);
            psJuniors.setInt(1, pgId);
            psJuniors.executeUpdate();
            psJuniors.close();
            
            // Now delete the photographer
            String sqlDelete = "DELETE FROM photographer WHERE pgid = ?";
            PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
            psDelete.setInt(1, pgId);
            int result = psDelete.executeUpdate();
            psDelete.close();
            
            conn.commit();
            return result > 0;
            
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { 
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return false;
    }
    
    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM photographer WHERE pgemail = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get count by role
     */
    public int getCountByRole(String role) {
        String sql = "SELECT COUNT(*) FROM photographer WHERE pgrole = ? AND pgstatus = 'active'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Update password
     */
    public boolean updatePassword(int pgId, String newPassword) {
        String sql = "UPDATE photographer SET pgpass = ? WHERE pgid = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Hash password before storing
            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setInt(2, pgId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private Photographer mapResultSet(ResultSet rs) throws SQLException {
        Photographer pg = new Photographer();
        pg.setPgId(rs.getInt("pgid"));
        int snr = rs.getInt("pgsnr");
        pg.setPgSnr(rs.wasNull() ? null : snr);
        pg.setPgName(rs.getString("pgname"));
        pg.setPgPh(rs.getString("pgph"));
        pg.setPgEmail(rs.getString("pgemail"));
        pg.setPgPass(rs.getString("pgpass"));
        pg.setPgRole(rs.getString("pgrole"));
        pg.setPgStatus(rs.getString("pgstatus"));
        pg.setPgCreated(rs.getDate("pgcreated"));
        return pg;
    }
}
