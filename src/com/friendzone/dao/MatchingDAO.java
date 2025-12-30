package com.friendzone.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gamefriendzone.DbConnection;

/**
 * DAO để quản lý hệ thống matching và compatibility
 */
public class MatchingDAO {
    
    /**
     * Like/Pass một user
     */
    public boolean likeUser(long likerId, long likedId, String likeType) {
        String sql = "INSERT INTO user_likes (liker_id, liked_id, like_type) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE like_type = ?, created_at = CURRENT_TIMESTAMP";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, likerId);
            ps.setLong(2, likedId);
            ps.setString(3, likeType);
            ps.setString(4, likeType);
            ps.executeUpdate();
            
            // Kiểm tra nếu match (cả 2 đều like nhau)
            if ("LIKE".equals(likeType) || "SUPER_LIKE".equals(likeType)) {
                checkAndCreateMatch(likerId, likedId);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra và tạo match nếu cả 2 đều thích nhau
     */
    public Long checkAndCreateMatch(long user1, long user2) {
        String checkSql = """
            SELECT 1 FROM user_likes 
            WHERE liker_id = ? AND liked_id = ? AND like_type IN ('LIKE', 'SUPER_LIKE')
        """;
        
        try (Connection conn = DbConnection.getConnection()) {
            // Kiểm tra user2 đã like user1 chưa
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setLong(1, user2);
                ps.setLong(2, user1);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null; // Chưa match
                }
            }
            
            // Tạo match
            long u1 = Math.min(user1, user2);
            long u2 = Math.max(user1, user2);
            
            String insertSql = "INSERT IGNORE INTO matches (user_id1, user_id2) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, u1);
                ps.setLong(2, u2);
                ps.executeUpdate();
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            
            // Trả về match_id nếu đã tồn tại
            String getMatchSql = "SELECT match_id FROM matches WHERE user_id1 = ? AND user_id2 = ?";
            try (PreparedStatement ps = conn.prepareStatement(getMatchSql)) {
                ps.setLong(1, u1);
                ps.setLong(2, u2);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong("match_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Lấy danh sách người để discover (chưa like/pass, phù hợp tiêu chí)
     */
    public List<Map<String, Object>> getDiscoverProfiles(long userId, int limit) {
        List<Map<String, Object>> profiles = new ArrayList<>();
        
        String sql = """
            SELECT u.user_id, u.nickname, u.avatar_url, u.gender, u.elo_rating,
                   p.bio, p.birth_date, p.location, p.occupation, p.looking_for,
                   TIMESTAMPDIFF(YEAR, p.birth_date, CURDATE()) as age,
                   (SELECT COUNT(*) FROM user_interests ui1 
                    JOIN user_interests ui2 ON ui1.interest_id = ui2.interest_id
                    WHERE ui1.user_id = u.user_id AND ui2.user_id = ?) as common_interests
            FROM users u
            LEFT JOIN user_profiles p ON u.user_id = p.user_id
            WHERE u.user_id != ?
              AND u.status != 'BANNED'
              AND u.deleted_at IS NULL
              AND u.user_id NOT IN (
                  SELECT liked_id FROM user_likes WHERE liker_id = ?
              )
              AND u.user_id NOT IN (
                  SELECT user_id1 FROM matches WHERE user_id2 = ? AND status = 'ACTIVE'
                  UNION
                  SELECT user_id2 FROM matches WHERE user_id1 = ? AND status = 'ACTIVE'
              )
            ORDER BY common_interests DESC, u.elo_rating DESC
            LIMIT ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);
            ps.setLong(5, userId);
            ps.setInt(6, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("userId", rs.getLong("user_id"));
                    profile.put("nickname", rs.getString("nickname"));
                    profile.put("avatarUrl", rs.getString("avatar_url"));
                    profile.put("gender", rs.getString("gender"));
                    profile.put("eloRating", rs.getInt("elo_rating"));
                    profile.put("bio", rs.getString("bio"));
                    profile.put("age", rs.getInt("age"));
                    profile.put("location", rs.getString("location"));
                    profile.put("occupation", rs.getString("occupation"));
                    profile.put("lookingFor", rs.getString("looking_for"));
                    profile.put("commonInterests", rs.getInt("common_interests"));
                    
                    // Lấy interests
                    profile.put("interests", getTopInterests(rs.getLong("user_id"), 5));
                    
                    profiles.add(profile);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return profiles;
    }
    
    /**
     * Lấy top N sở thích của user
     */
    private List<String> getTopInterests(long userId, int limit) {
        List<String> interests = new ArrayList<>();
        String sql = """
            SELECT i.interest_name 
            FROM user_interests ui
            JOIN interests i ON ui.interest_id = i.interest_id
            WHERE ui.user_id = ?
            LIMIT ?
        """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    interests.add(rs.getString("interest_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return interests;
    }
    
    /**
     * Lấy danh sách matches của user
     */
    public List<Map<String, Object>> getMatches(long userId) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        String sql = """
            SELECT m.match_id, m.matched_at, m.games_played, m.total_compatibility, m.last_interaction,
                   u.user_id, u.nickname, u.avatar_url, u.gender, u.status,
                   gis.overall_compatibility, gis.total_games, gis.avg_chemistry_score, 
                   gis.avg_fun_score, gis.avg_communication_score
            FROM matches m
            JOIN users u ON (u.user_id = IF(m.user_id1 = ?, m.user_id2, m.user_id1))
            LEFT JOIN game_interaction_stats gis ON 
                (gis.user_id1 = LEAST(?, u.user_id) AND gis.user_id2 = GREATEST(?, u.user_id))
            WHERE (m.user_id1 = ? OR m.user_id2 = ?)
              AND m.status = 'ACTIVE'
            ORDER BY m.last_interaction DESC, m.matched_at DESC
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);
            ps.setLong(5, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> match = new HashMap<>();
                    match.put("matchId", rs.getLong("match_id"));
                    match.put("matchedAt", rs.getTimestamp("matched_at"));
                    match.put("gamesPlayed", rs.getInt("games_played"));
                    match.put("totalCompatibility", rs.getDouble("total_compatibility"));
                    match.put("lastInteraction", rs.getTimestamp("last_interaction"));
                    
                    // Partner info
                    match.put("partnerId", rs.getLong("user_id"));
                    match.put("partnerNickname", rs.getString("nickname"));
                    match.put("partnerAvatar", rs.getString("avatar_url"));
                    match.put("partnerGender", rs.getString("gender"));
                    match.put("partnerStatus", rs.getString("status"));
                    
                    // Compatibility scores
                    match.put("overallCompatibility", rs.getDouble("overall_compatibility"));
                    match.put("totalGames", rs.getInt("total_games"));
                    match.put("chemistryScore", rs.getDouble("avg_chemistry_score"));
                    match.put("funScore", rs.getDouble("avg_fun_score"));
                    match.put("communicationScore", rs.getDouble("avg_communication_score"));
                    
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matches;
    }
    
    /**
     * Lấy điểm tương thích chi tiết giữa 2 người
     */
    public Map<String, Object> getCompatibilityDetails(long user1, long user2) {
        Map<String, Object> result = new HashMap<>();
        
        long u1 = Math.min(user1, user2);
        long u2 = Math.max(user1, user2);
        
        // Lấy thống kê tổng
        String statsSql = """
            SELECT * FROM game_interaction_stats 
            WHERE user_id1 = ? AND user_id2 = ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(statsSql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("totalGames", rs.getInt("total_games"));
                    result.put("totalTimeTogether", rs.getInt("total_time_together_minutes"));
                    result.put("gamesWonUser1", rs.getInt("games_won_user1"));
                    result.put("gamesWonUser2", rs.getInt("games_won_user2"));
                    result.put("gamesDraw", rs.getInt("games_draw"));
                    result.put("avgChemistry", rs.getDouble("avg_chemistry_score"));
                    result.put("avgFun", rs.getDouble("avg_fun_score"));
                    result.put("avgCommunication", rs.getDouble("avg_communication_score"));
                    result.put("overallCompatibility", rs.getDouble("overall_compatibility"));
                    result.put("firstPlayed", rs.getTimestamp("first_played"));
                    result.put("lastPlayed", rs.getTimestamp("last_played"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Lấy lịch sử các game
        String historySql = """
            SELECT cs.*, g.display_name as game_name
            FROM compatibility_scores cs
            JOIN games g ON cs.game_code = g.game_code
            WHERE cs.user_id1 = ? AND cs.user_id2 = ?
            ORDER BY cs.calculated_at DESC
            LIMIT 10
        """;
        
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(historySql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> game = new HashMap<>();
                    game.put("gameName", rs.getString("game_name"));
                    game.put("chemistry", rs.getInt("chemistry_score"));
                    game.put("fun", rs.getInt("fun_score"));
                    game.put("communication", rs.getInt("communication_score"));
                    game.put("sportsmanship", rs.getInt("sportsmanship_score"));
                    game.put("winnerId", rs.getLong("winner_id"));
                    game.put("duration", rs.getInt("game_duration_seconds"));
                    game.put("closeMatch", rs.getBoolean("close_match"));
                    game.put("playedAt", rs.getTimestamp("calculated_at"));
                    history.add(game);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        result.put("gameHistory", history);
        
        // Tính số sở thích chung
        String commonInterestsSql = """
            SELECT COUNT(*) as common_count,
                   GROUP_CONCAT(i.interest_name SEPARATOR ', ') as interests
            FROM user_interests ui1
            JOIN user_interests ui2 ON ui1.interest_id = ui2.interest_id
            JOIN interests i ON ui1.interest_id = i.interest_id
            WHERE ui1.user_id = ? AND ui2.user_id = ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(commonInterestsSql)) {
            ps.setLong(1, user1);
            ps.setLong(2, user2);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("commonInterestsCount", rs.getInt("common_count"));
                    result.put("commonInterests", rs.getString("interests"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Tính điểm tương thích sau mỗi game
     */
    public double calculateCompatibilityAfterGame(long sessionId) {
        try (Connection conn = DbConnection.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_calculate_compatibility(?, ?)}")) {
            cs.setLong(1, sessionId);
            cs.registerOutParameter(2, Types.DECIMAL);
            cs.execute();
            return cs.getDouble(2);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Lưu đánh giá sau game
     */
    public boolean saveGameRating(long sessionId, long userId, int rating, boolean wouldPlayAgain) {
        // Xác định user1 hay user2
        String sql = """
            UPDATE compatibility_scores 
            SET user1_rating = IF(user_id1 = ?, ?, user1_rating),
                user2_rating = IF(user_id2 = ?, ?, user2_rating),
                user1_would_play_again = IF(user_id1 = ?, ?, user1_would_play_again),
                user2_would_play_again = IF(user_id2 = ?, ?, user2_would_play_again)
            WHERE game_session_id = ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, rating);
            ps.setLong(3, userId);
            ps.setInt(4, rating);
            ps.setLong(5, userId);
            ps.setBoolean(6, wouldPlayAgain);
            ps.setLong(7, userId);
            ps.setBoolean(8, wouldPlayAgain);
            ps.setLong(9, sessionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Unmatch
     */
    public boolean unmatch(long userId, long partnerId) {
        long u1 = Math.min(userId, partnerId);
        long u2 = Math.max(userId, partnerId);
        
        String sql = "UPDATE matches SET status = 'UNMATCHED' WHERE user_id1 = ? AND user_id2 = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra xem đã match chưa
     */
    public boolean isMatched(long user1, long user2) {
        long u1 = Math.min(user1, user2);
        long u2 = Math.max(user1, user2);
        
        String sql = "SELECT 1 FROM matches WHERE user_id1 = ? AND user_id2 = ? AND status = 'ACTIVE'";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy những người đã like mình (mà mình chưa like lại)
     */
    public List<Map<String, Object>> getWhoLikedMe(long userId) {
        List<Map<String, Object>> likers = new ArrayList<>();
        
        String sql = """
            SELECT ul.liker_id, ul.like_type, ul.created_at,
                   u.nickname, u.avatar_url, u.gender
            FROM user_likes ul
            JOIN users u ON ul.liker_id = u.user_id
            WHERE ul.liked_id = ?
              AND ul.like_type IN ('LIKE', 'SUPER_LIKE')
              AND ul.liker_id NOT IN (
                  SELECT liked_id FROM user_likes WHERE liker_id = ?
              )
            ORDER BY ul.created_at DESC
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> liker = new HashMap<>();
                    liker.put("userId", rs.getLong("liker_id"));
                    liker.put("likeType", rs.getString("like_type"));
                    liker.put("likedAt", rs.getTimestamp("created_at"));
                    liker.put("nickname", rs.getString("nickname"));
                    liker.put("avatarUrl", rs.getString("avatar_url"));
                    liker.put("gender", rs.getString("gender"));
                    likers.add(liker);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return likers;
    }
    
    // =====================================================
    // NEW SIMPLIFIED METHODS FOR DATING FEATURES
    // =====================================================
    
    /**
     * Ghi nhận swipe (LIKE, PASS, SUPER_LIKE)
     */
    public boolean recordSwipe(long userId, long targetId, String action) {
        // Sử dụng bảng user_swipes thay vì user_likes
        String sql = """
            INSERT INTO user_swipes (user_id, target_id, action) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE action = ?, swiped_at = CURRENT_TIMESTAMP
        """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, targetId);
            ps.setString(3, action);
            ps.setString(4, action);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra xem 2 người đã like lẫn nhau chưa (mutual like = match)
     */
    public Map<String, Object> checkMutualLike(long user1, long user2) {
        String checkSql = """
            SELECT s1.action as action1, s2.action as action2
            FROM user_swipes s1
            JOIN user_swipes s2 ON s1.user_id = s2.target_id AND s1.target_id = s2.user_id
            WHERE s1.user_id = ? AND s1.target_id = ?
              AND s1.action IN ('LIKE', 'SUPER_LIKE')
              AND s2.action IN ('LIKE', 'SUPER_LIKE')
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, user1);
            ps.setLong(2, user2);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Có match! Tạo record trong matches
                    long matchId = createMatch(user1, user2, 
                        "SUPER_LIKE".equals(rs.getString("action1")) || "SUPER_LIKE".equals(rs.getString("action2")));
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("matchId", matchId);
                    result.put("isSuperLike", "SUPER_LIKE".equals(rs.getString("action1")));
                    return result;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Tạo match record
     */
    private long createMatch(long user1, long user2, boolean isSuperLike) {
        String sql = """
            INSERT INTO matches (user_id1, user_id2, status) 
            VALUES (?, ?, 'ACTIVE')
            ON DUPLICATE KEY UPDATE matched_at = matched_at
        """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, Math.min(user1, user2));
            ps.setLong(2, Math.max(user1, user2));
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * Lấy danh sách users để discover (chưa swipe)
     */
    public List<Map<String, Object>> getDiscoverUsers(long userId, int limit) {
        List<Map<String, Object>> users = new ArrayList<>();
        
        String sql = """
            SELECT u.user_id, u.nickname, u.gender, u.bio, u.birth_date, u.location
            FROM users u
            WHERE u.user_id != ?
              AND u.status != 'BANNED'
              AND u.deleted_at IS NULL
            ORDER BY RAND()
            LIMIT ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", rs.getLong("user_id"));
                    user.put("username", rs.getString("nickname"));
                    user.put("gender", rs.getString("gender"));
                    user.put("bio", rs.getString("bio") != null ? rs.getString("bio") : "");
                    
                    // Calculate age from birth_date
                    java.sql.Date birthDate = rs.getDate("birth_date");
                    int age = 0;
                    if (birthDate != null) {
                        java.time.LocalDate birth = birthDate.toLocalDate();
                        age = java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
                    }
                    user.put("age", age);
                    user.put("location", rs.getString("location") != null ? rs.getString("location") : "");
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    /**
     * Tính điểm tương thích giữa 2 người dựa trên interests và game history
     */
    public int calculateCompatibility(long user1, long user2) {
        int interestScore = 0;
        int gameScore = 0;
        
        // Tính điểm sở thích chung (dùng interest_id thay vì interest)
        String interestSql = """
            SELECT COUNT(*) as common_count FROM user_interests ui1
            JOIN user_interests ui2 ON ui1.interest_id = ui2.interest_id
            WHERE ui1.user_id = ? AND ui2.user_id = ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(interestSql)) {
            ps.setLong(1, user1);
            ps.setLong(2, user2);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int common = rs.getInt("common_count");
                    interestScore = Math.min(common * 10, 50); // Max 50 từ interests
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Tính điểm từ game đã chơi cùng nhau
        String gameSql = """
            SELECT game_points FROM compatibility_scores 
            WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(gameSql)) {
            ps.setLong(1, Math.min(user1, user2));
            ps.setLong(2, Math.max(user1, user2));
            ps.setLong(3, Math.min(user1, user2));
            ps.setLong(4, Math.max(user1, user2));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    gameScore = rs.getInt("game_points");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Tổng điểm (interest + game) / 2, max 100
        return Math.min(interestScore + gameScore, 100);
    }
    
    /**
     * Cập nhật điểm tương thích sau mỗi game
     */
    public void updateCompatibilityAfterGame(long p1, long p2, String gameType, long winnerId) {
        long u1 = Math.min(p1, p2);
        long u2 = Math.max(p1, p2);
        
        // Tính điểm game mới
        int gamePoints = 0;
        
        // Lấy điểm cũ
        String getSql = """
            SELECT game_points, games_played FROM compatibility_scores 
            WHERE user1_id = ? AND user2_id = ?
        """;
        
        int currentPoints = 0;
        int gamesPlayed = 0;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(getSql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentPoints = rs.getInt("game_points");
                    gamesPlayed = rs.getInt("games_played");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Tính điểm mới dựa trên loại game
        switch (gameType) {
            case "LOVE_QUIZ":
                gamePoints = 10; // Quiz tình yêu cho điểm cao nhất
                break;
            case "DRAW_GUESS":
                gamePoints = 8; // Vẽ đoán cũng hay
                break;
            case "WORD_CHAIN":
                gamePoints = 5;
                break;
            case "CARO":
            case "CATCH_HEART":
                gamePoints = 3;
                break;
            default:
                gamePoints = 2;
        }
        
        int newPoints = Math.min(currentPoints + gamePoints, 50); // Max 50 từ games
        gamesPlayed++;
        
        // Cập nhật hoặc insert
        String upsertSql = """
            INSERT INTO compatibility_scores (user1_id, user2_id, game_points, games_played, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE 
                game_points = ?, 
                games_played = ?,
                updated_at = CURRENT_TIMESTAMP
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(upsertSql)) {
            ps.setLong(1, u1);
            ps.setLong(2, u2);
            ps.setInt(3, newPoints);
            ps.setInt(4, gamesPlayed);
            ps.setInt(5, newPoints);
            ps.setInt(6, gamesPlayed);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy danh sách matches của user (sử dụng bảng user_matches)
     */
    public List<Map<String, Object>> getUserMatches(long userId) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        String sql = """
            SELECT m.match_id, m.matched_at,
                   u.user_id, u.nickname
            FROM matches m
            JOIN users u ON u.user_id = IF(m.user_id1 = ?, m.user_id2, m.user_id1)
            WHERE (m.user_id1 = ? OR m.user_id2 = ?)
            ORDER BY m.matched_at DESC
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> match = new HashMap<>();
                    match.put("matchId", rs.getLong("match_id"));
                    match.put("matchedAt", rs.getTimestamp("matched_at").toString());
                    match.put("isSuperLike", false); // Không còn cột is_super_like
                    match.put("partnerId", rs.getLong("user_id"));
                    match.put("username", rs.getString("nickname"));
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matches;
    }
    
    /**
     * Lấy danh sách người đã tương tác (chơi game) với compatibility scores
     */
    public List<Map<String, Object>> getCompatibilityList(long userId) {
        List<Map<String, Object>> compatList = new ArrayList<>();
        
        String sql = """
            SELECT cs.*, u.nickname,
                   (SELECT COUNT(*) FROM user_interests ui1 
                    JOIN user_interests ui2 ON ui1.interest_id = ui2.interest_id
                    WHERE ui1.user_id = ? AND ui2.user_id = u.user_id) * 10 as interest_points
            FROM compatibility_scores cs
            JOIN users u ON u.user_id = IF(cs.user1_id = ?, cs.user2_id, cs.user1_id)
            WHERE cs.user1_id = ? OR cs.user2_id = ?
            ORDER BY (cs.game_points + 
                     (SELECT COUNT(*) FROM user_interests ui1 
                      JOIN user_interests ui2 ON ui1.interest_id = ui2.interest_id
                      WHERE ui1.user_id = ? AND ui2.user_id = u.user_id) * 10) DESC
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);
            ps.setLong(5, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> compat = new HashMap<>();
                    long partnerId = rs.getLong("user1_id") == userId ? 
                                     rs.getLong("user2_id") : rs.getLong("user1_id");
                    compat.put("partnerId", partnerId);
                    compat.put("userId", partnerId);
                    compat.put("username", rs.getString("nickname"));
                    compat.put("gamePoints", rs.getInt("game_points"));
                    compat.put("gamesPlayed", rs.getInt("games_played"));
                    compat.put("interestPoints", Math.min(rs.getInt("interest_points"), 50));
                    compat.put("totalScore", Math.min(rs.getInt("game_points") + rs.getInt("interest_points"), 100));
                    compatList.add(compat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return compatList;
    }
}
