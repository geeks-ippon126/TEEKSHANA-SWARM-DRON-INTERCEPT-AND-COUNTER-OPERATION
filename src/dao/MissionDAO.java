package dao;

import utils.DatabaseConnection;
import java.sql.*;

public class MissionDAO {

    // ── CREATE ─────────────────────────────────────────────────────────────
    public int createMission(String missionName) {
        String sql = "INSERT INTO missions (mission_name, status) VALUES (?, 'active')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, missionName);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DAO] createMission error: " + e.getMessage());
        }
        return -1;
    }

    // ── READ ───────────────────────────────────────────────────────────────
    public ResultSet getAllMissions() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            return st.executeQuery(
                "SELECT id, mission_name, targets_hit, drones_lost, accuracy, status, start_time, end_time " +
                "FROM missions ORDER BY id DESC");
        } catch (SQLException e) {
            System.err.println("[DAO] getAllMissions error: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getMissionById(int id) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, mission_name, targets_hit, drones_lost, accuracy, status FROM missions WHERE id=?");
            ps.setInt(1, id);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.err.println("[DAO] getMissionById error: " + e.getMessage());
            return null;
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────
    public boolean updateMission(int id, String name, int targetsHit, int dronesLost, double accuracy, String status) {
        String sql = "UPDATE missions SET mission_name=?, targets_hit=?, drones_lost=?, accuracy=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, targetsHit);
            ps.setInt(3, dronesLost);
            ps.setDouble(4, accuracy);
            ps.setString(5, status);
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] updateMission error: " + e.getMessage());
            return false;
        }
    }

    public void completeMission(int missionId, int targetsHit, int dronesLost, double accuracy) {
        String sql = "UPDATE missions SET end_time=NOW(), targets_hit=?, drones_lost=?, accuracy=?, status='completed' WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetsHit);
            ps.setInt(2, dronesLost);
            ps.setDouble(3, accuracy);
            ps.setInt(4, missionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] completeMission error: " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────
    public boolean deleteMission(int id) {
        String sql = "DELETE FROM missions WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] deleteMission error: " + e.getMessage());
            return false;
        }
    }
}