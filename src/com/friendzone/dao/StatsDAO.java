package com.friendzone.dao;

import com.friendzone.model.User;
import gamefriendzone.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatsDAO {
    
    /**
     * Get top players for a specific game
     */
    public List<User> getTopPlayers(String gameCode, int limit) {
        List<User> topPlayers = new ArrayList<>();
        String sql = "SELECT u.user_id, u.nickname, u.elo_rating, s.wins, s.losses, s.draws, s.longest_win_streak " +
                     "FROM user_game_stats s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN games g ON s.game_id = g.game_id " +
                     "WHERE g.game_code = ? " +
                     "ORDER BY s.wins DESC, u.elo_rating DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, gameCode);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("user_id"));
                u.setNickname(rs.getString("nickname"));
                u.setElo(rs.getInt("elo_rating"));
                u.setMoney(0.0); // Not used in leaderboard
                topPlayers.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return topPlayers;
    }
    
    /**
     * Get user stats for a specific game
     */
    public java.util.Map<String, Integer> getUserStats(long userId, String gameCode) {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT s.total_matches, s.wins, s.losses, s.draws, s.longest_win_streak " +
                     "FROM user_game_stats s " +
                     "JOIN games g ON s.game_id = g.game_id " +
                     "WHERE s.user_id = ? AND g.game_code = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, gameCode);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                stats.put("total_matches", rs.getInt("total_matches"));
                stats.put("wins", rs.getInt("wins"));
                stats.put("losses", rs.getInt("losses"));
                stats.put("draws", rs.getInt("draws"));
                stats.put("longest_win_streak", rs.getInt("longest_win_streak"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Update high score for single player games (like Catch Heart)
     * Note: Since user_game_stats doesn't have high_score column,
     * we use 'wins' to track the score for single player games
     */
    public boolean updateHighScore(long userId, String gameCode, int score) {
        // First, get the game_id from gameCode
        String getGameIdSql = "SELECT game_id FROM games WHERE game_code = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement getStmt = conn.prepareStatement(getGameIdSql)) {
            getStmt.setString(1, gameCode);
            ResultSet rs = getStmt.executeQuery();
            
            if (rs.next()) {
                int gameId = rs.getInt("game_id");
                
                // Use INSERT ... ON DUPLICATE KEY UPDATE
                // Store score in 'wins' column for single-player games (as high score tracker)
                String upsertSql = "INSERT INTO user_game_stats (user_id, game_id, total_matches, wins, last_played) " +
                                   "VALUES (?, ?, 1, ?, NOW()) " +
                                   "ON DUPLICATE KEY UPDATE total_matches = total_matches + 1, " +
                                   "wins = GREATEST(wins, VALUES(wins)), last_played = NOW()";
                
                try (PreparedStatement upsertStmt = conn.prepareStatement(upsertSql)) {
                    upsertStmt.setLong(1, userId);
                    upsertStmt.setInt(2, gameId);
                    upsertStmt.setInt(3, score);
                    upsertStmt.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get high score for a specific game and user
     * For single-player games, 'wins' column stores the high score
     */
    public int getHighScore(long userId, String gameCode) {
        String sql = "SELECT s.wins FROM user_game_stats s " +
                     "JOIN games g ON s.game_id = g.game_id " +
                     "WHERE s.user_id = ? AND g.game_code = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, gameCode);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("wins");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
