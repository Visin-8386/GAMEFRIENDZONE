package com.friendzone.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import gamefriendzone.DbConnection;

public class FriendDAO {
    
    public boolean sendFriendRequest(long senderId, long receiverId) {
        String sql = "INSERT INTO friendships (user_id1, user_id2, status, action_user_id) VALUES (?, ?, 'PENDING', ?) " +
                     "ON DUPLICATE KEY UPDATE status = IF(status='ACCEPTED', 'ACCEPTED', 'PENDING'), action_user_id = ?";
        
        long id1 = Math.min(senderId, receiverId);
        long id2 = Math.max(senderId, receiverId);
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id1);
            pstmt.setLong(2, id2);
            pstmt.setLong(3, senderId);
            pstmt.setLong(4, senderId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean acceptFriendRequest(long requesterId, long accepterId) {
        String sql = "UPDATE friendships SET status = 'ACCEPTED', action_user_id = ? " +
                     "WHERE user_id1 = ? AND user_id2 = ? AND status = 'PENDING'";
        
        long id1 = Math.min(requesterId, accepterId);
        long id2 = Math.max(requesterId, accepterId);
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, accepterId);
            pstmt.setLong(2, id1);
            pstmt.setLong(3, id2);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean checkFriendship(long u1, long u2) {
        String sql = "SELECT status FROM friendships WHERE user_id1 = ? AND user_id2 = ? AND status = 'ACCEPTED'";
        long id1 = Math.min(u1, u2);
        long id2 = Math.max(u1, u2);
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id1);
            pstmt.setLong(2, id2);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra đã gửi lời mời kết bạn đang chờ xử lý chưa
     */
    public boolean hasPendingRequest(long senderId, long receiverId) {
        String sql = "SELECT status FROM friendships WHERE user_id1 = ? AND user_id2 = ? AND status = 'PENDING'";
        long id1 = Math.min(senderId, receiverId);
        long id2 = Math.max(senderId, receiverId);
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id1);
            pstmt.setLong(2, id2);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
