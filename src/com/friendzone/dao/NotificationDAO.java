package com.friendzone.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gamefriendzone.DbConnection;

/**
 * Data Access Object for managing notifications
 * Schema: notifications(notification_id, user_id, type, content, is_read, created_at)
 */
public class NotificationDAO {
    
    /**
     * Get all unread notifications for a user
     */
    public List<Map<String, String>> getUnreadNotifications(long userId) {
        List<Map<String, String>> notifications = new ArrayList<>();
        String sql = "SELECT notification_id, type, content, created_at " +
                     "FROM notifications " +
                     "WHERE user_id = ? AND is_read = FALSE " +
                     "ORDER BY created_at DESC " +
                     "LIMIT 20";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> notif = new HashMap<>();
                notif.put("id", String.valueOf(rs.getLong("notification_id")));
                notif.put("type", rs.getString("type"));
                notif.put("content", rs.getString("content"));
                notif.put("created_at", rs.getTimestamp("created_at").toString());
                notifications.add(notif);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    /**
     * Mark a notification as read
     */
    public boolean markAsRead(long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Mark all notifications for a user as read
     */
    public boolean markAllAsRead(long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Create a notification (system notification or from another user)
     * Content should include the sender info if needed
     */
    public boolean createNotification(long toUserId, Long fromUserId, String type, String content) {
        String sql = "INSERT INTO notifications (user_id, type, content) VALUES (?, ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, toUserId);
            stmt.setString(2, type);
            // Include fromUserId info in content if needed
            String fullContent = content;
            if (fromUserId != null && !content.contains("từ user #")) {
                fullContent = content + " (từ user #" + fromUserId + ")";
            }
            stmt.setString(3, fullContent);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Create a simple notification without sender info
     */
    public boolean createNotification(long userId, String type, String content) {
        String sql = "INSERT INTO notifications (user_id, type, content) VALUES (?, ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, type);
            stmt.setString(3, content);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get unread notification count
     */
    public int getUnreadCount(long userId) {
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND is_read = FALSE";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
