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

public class ChatDAO {
    
    /**
     * Get or create a private chat room between two users
     * Synchronized to prevent race condition when both users start DM at same time
     */
    public synchronized long getOrCreatePrivateRoom(long user1, long user2) {
        long id1 = Math.min(user1, user2);
        long id2 = Math.max(user1, user2);
        
        // Check if room exists (with lock to prevent race condition)
        String checkSql = "SELECT room_id FROM private_rooms WHERE user_id1 = ? AND user_id2 = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setLong(1, id1);
            stmt.setLong(2, id2);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("room_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        
        // Create new room
        String createRoomSql = "INSERT INTO rooms (type) VALUES ('PRIVATE')";
        String linkRoomSql = "INSERT INTO private_rooms (user_id1, user_id2, room_id) VALUES (?, ?, ?)";
        String addMembersSql = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            long roomId;
            try (PreparedStatement stmt = conn.prepareStatement(createRoomSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    roomId = keys.getLong(1);
                } else {
                    conn.rollback();
                    return -1;
                }
            }
            
            // Link room to users
            try (PreparedStatement stmt = conn.prepareStatement(linkRoomSql)) {
                stmt.setLong(1, id1);
                stmt.setLong(2, id2);
                stmt.setLong(3, roomId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                // If constraint violation (duplicate entry), check again if room was created by another thread
                if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("Check constraint")) {
                    conn.rollback();
                    // Re-check if room exists now
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setLong(1, id1);
                        checkStmt.setLong(2, id2);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            return rs.getLong("room_id");
                        }
                    }
                }
                throw e; // Re-throw if not duplicate
            }
            
            // Add both users as members
            try (PreparedStatement stmt = conn.prepareStatement(addMembersSql)) {
                stmt.setLong(1, roomId);
                stmt.setLong(2, user1);
                stmt.executeUpdate();
                
                stmt.setLong(2, user2);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return roomId;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Send a message to a room
     */
    public boolean sendMessage(long roomId, long senderId, String content, String messageType) {
        String sql = "INSERT INTO messages (room_id, sender_id, content, message_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);
            stmt.setString(4, messageType);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get recent messages from a room
     */
    public List<Map<String, String>> getMessages(long roomId, int limit) {
        List<Map<String, String>> messages = new ArrayList<>();
        String sql = "SELECT m.message_id, m.sender_id, m.content, m.message_type, m.created_at, u.nickname " +
                     "FROM messages m JOIN users u ON m.sender_id = u.user_id " +
                     "WHERE m.room_id = ? AND m.deleted_at IS NULL " +
                     "ORDER BY m.created_at DESC LIMIT ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> msg = new HashMap<>();
                msg.put("messageId", String.valueOf(rs.getLong("message_id")));
                msg.put("senderId", String.valueOf(rs.getLong("sender_id")));
                msg.put("senderName", rs.getString("nickname"));
                msg.put("content", rs.getString("content"));
                msg.put("type", rs.getString("message_type"));
                msg.put("time", rs.getTimestamp("created_at").toString());
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Reverse to get chronological order
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    /**
     * Get the other user in a private room
     */
    public long getOtherUserId(long roomId, long myId) {
        String sql = "SELECT user_id1, user_id2 FROM private_rooms WHERE room_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long id1 = rs.getLong("user_id1");
                long id2 = rs.getLong("user_id2");
                return (id1 == myId) ? id2 : id1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public long createGroupRoom(String roomName, long creatorId) {
        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            String sql1 = "INSERT INTO rooms (type, created_at) VALUES ('GROUP', NOW())";
            PreparedStatement stmt1 = conn.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
            stmt1.executeUpdate();
            ResultSet rs = stmt1.getGeneratedKeys();
            if (rs.next()) {
                long roomId = rs.getLong(1);
                String sql2 = "INSERT INTO room_members (room_id, user_id, joined_at) VALUES (?, ?, NOW())";
                PreparedStatement stmt2 = conn.prepareStatement(sql2);
                stmt2.setLong(1, roomId);
                stmt2.setLong(2, creatorId);
                stmt2.executeUpdate();
                conn.commit();
                return roomId;
            }
            conn.rollback();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {}
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {}
        }
        return -1;
    }
    
    public boolean addMemberToRoom(long roomId, long userId) {
        String sql = "INSERT INTO room_members (room_id, user_id, joined_at) VALUES (?, ?, NOW())";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean removeMemberFromRoom(long roomId, long userId) {
        String sql = "DELETE FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public List<Map<String, String>> getUserGroups(long userId) {
        List<Map<String, String>> groups = new ArrayList<>();
        String sql = "SELECT r.room_id, r.created_at, COUNT(rm.user_id) as member_count " +
                     "FROM rooms r JOIN room_members rm ON r.room_id = rm.room_id " +
                     "WHERE r.type = 'GROUP' AND r.room_id IN " +
                     "(SELECT room_id FROM room_members WHERE user_id = ?) " +
                     "GROUP BY r.room_id ORDER BY r.created_at DESC";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long roomId = rs.getLong("room_id");
                Map<String, String> group = new HashMap<>();
                group.put("room_id", String.valueOf(roomId));
                group.put("member_count", String.valueOf(rs.getInt("member_count")));
                group.put("created_at", rs.getTimestamp("created_at").toString());
                group.put("room_name", getRoomName(roomId)); // Add room name
                groups.add(group);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }
    
    public List<Long> getRoomMembers(long roomId) {
        List<Long> members = new ArrayList<>();
        String sql = "SELECT user_id FROM room_members WHERE room_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }
    
    /**
     * Get room name (for group rooms)
     * Since rooms table doesn't have room_name, we generate a name from member nicknames
     */
    public String getRoomName(long roomId) {
        // Get first 3 member nicknames to create group name
        String sql = "SELECT u.nickname FROM room_members rm " +
                     "JOIN users u ON rm.user_id = u.user_id " +
                     "WHERE rm.room_id = ? " +
                     "ORDER BY rm.joined_at ASC LIMIT 3";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("nickname"));
            }
            if (!names.isEmpty()) {
                String groupName = String.join(", ", names);
                if (names.size() >= 3) {
                    groupName += "...";
                }
                return groupName;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Fallback: return generic name with room ID
        return "Nhóm mới #" + roomId;
    }
    
    /**
     * Get detailed info of room members
     * Uses users.status column (ONLINE/OFFLINE/IN_GAME/BANNED)
     */
    public List<Map<String, String>> getRoomMembersInfo(long roomId) {
        List<Map<String, String>> members = new ArrayList<>();
        String sql = "SELECT u.user_id, u.nickname, u.status, u.avatar_url, rm.joined_at " +
                     "FROM room_members rm " +
                     "JOIN users u ON rm.user_id = u.user_id " +
                     "WHERE rm.room_id = ? " +
                     "ORDER BY rm.joined_at ASC";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> member = new HashMap<>();
                member.put("userId", String.valueOf(rs.getLong("user_id")));
                member.put("nickname", rs.getString("nickname"));
                String status = rs.getString("status");
                member.put("status", status);
                member.put("isOnline", String.valueOf("ONLINE".equals(status) || "IN_GAME".equals(status)));
                member.put("avatarUrl", rs.getString("avatar_url"));
                member.put("joinedAt", rs.getTimestamp("joined_at").toString());
                members.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }
}
