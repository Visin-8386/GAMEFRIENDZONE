package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import com.friendzone.model.NetworkMessage;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Panel hiển thị danh sách matches và compatibility scores
 * Cho phép xem chi tiết và bắt đầu chat/chơi game với match
 */
public class MatchesPanel extends JPanel {
    private ClientSocket clientSocket;
    private int currentUserId;
    private String currentUsername;
    private MainFrame mainFrame;
    
    private JTabbedPane tabbedPane;
    private JPanel matchesListPanel;
    private JPanel compatibilityListPanel;
    private JScrollPane matchesScroll;
    private JScrollPane compatibilityScroll;
    
    private List<Map<String, Object>> matches;
    private List<Map<String, Object>> compatibilityList;
    
    private JButton refreshBtn;
    private JLabel statusLabel;
    
    // Colors
    // Colors
    private final Color PRIMARY_COLOR = new Color(255, 75, 110);
    private final Color SECONDARY_COLOR = new Color(255, 142, 83);
    private final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private final Color CARD_COLOR = new Color(30, 30, 45); // Dark Grey-Blue
    private final Color TEXT_PRIMARY = new Color(230, 230, 230);
    private final Color TEXT_SECONDARY = new Color(170, 170, 190);
    private final Color MATCH_COLOR = new Color(46, 204, 113); // Green
    private final Color SUPER_LIKE_COLOR = new Color(52, 152, 219); // Blue
    
    public MatchesPanel(ClientSocket clientSocket, int userId, String username) {
        this.clientSocket = clientSocket;
        this.currentUserId = userId;
        this.currentUsername = username;
        this.matches = new ArrayList<>();
        this.compatibilityList = new ArrayList<>();
        
        initComponents();
        loadData();
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    private void initComponents() {
        setBackground(BG_COLOR);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(BG_COLOR);
        tabbedPane.setForeground(Color.WHITE); // Tab text color
        
        // Matches tab
        matchesListPanel = new JPanel();
        matchesListPanel.setLayout(new BoxLayout(matchesListPanel, BoxLayout.Y_AXIS));
        matchesListPanel.setBackground(BG_COLOR);
        matchesScroll = new JScrollPane(matchesListPanel);
        matchesScroll.setBorder(null);
        matchesScroll.getVerticalScrollBar().setUnitIncrement(16);
        matchesScroll.getViewport().setBackground(BG_COLOR);
        tabbedPane.addTab("Matches", matchesScroll);
        
        // Compatibility tab
        compatibilityListPanel = new JPanel();
        compatibilityListPanel.setLayout(new BoxLayout(compatibilityListPanel, BoxLayout.Y_AXIS));
        compatibilityListPanel.setBackground(BG_COLOR);
        compatibilityScroll = new JScrollPane(compatibilityListPanel);
        compatibilityScroll.setBorder(null);
        compatibilityScroll.getVerticalScrollBar().setUnitIncrement(16);
        compatibilityScroll.getViewport().setBackground(BG_COLOR);
        tabbedPane.addTab("Do tuong thich", compatibilityScroll);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status label
        statusLabel = new JLabel("Đang tải...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // Back button
        JButton backBtn = new JButton("← Quay lại");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backBtn.setBackground(CARD_COLOR);
        backBtn.setForeground(TEXT_PRIMARY);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            if (mainFrame != null) mainFrame.showCard("LOBBY");
        });
        
        // Title
        JLabel titleLabel = new JLabel("Matches của bạn");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Refresh button
        refreshBtn = new JButton("Làm mới");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setBackground(PRIMARY_COLOR);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadData());
        
        panel.add(backBtn, BorderLayout.WEST);
        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(refreshBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private void loadData() {
        statusLabel.setText("Đang tải dữ liệu...");
        refreshBtn.setEnabled(false);
        
        // Request matches from server
        java.util.Map<String, String> matchData = new java.util.HashMap<>();
        matchData.put("userId", String.valueOf(currentUserId));
        clientSocket.send("GET_MATCHES", matchData);
        
        // Request compatibility list
        java.util.Map<String, String> compatData = new java.util.HashMap<>();
        compatData.put("userId", String.valueOf(currentUserId));
        clientSocket.send("GET_COMPATIBILITY_LIST", compatData);
    }
    
    @SuppressWarnings("unchecked")
    public void handleMatchesResponse(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            matches.clear();
            java.util.Map<String, String> data = message.getData();
            if (data != null && data.containsKey("matches")) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> list = gson.fromJson(data.get("matches"), listType);
                    if (list != null) matches.addAll(list);
                } catch (Exception e) { e.printStackTrace(); }
            }
            updateMatchesList();
            refreshBtn.setEnabled(true);
            updateStatus();
        });
    }
    
    @SuppressWarnings("unchecked")
    public void handleCompatibilityResponse(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            compatibilityList.clear();
            java.util.Map<String, String> data = message.getData();
            if (data != null && data.containsKey("compatibilityList")) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> list = gson.fromJson(data.get("compatibilityList"), listType);
                    if (list != null) compatibilityList.addAll(list);
                } catch (Exception e) { e.printStackTrace(); }
            }
            updateCompatibilityList();
            refreshBtn.setEnabled(true);
            updateStatus();
        });
    }
    
    private void updateMatchesList() {
        matchesListPanel.removeAll();
        
        if (matches.isEmpty()) {
            JPanel emptyPanel = createEmptyPanel(
                "Chưa có match nào",
                "Hãy khám phá và like người khác để tìm match!"
            );
            matchesListPanel.add(emptyPanel);
        } else {
            for (Map<String, Object> match : matches) {
                JPanel card = createMatchCard(match);
                matchesListPanel.add(card);
                matchesListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        
        matchesListPanel.revalidate();
        matchesListPanel.repaint();
    }
    
    private void updateCompatibilityList() {
        compatibilityListPanel.removeAll();
        
        if (compatibilityList.isEmpty()) {
            JPanel emptyPanel = createEmptyPanel(
                "Chua co du lieu",
                "Chơi game với người khác để tính độ tương thích!"
            );
            compatibilityListPanel.add(emptyPanel);
        } else {
            // Sort by score descending
            compatibilityList.sort((a, b) -> {
                int scoreA = (int) a.getOrDefault("totalScore", 0);
                int scoreB = (int) b.getOrDefault("totalScore", 0);
                return Integer.compare(scoreB, scoreA);
            });
            
            for (Map<String, Object> compat : compatibilityList) {
                JPanel card = createCompatibilityCard(compat);
                compatibilityListPanel.add(card);
                compatibilityListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        
        compatibilityListPanel.revalidate();
        compatibilityListPanel.repaint();
    }
    
    private JPanel createEmptyPanel(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1),
            BorderFactory.createEmptyBorder(50, 30, 50, 30)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(Box.createVerticalGlue());
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createMatchCard(Map<String, Object> match) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MATCH_COLOR, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Avatar
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY_COLOR, getWidth(), getHeight(), SECONDARY_COLOR);
                g2d.setPaint(gp);
                g2d.fillOval(0, 0, getWidth(), getHeight());
                
                // Initial
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
                String initial = match.getOrDefault("username", "?").toString().substring(0, 1).toUpperCase();
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(initial, x, y);
                
                g2d.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(70, 70));
        avatarPanel.setOpaque(false);
        card.add(avatarPanel, BorderLayout.WEST);
        
        // Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(CARD_COLOR);
        
        String username = (String) match.getOrDefault("username", "Unknown");
        boolean isSuperLike = (boolean) match.getOrDefault("isSuperLike", false);
        String matchTime = (String) match.getOrDefault("matchedAt", "");
        
        JLabel nameLabel = new JLabel(username + (isSuperLike ? " [*]" : ""));
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_PRIMARY);
        
        JLabel typeLabel = new JLabel(isSuperLike ? "Super Like Match!" : "Matched!");
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        typeLabel.setForeground(isSuperLike ? SUPER_LIKE_COLOR : MATCH_COLOR);
        
        JLabel timeLabel = new JLabel("Matched: " + matchTime);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(TEXT_SECONDARY);
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(typeLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        infoPanel.add(timeLabel);
        
        card.add(infoPanel, BorderLayout.CENTER);
        
        // Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionsPanel.setBackground(CARD_COLOR);
        
        JButton chatBtn = createActionButton("Chat", PRIMARY_COLOR);
        chatBtn.addActionListener(e -> openChat(match));
        
        JButton gameBtn = createActionButton("Chơi game", SECONDARY_COLOR);
        gameBtn.addActionListener(e -> inviteToGame(match));
        
        actionsPanel.add(chatBtn);
        actionsPanel.add(gameBtn);
        
        card.add(actionsPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private JPanel createCompatibilityCard(Map<String, Object> compat) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        
        // Score circle
        int totalScore = ((Number) compat.getOrDefault("totalScore", 0)).intValue();
        JPanel scorePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background circle
                g2d.setColor(new Color(40, 40, 60));
                g2d.fillOval(5, 5, 60, 60);
                
                // Score arc
                Color scoreColor = getScoreColor(totalScore);
                g2d.setColor(scoreColor);
                g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int arcAngle = (int) (totalScore * 3.6);
                g2d.drawArc(5, 5, 60, 60, 90, -arcAngle);
                
                // Score text
                g2d.setColor(TEXT_PRIMARY);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String scoreText = totalScore + "%";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(scoreText)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(scoreText, x, y);
                
                g2d.dispose();
            }
        };
        scorePanel.setPreferredSize(new Dimension(70, 70));
        scorePanel.setOpaque(false);
        card.add(scorePanel, BorderLayout.WEST);
        
        // Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(CARD_COLOR);
        
        String username = (String) compat.getOrDefault("username", "Unknown");
        int interestPoints = ((Number) compat.getOrDefault("interestPoints", 0)).intValue();
        int gamePoints = ((Number) compat.getOrDefault("gamePoints", 0)).intValue();
        int gamesPlayed = ((Number) compat.getOrDefault("gamesPlayed", 0)).intValue();
        
        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_PRIMARY);
        
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statsPanel.setBackground(CARD_COLOR);
        
        JLabel interestLabel = createStatLabel("Sở thích: " + interestPoints + "%");
        JLabel gameLabel = createStatLabel("Game: " + gamePoints + "%");
        JLabel playedLabel = createStatLabel("Đã chơi: " + gamesPlayed);
        
        statsPanel.add(interestLabel);
        statsPanel.add(gameLabel);
        statsPanel.add(playedLabel);
        
        // Compatibility message
        String compatMsg = getCompatibilityMessage(totalScore);
        JLabel msgLabel = new JLabel(compatMsg);
        msgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        msgLabel.setForeground(getScoreColor(totalScore));
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(statsPanel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(msgLabel);
        
        card.add(infoPanel, BorderLayout.CENTER);
        
        // Action buttons
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBackground(CARD_COLOR);
        
        JButton viewBtn = createActionButton("Xem hồ sơ", new Color(60, 60, 80));
        viewBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewBtn.addActionListener(e -> viewProfile(compat));
        
        JButton gameBtn = createActionButton("Mời chơi", PRIMARY_COLOR);
        gameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameBtn.addActionListener(e -> inviteToGame(compat));
        
        actionsPanel.add(viewBtn);
        actionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        actionsPanel.add(gameBtn);
        
        card.add(actionsPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return label;
    }
    
    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 30));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }
    
    private Color getScoreColor(int score) {
        if (score >= 80) return new Color(76, 217, 100);  // Green
        if (score >= 60) return new Color(255, 204, 0);   // Yellow
        if (score >= 40) return new Color(255, 149, 0);   // Orange
        return new Color(255, 59, 48);                     // Red
    }
    
    private String getCompatibilityMessage(int score) {
        if (score >= 90) return "Hoàn hảo cho nhau!";
        if (score >= 80) return "Rất tương thích!";
        if (score >= 70) return "Tương thích tốt";
        if (score >= 60) return "Khá hợp nhau";
        if (score >= 50) return "Có thể thử";
        if (score >= 40) return "Cần tìm hiểu thêm";
        return "Hãy chơi thêm game cùng nhau!";
    }
    
    private void updateStatus() {
        int matchCount = matches.size();
        int compatCount = compatibilityList.size();
        statusLabel.setText(String.format("%d matches - %d nguoi da tuong tac", matchCount, compatCount));
    }
    
    private void openChat(Map<String, Object> match) {
        long partnerId = ((Number) match.getOrDefault("partnerId", 0)).longValue();
        String partnerName = (String) match.getOrDefault("username", "Unknown");
        long roomId = ((Number) match.getOrDefault("roomId", 0)).longValue();
        
        // Open DM window
        SwingUtilities.invokeLater(() -> {
            DMWindow dmWindow = new DMWindow(clientSocket, roomId, currentUserId, partnerId, partnerName);
            dmWindow.setVisible(true);
        });
    }
    
    private void inviteToGame(Map<String, Object> user) {
        long partnerId = ((Number) user.getOrDefault("partnerId", 
                        ((Number) user.getOrDefault("userId", 0)))).longValue();
        String partnerName = (String) user.getOrDefault("username", "Unknown");
        
        // Show game selection dialog
        String[] games = {"Caro", "Bắt tim", "Nối từ", "Quiz tình yêu", "Vẽ đoán"};
        String selectedGame = (String) JOptionPane.showInputDialog(
            this,
            "Chọn game để mời " + partnerName + " chơi:",
            "Mời chơi game",
            JOptionPane.PLAIN_MESSAGE,
            null,
            games,
            games[0]
        );
        
        if (selectedGame != null) {
            String gameType = switch(selectedGame) {
                case "Caro" -> "CARO";
                case "Bắt tim" -> "CATCH_HEART";
                case "Nối từ" -> "WORD_CHAIN";
                case "Quiz tình yêu" -> "LOVE_QUIZ";
                case "Vẽ đoán" -> "DRAW_GUESS";
                default -> "CARO";
            };
            
            java.util.Map<String, String> inviteData = new java.util.HashMap<>();
            inviteData.put("fromUserId", String.valueOf(currentUserId));
            inviteData.put("fromUsername", currentUsername);
            inviteData.put("toUserId", String.valueOf(partnerId));
            inviteData.put("gameType", gameType);
            clientSocket.send("GAME_INVITE", inviteData);
            
            JOptionPane.showMessageDialog(this,
                "Đã gửi lời mời chơi " + selectedGame + " đến " + partnerName + "!",
                "Đã gửi lời mời",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void viewProfile(Map<String, Object> user) {
        long userId = ((Number) user.getOrDefault("partnerId",
                      ((Number) user.getOrDefault("userId", 0)))).longValue();
        String username = (String) user.getOrDefault("username", "Unknown");
        
        // Request profile from server
        java.util.Map<String, String> profileData = new java.util.HashMap<>();
        profileData.put("userId", String.valueOf(userId));
        clientSocket.send("GET_USER_PROFILE", profileData);
        
        // Show loading message
        JOptionPane.showMessageDialog(this,
            "Đang tải hồ sơ của " + username + "...",
            "Xem hồ sơ",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void handleNewMatch(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            java.util.Map<String, String> data = message.getData();
            String partnerName = data != null ? data.getOrDefault("partnerUsername", "Unknown") : "Unknown";
            boolean isSuperLike = "true".equals(data != null ? data.get("isSuperLike") : "false");
            
            // Show match notification
            String title = isSuperLike ? "[*] Super Match!" : "It's a Match!";
            String msg = "Bạn và " + partnerName + " đã match!\nHãy bắt đầu trò chuyện ngay!";
            
            JOptionPane.showMessageDialog(this,
                msg, title, JOptionPane.INFORMATION_MESSAGE);
            
            // Reload matches
            loadData();
        });
    }
    
    public void handleCompatibilityUpdate(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            java.util.Map<String, String> data = message.getData();
            String partnerName = data != null ? data.getOrDefault("partnerUsername", "Unknown") : "Unknown";
            int newScore = 0;
            int gamePoints = 0;
            try {
                newScore = Integer.parseInt(data != null ? data.getOrDefault("newScore", "0") : "0");
                gamePoints = Integer.parseInt(data != null ? data.getOrDefault("gamePoints", "0") : "0");
            } catch (Exception e) {}
            
            // Show update notification
            JOptionPane.showMessageDialog(this,
                String.format("Độ tương thích với %s đã cập nhật!\nGame: +%d%% -> Tổng: %d%%",
                    partnerName, gamePoints, newScore),
                "Cập nhật tương thích",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Reload data
            loadData();
        });
    }
    
    public void refresh() {
        loadData();
    }
}
