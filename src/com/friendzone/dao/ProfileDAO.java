package com.friendzone.dao;

import gamefriendzone.DbConnection;
import java.sql.*;
import java.util.*;

/**
 * DAO để quản lý profile và sở thích người dùng
 * Profile đã được gộp vào bảng users (không cần bảng user_profiles riêng)
 */
public class ProfileDAO {
    
    /**
     * Lấy profile đầy đủ của user (trực tiếp từ bảng users)
     */
    public Map<String, Object> getProfile(long userId) {
        Map<String, Object> profile = new HashMap<>();
        
        String sql = """
            SELECT user_id, username, nickname, avatar_url, gender, elo_rating, status, created_at,
                   bio, birth_date, location, occupation, education,
                   looking_for, age_min, age_max, preferred_gender, is_verified, profile_complete_percent
            FROM users
            WHERE user_id = ? AND deleted_at IS NULL
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile.put("userId", rs.getLong("user_id"));
                    profile.put("username", rs.getString("username"));
                    profile.put("nickname", rs.getString("nickname"));
                    profile.put("avatarUrl", rs.getString("avatar_url"));
                    profile.put("gender", rs.getString("gender"));
                    profile.put("eloRating", rs.getInt("elo_rating"));
                    profile.put("status", rs.getString("status"));
                    profile.put("createdAt", rs.getTimestamp("created_at"));
                    profile.put("bio", rs.getString("bio"));
                    profile.put("birthDate", rs.getDate("birth_date"));
                    profile.put("location", rs.getString("location"));
                    profile.put("occupation", rs.getString("occupation"));
                    profile.put("education", rs.getString("education"));
                    profile.put("lookingFor", rs.getString("looking_for"));
                    profile.put("ageMin", rs.getInt("age_min"));
                    profile.put("ageMax", rs.getInt("age_max"));
                    profile.put("preferredGender", rs.getString("preferred_gender"));
                    profile.put("isVerified", rs.getBoolean("is_verified"));
                    profile.put("profileComplete", rs.getInt("profile_complete_percent"));
                    
                    // Lấy sở thích
                    profile.put("interests", getUserInterests(userId));
                    
                    // Lấy ảnh
                    profile.put("photos", getUserPhotos(userId));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return profile;
    }
    
    /**
     * Cập nhật profile (trực tiếp vào bảng users)
     */
    public boolean updateProfile(long userId, Map<String, String> data) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();
        
        if (data.containsKey("nickname")) {
            sql.append("nickname = ?, ");
            params.add(data.get("nickname"));
        }
        if (data.containsKey("avatarUrl")) {
            sql.append("avatar_url = ?, ");
            params.add(data.get("avatarUrl"));
        }
        if (data.containsKey("gender")) {
            sql.append("gender = ?, ");
            params.add(data.get("gender"));
        }
        if (data.containsKey("bio")) {
            sql.append("bio = ?, ");
            params.add(data.get("bio"));
        }
        if (data.containsKey("birthDate")) {
            sql.append("birth_date = ?, ");
            params.add(data.get("birthDate"));
        }
        if (data.containsKey("location")) {
            sql.append("location = ?, ");
            params.add(data.get("location"));
        }
        if (data.containsKey("occupation")) {
            sql.append("occupation = ?, ");
            params.add(data.get("occupation"));
        }
        if (data.containsKey("education")) {
            sql.append("education = ?, ");
            params.add(data.get("education"));
        }
        if (data.containsKey("lookingFor")) {
            sql.append("looking_for = ?, ");
            params.add(data.get("lookingFor"));
        }
        if (data.containsKey("ageMin")) {
            sql.append("age_min = ?, ");
            params.add(Integer.parseInt(data.get("ageMin")));
        }
        if (data.containsKey("ageMax")) {
            sql.append("age_max = ?, ");
            params.add(Integer.parseInt(data.get("ageMax")));
        }
        if (data.containsKey("preferredGender")) {
            sql.append("preferred_gender = ?, ");
            params.add(data.get("preferredGender"));
        }
        
        if (params.isEmpty()) return false;
        
        // Remove trailing ", "
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE user_id = ?");
        params.add(userId);
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            int updated = ps.executeUpdate();
            
            // Cập nhật % hoàn thành profile
            if (updated > 0) {
                updateProfileCompletion(userId);
            }
            
            return updated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy sở thích của user
     */
    public List<Map<String, Object>> getUserInterests(long userId) {
        List<Map<String, Object>> interests = new ArrayList<>();
        String sql = """
            SELECT i.interest_id, i.interest_name, c.category_name, c.icon
            FROM user_interests ui
            JOIN interests i ON ui.interest_id = i.interest_id
            JOIN interest_categories c ON i.category_id = c.category_id
            WHERE ui.user_id = ?
            ORDER BY c.category_name
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> interest = new HashMap<>();
                    interest.put("id", rs.getInt("interest_id"));
                    interest.put("name", rs.getString("interest_name"));
                    interest.put("category", rs.getString("category_name"));
                    interest.put("icon", rs.getString("icon"));
                    interests.add(interest);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return interests;
    }
    
    /**
     * Thêm sở thích
     */
    public boolean addInterest(long userId, int interestId) {
        String sql = "INSERT IGNORE INTO user_interests (user_id, interest_id) VALUES (?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, interestId);
            boolean result = ps.executeUpdate() > 0;
            if (result) updateProfileCompletion(userId);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa sở thích
     */
    public boolean removeInterest(long userId, int interestId) {
        String sql = "DELETE FROM user_interests WHERE user_id = ? AND interest_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, interestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cập nhật danh sách sở thích
     */
    public boolean updateInterests(long userId, List<Integer> interestIds) {
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Xóa tất cả sở thích cũ
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM user_interests WHERE user_id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            
            // Thêm sở thích mới
            if (!interestIds.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO user_interests (user_id, interest_id) VALUES (?, ?)")) {
                    for (int interestId : interestIds) {
                        ps.setLong(1, userId);
                        ps.setInt(2, interestId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            
            conn.commit();
            updateProfileCompletion(userId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy ảnh của user
     */
    public List<Map<String, Object>> getUserPhotos(long userId) {
        List<Map<String, Object>> photos = new ArrayList<>();
        String sql = "SELECT photo_id, photo_url, is_primary, display_order FROM user_photos WHERE user_id = ? ORDER BY display_order";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> photo = new HashMap<>();
                    photo.put("id", rs.getLong("photo_id"));
                    photo.put("url", rs.getString("photo_url"));
                    photo.put("isPrimary", rs.getBoolean("is_primary"));
                    photo.put("order", rs.getInt("display_order"));
                    photos.add(photo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return photos;
    }
    
    /**
     * Thêm ảnh
     */
    public long addPhoto(long userId, String photoUrl, boolean isPrimary) {
        String sql = "INSERT INTO user_photos (user_id, photo_url, is_primary) VALUES (?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, photoUrl);
            ps.setBoolean(3, isPrimary);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    updateProfileCompletion(userId);
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * Xóa ảnh
     */
    public boolean deletePhoto(long userId, long photoId) {
        String sql = "DELETE FROM user_photos WHERE photo_id = ? AND user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, photoId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy tất cả categories và interests
     */
    public List<Map<String, Object>> getAllInterestCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        String sql = """
            SELECT c.category_id, c.category_name, c.icon,
                   i.interest_id, i.interest_name
            FROM interest_categories c
            LEFT JOIN interests i ON c.category_id = i.category_id
            ORDER BY c.category_id, i.interest_name
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            Map<Integer, Map<String, Object>> categoryMap = new LinkedHashMap<>();
            
            while (rs.next()) {
                int catId = rs.getInt("category_id");
                
                if (!categoryMap.containsKey(catId)) {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("id", catId);
                    cat.put("name", rs.getString("category_name"));
                    cat.put("icon", rs.getString("icon"));
                    cat.put("interests", new ArrayList<Map<String, Object>>());
                    categoryMap.put(catId, cat);
                }
                
                if (rs.getInt("interest_id") > 0) {
                    Map<String, Object> interest = new HashMap<>();
                    interest.put("id", rs.getInt("interest_id"));
                    interest.put("name", rs.getString("interest_name"));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> interestList = (List<Map<String, Object>>) categoryMap.get(catId).get("interests");
                    interestList.add(interest);
                }
            }
            
            categories.addAll(categoryMap.values());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }
    
    /**
     * Tính và cập nhật % profile hoàn thành
     */
    public void updateProfileCompletion(long userId) {
        int completion = calculateProfileCompletion(userId);
        String sql = "UPDATE users SET profile_complete_percent = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, completion);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tính % profile hoàn thành
     */
    public int calculateProfileCompletion(long userId) {
        int completion = 0;
        
        String sql = """
            SELECT nickname, bio, birth_date, location, occupation, education, avatar_url
            FROM users WHERE user_id = ?
        """;
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("nickname") != null && !rs.getString("nickname").isEmpty()) completion += 10;
                    if (rs.getString("bio") != null && !rs.getString("bio").isEmpty()) completion += 15;
                    if (rs.getDate("birth_date") != null) completion += 10;
                    if (rs.getString("location") != null && !rs.getString("location").isEmpty()) completion += 10;
                    if (rs.getString("occupation") != null && !rs.getString("occupation").isEmpty()) completion += 10;
                    if (rs.getString("education") != null && !rs.getString("education").isEmpty()) completion += 10;
                    if (rs.getString("avatar_url") != null && !rs.getString("avatar_url").equals("default.png")) completion += 10;
                }
            }
            
            // Kiểm tra interests
            List<Map<String, Object>> interests = getUserInterests(userId);
            if (interests.size() >= 3) completion += 15;
            else if (!interests.isEmpty()) completion += 8;
            
            // Kiểm tra photos
            List<Map<String, Object>> photos = getUserPhotos(userId);
            if (!photos.isEmpty()) completion += 10;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Math.min(100, completion);
    }
}
