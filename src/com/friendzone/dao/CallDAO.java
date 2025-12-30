package com.friendzone.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gamefriendzone.DbConnection;

/**
 * Data Access Object for managing video call history
 */
public class CallDAO {
    
    /**
     * Start a new call record
     * @return call_id if successful, -1 otherwise
     */
    public long startCall(long callerId, long receiverId, String callType) {
        // Status: ONGOING (đang gọi/đổ chuông), COMPLETED (hoàn tất), MISSED, REJECTED, BUSY
        String sql = "INSERT INTO calls (caller_id, receiver_id, call_type, status, start_time) " +
                     "VALUES (?, ?, ?, 'ONGOING', NOW())";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, callerId);
            stmt.setLong(2, receiverId);
            stmt.setString(3, callType);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
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
     * Create a call (alias for startCall)
     */
    public long createCall(long callerId, long receiverId, String callType) {
        return startCall(callerId, receiverId, callType);
    }
    
    /**
     * Create a group call
     */
    public long createGroupCall(long roomId, long callerId, String callType) {
        String sql = "INSERT INTO calls (caller_id, receiver_id, call_type, status, start_time, room_id) " +
                     "VALUES (?, 0, ?, 'RINGING', NOW(), ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, callerId);
            stmt.setString(2, callType);
            stmt.setLong(3, roomId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            // Nếu bảng không có cột room_id, dùng cách đơn giản hơn
            return startCall(callerId, 0, callType);
        }
        return -1;
    }
    
    /**
     * End a call and update status + duration
     */
    public boolean endCall(long callId, String status) {
        String sql = "UPDATE calls SET status = ?, end_time = NOW(), " +
                     "duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW()) " +
                     "WHERE call_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, callId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * End a call with COMPLETED status
     */
    public boolean endCall(long callId) {
        return endCall(callId, "COMPLETED");
    }
    
    /**
     * Get call history for a user (both incoming and outgoing)
     */
    public List<Map<String, String>> getCallHistory(long userId, int limit) {
        List<Map<String, String>> calls = new ArrayList<>();
        String sql = "SELECT c.call_id, c.caller_id, c.receiver_id, c.call_type, c.status, " +
                     "c.start_time, c.duration_seconds, " +
                     "u1.nickname as caller_name, u2.nickname as receiver_name " +
                     "FROM calls c " +
                     "LEFT JOIN users u1 ON c.caller_id = u1.user_id " +
                     "LEFT JOIN users u2 ON c.receiver_id = u2.user_id " +
                     "WHERE c.caller_id = ? OR c.receiver_id = ? " +
                     "ORDER BY c.start_time DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> call = new HashMap<>();
                call.put("call_id", String.valueOf(rs.getLong("call_id")));
                call.put("caller_id", String.valueOf(rs.getLong("caller_id")));
                call.put("receiver_id", String.valueOf(rs.getLong("receiver_id")));
                call.put("call_type", rs.getString("call_type"));
                call.put("status", rs.getString("status"));
                call.put("start_time", rs.getTimestamp("start_time").toString());
                call.put("duration", String.valueOf(rs.getInt("duration_seconds")));
                call.put("caller_name", rs.getString("caller_name"));
                call.put("receiver_name", rs.getString("receiver_name"));
                calls.add(call);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return calls;
    }
    
    /**
     * Update call status (e.g., from RINGING to ANSWERED)
     */
    public boolean updateCallStatus(long callId, String status) {
        String sql = "UPDATE calls SET status = ? WHERE call_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, callId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get call type (VIDEO or VOICE) by call ID
     */
    public String getCallType(long callId) {
        String sql = "SELECT call_type FROM calls WHERE call_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, callId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("call_type");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "VIDEO"; // Default
    }
}
