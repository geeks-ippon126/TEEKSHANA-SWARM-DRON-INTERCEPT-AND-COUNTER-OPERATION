package dao;

import utils.DatabaseConnection;
import java.sql.*;

public class ReinforcementDAO {

    // ── CREATE ─────────────────────────────────────────────────────────────
    public int requestDrones(int missionId, int requestedCount) {
        String sql = "INSERT INTO reinforcements (mission_id, requested_drones, status) VALUES (?, ?, 'pending')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, missionId);
            ps.setInt(2, requestedCount);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DAO] requestDrones error: " + e.getMessage());
        }
        return -1;
    }

    // ── READ ───────────────────────────────────────────────────────────────
    public ResultSet getAllReinforcements() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            return st.executeQuery(
                "SELECT id, mission_id, requested_drones, approved_drones, status, request_time " +
                "FROM reinforcements ORDER BY id DESC");
        } catch (SQLException e) {
            System.err.println("[DAO] getAllReinforcements error: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getReinforcementsForMission(int missionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, requested_drones, approved_drones, status, request_time " +
                "FROM reinforcements WHERE mission_id=?");
            ps.setInt(1, missionId);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.err.println("[DAO] getReinforcementsForMission error: " + e.getMessage());
            return null;
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────
    public boolean updateReinforcement(int id, int missionId, int requested, int approved, String status) {
        String sql = "UPDATE reinforcements SET mission_id=?, requested_drones=?, approved_drones=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, missionId);
            ps.setInt(2, requested);
            ps.setInt(3, approved);
            ps.setString(4, status);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] updateReinforcement error: " + e.getMessage());
            return false;
        }
    }

    public void approveReinforcement(int reinforcementId, int approvedCount) {
        String sql = "UPDATE reinforcements SET approved_drones=?, status='approved' WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approvedCount);
            ps.setInt(2, reinforcementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] approveReinforcement error: " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────
    public boolean deleteReinforcement(int id) {
        String sql = "DELETE FROM reinforcements WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] deleteReinforcement error: " + e.getMessage());
            return false;
        }
    }
}