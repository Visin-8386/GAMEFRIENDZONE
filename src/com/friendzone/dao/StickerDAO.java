package com.friendzone.dao;

import gamefriendzone.DbConnection;
import java.sql.*;
import java.util.*;

public class StickerDAO {

    public List<Map<String, String>> getAllPacks() {
        List<Map<String, String>> packs = new ArrayList<>();
        String sql = "SELECT * FROM sticker_packs ORDER BY pack_id";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, String> pack = new HashMap<>();
                pack.put("pack_id", String.valueOf(rs.getInt("pack_id")));
                pack.put("name", rs.getString("name"));
                pack.put("description", rs.getString("description"));
                pack.put("is_premium", String.valueOf(rs.getBoolean("is_premium")));
                pack.put("price", String.valueOf(rs.getInt("price")));
                packs.add(pack);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return packs;
    }
    
    public List<Map<String, String>> getStickersByPack(int packId) {
        List<Map<String, String>> stickers = new ArrayList<>();
        String sql = "SELECT * FROM stickers WHERE pack_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, packId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> sticker = new HashMap<>();
                sticker.put("sticker_id", String.valueOf(rs.getInt("sticker_id")));
                sticker.put("file_url", rs.getString("file_url"));
                sticker.put("code", rs.getString("code"));
                stickers.add(sticker);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stickers;
    }
}
