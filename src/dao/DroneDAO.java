package dao;

import utils.DatabaseConnection;
import java.sql.*;

public class DroneDAO {

    // ── CREATE ─────────────────────────────────────────────────────────────
    public boolean insertDrone(String droneId, String status, int missionId, int fuelLevel) {
        String sql = "INSERT INTO drones (drone_id, status, mission_id, fuel_level) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, droneId);
            ps.setString(2, status);
            if (missionId > 0) ps.setInt(3, missionId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, fuelLevel);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] insertDrone error: " + e.getMessage());
            return false;
        }
    }

    public void upsertDrone(String droneId, String status, int missionId) {
        String sql = "INSERT INTO drones (drone_id, status, mission_id) VALUES (?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE status=VALUES(status), mission_id=VALUES(mission_id), last_update=NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, droneId);
            ps.setString(2, status);
            ps.setInt(3, missionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] upsertDrone error: " + e.getMessage());
        }
    }

    // ── READ ───────────────────────────────────────────────────────────────
    public ResultSet getAllDrones() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            return st.executeQuery(
                "SELECT id, drone_id, status, mission_id, fuel_level, last_update FROM drones ORDER BY drone_id");
        } catch (SQLException e) {
            System.err.println("[DAO] getAllDrones error: " + e.getMessage());
            return null;
        }
    }

    public ResultSet getFleetStatus() {
        return getAllDrones();
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────
    public boolean updateDrone(int id, String droneId, String status, int missionId, int fuelLevel) {
        String sql = "UPDATE drones SET drone_id=?, status=?, mission_id=?, fuel_level=?, last_update=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, droneId);
            ps.setString(2, status);
            if (missionId > 0) ps.setInt(3, missionId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, fuelLevel);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] updateDrone error: " + e.getMessage());
            return false;
        }
    }

    public void markDestroyed(String droneId) {
        String sql = "UPDATE drones SET status='destroyed', last_update=NOW() WHERE drone_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, droneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] markDestroyed error: " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────
    public boolean deleteDrone(int id) {
        String sql = "DELETE FROM drones WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] deleteDrone error: " + e.getMessage());
            return false;
        }
    }
}