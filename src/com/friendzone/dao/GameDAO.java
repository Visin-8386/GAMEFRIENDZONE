package com.friendzone.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import gamefriendzone.DbConnection;

public class GameDAO {

    public long createSession(long p1, long p2, String gameCode) {
        // CRITICAL: Sort IDs to match DB constraint (user_id1 < user_id2)
        // Although sp_create_game_session might handle it (it uses LEAST/GREATEST), 
        // the prompt explicitly asked to "Sort IDs before calling DB".
        long playerA = Math.min(p1, p2);
        long playerB = Math.max(p1, p2);

        String sql = "{CALL sp_create_game_session(?, ?, ?, ?, ?)}";
        try (Connection conn = DbConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            stmt.setLong(1, playerA);
            stmt.setLong(2, playerB);
            stmt.setString(3, gameCode);
            
            stmt.registerOutParameter(4, Types.BIGINT); // p_session_id
            stmt.registerOutParameter(5, Types.VARCHAR); // p_error
            
            stmt.execute();
            
            String error = stmt.getString(5);
            if (error != null) {
                System.err.println("Error creating session: " + error);
                return -1;
            }
            
            return stmt.getLong(4);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void saveMove(long sessionId, long playerId, String jsonData) {
        String sql = "{CALL sp_save_move(?, ?, ?, ?, ?)}";
        try (Connection conn = DbConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            stmt.setLong(1, sessionId);
            stmt.setLong(2, playerId);
            stmt.setString(3, jsonData); // JSON string
            
            stmt.registerOutParameter(4, Types.INTEGER); // p_move_number
            stmt.registerOutParameter(5, Types.VARCHAR); // p_error
            
            stmt.execute();
            
            String error = stmt.getString(5);
            if (error != null) {
                System.err.println("Error saving move: " + error);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishGame(long sessionId, long winnerId) {
        String sql = "{CALL sp_finish_game(?, ?, ?, ?)}";
        try (Connection conn = DbConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            
            stmt.setLong(1, sessionId);
            stmt.setLong(2, winnerId);
            
            stmt.registerOutParameter(3, Types.BOOLEAN); // p_success
            stmt.registerOutParameter(4, Types.VARCHAR); // p_error
            
            stmt.execute();
            
            String error = stmt.getString(4);
            if (error != null) {
                System.err.println("Error finishing game: " + error);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy 2 người chơi của một session
     * @return long[2] với [0] = user_id1, [1] = user_id2, hoặc null nếu lỗi
     */
    public long[] getSessionPlayers(long sessionId) {
        String sql = "SELECT player1_id, player2_id FROM game_sessions WHERE session_id = ?";
        try (Connection conn = DbConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, sessionId);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new long[] { rs.getLong("player1_id"), rs.getLong("player2_id") };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
