package com.friendzone.dao;

import gamefriendzone.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO để quản lý câu hỏi Quiz
 */
public class QuizDAO {
    
    /**
     * Lấy danh sách câu hỏi ngẫu nhiên
     * @param count Số lượng câu hỏi cần lấy
     * @param category Danh mục (null = tất cả)
     * @return Danh sách câu hỏi
     */
    public List<QuizQuestion> getRandomQuestions(int count, String category) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        String sql;
        if (category != null && !category.isEmpty()) {
            sql = "SELECT * FROM quiz_questions WHERE is_active = TRUE AND category = ? ORDER BY RAND() LIMIT ?";
        } else {
            sql = "SELECT * FROM quiz_questions WHERE is_active = TRUE ORDER BY RAND() LIMIT ?";
        }
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (category != null && !category.isEmpty()) {
                stmt.setString(paramIndex++, category);
            }
            stmt.setInt(paramIndex, count);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                QuizQuestion q = new QuizQuestion();
                q.setId(rs.getInt("question_id"));
                q.setQuestion(rs.getString("question_text"));
                q.setAnswerA(rs.getString("answer_a"));
                q.setAnswerB(rs.getString("answer_b"));
                q.setAnswerC(rs.getString("answer_c"));
                q.setAnswerD(rs.getString("answer_d"));
                q.setCategory(rs.getString("category"));
                q.setDifficulty(rs.getString("difficulty"));
                questions.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return questions;
    }
    
    /**
     * Lấy tất cả câu hỏi đang hoạt động
     */
    public List<QuizQuestion> getAllActiveQuestions() {
        return getRandomQuestions(1000, null);
    }
    
    /**
     * Thêm câu hỏi mới
     */
    public boolean addQuestion(String question, String a, String b, String c, String d, String category) {
        String sql = "INSERT INTO quiz_questions (question_text, answer_a, answer_b, answer_c, answer_d, category) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, question);
            stmt.setString(2, a);
            stmt.setString(3, b);
            stmt.setString(4, c);
            stmt.setString(5, d);
            stmt.setString(6, category != null ? category : "LOVE");
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Vô hiệu hóa câu hỏi
     */
    public boolean deactivateQuestion(int questionId) {
        String sql = "UPDATE quiz_questions SET is_active = FALSE WHERE question_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, questionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Inner class đại diện cho một câu hỏi
     */
    public static class QuizQuestion {
        private int id;
        private String question;
        private String answerA;
        private String answerB;
        private String answerC;
        private String answerD;
        private String category;
        private String difficulty;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswerA() { return answerA; }
        public void setAnswerA(String answerA) { this.answerA = answerA; }
        
        public String getAnswerB() { return answerB; }
        public void setAnswerB(String answerB) { this.answerB = answerB; }
        
        public String getAnswerC() { return answerC; }
        public void setAnswerC(String answerC) { this.answerC = answerC; }
        
        public String getAnswerD() { return answerD; }
        public void setAnswerD(String answerD) { this.answerD = answerD; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        
        /**
         * Trả về mảng [question, A, B, C, D] cho tương thích với code cũ
         */
        public String[] toArray() {
            return new String[] { question, answerA, answerB, answerC, answerD };
        }
    }
}
