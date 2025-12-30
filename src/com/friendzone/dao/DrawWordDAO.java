package com.friendzone.dao;

import gamefriendzone.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DAO để quản lý từ vựng cho game vẽ hình đoán chữ
 */
public class DrawWordDAO {
    
    private static List<String> cachedWords = null;
    private static final Random random = new Random();
    
    /**
     * Lấy một từ ngẫu nhiên từ database
     */
    public String getRandomWord() {
        List<String> words = getAllWords();
        if (words.isEmpty()) {
            // Fallback nếu database rỗng
            String[] fallback = {"con mèo", "ngôi nhà", "mặt trời", "trái tim", "bông hoa"};
            return fallback[random.nextInt(fallback.length)];
        }
        return words.get(random.nextInt(words.size()));
    }
    
    /**
     * Lấy n từ ngẫu nhiên không trùng lặp
     */
    public List<String> getRandomWords(int count) {
        List<String> allWords = new ArrayList<>(getAllWords());
        List<String> result = new ArrayList<>();
        
        if (allWords.isEmpty()) {
            // Fallback
            String[] fallback = {"con mèo", "ngôi nhà", "mặt trời", "trái tim", "bông hoa", 
                                 "con chó", "chiếc xe", "cây cầu", "con cá", "quả táo"};
            for (int i = 0; i < Math.min(count, fallback.length); i++) {
                result.add(fallback[i]);
            }
            return result;
        }
        
        java.util.Collections.shuffle(allWords);
        for (int i = 0; i < Math.min(count, allWords.size()); i++) {
            result.add(allWords.get(i));
        }
        return result;
    }
    
    /**
     * Lấy tất cả từ từ database (cache để tối ưu)
     */
    public List<String> getAllWords() {
        if (cachedWords != null) {
            return cachedWords;
        }
        
        List<String> words = new ArrayList<>();
        String sql = "SELECT word FROM draw_words WHERE is_active = TRUE";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                words.add(rs.getString("word"));
            }
            cachedWords = words;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return words;
    }
    
    /**
     * Thêm từ mới vào database
     */
    public boolean addWord(String word, String category) {
        String sql = "INSERT INTO draw_words (word, category) VALUES (?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, word);
            ps.setString(2, category);
            
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                cachedWords = null; // Clear cache
            }
            return result;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa từ
     */
    public boolean removeWord(String word) {
        String sql = "UPDATE draw_words SET is_active = FALSE WHERE word = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, word);
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                cachedWords = null;
            }
            return result;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy từ theo category
     */
    public List<String> getWordsByCategory(String category) {
        List<String> words = new ArrayList<>();
        String sql = "SELECT word FROM draw_words WHERE category = ? AND is_active = TRUE";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    words.add(rs.getString("word"));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return words;
    }
    
    /**
     * Refresh cache
     */
    public void refreshCache() {
        cachedWords = null;
        getAllWords();
    }
}
