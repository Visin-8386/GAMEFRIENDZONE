package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.friendzone.client.controller.ClientSocket;

/**
 * Panel khám phá và swipe người mới - Kiểu Tinder
 */
public class DiscoverPanel extends JPanel {
    private ClientSocket socket;
    private MainFrame mainFrame;
    private long userId;
    private String username;
    
    // UI Components
    private JPanel cardContainer;
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel ageLocationLabel;
    private JLabel bioLabel;
    private JPanel interestsPanel;
    private JLabel compatibilityLabel;
    private JButton likeBtn;
    private JButton passBtn;
    private JButton superLikeBtn;
    private JButton inviteGameBtn;
    private JLabel emptyLabel;
    
    // Data
    private List<Map<String, Object>> profiles = new ArrayList<>();
    private int currentIndex = 0;
    private Map<String, Object> currentProfile;
    
    // Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color CARD_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color ACCENT_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color LIKE_COLOR = new Color(46, 204, 113); // Green
    private static final Color PASS_COLOR = new Color(231, 76, 60); // Red
    private static final Color SUPER_LIKE_COLOR = new Color(52, 152, 219); // Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color SECONDARY_TEXT = new Color(170, 170, 190);
    
    public DiscoverPanel() {
        this(null, 0, "");
    }
    
    public DiscoverPanel(ClientSocket socket, int userId, String username) {
        this.socket = socket;
        this.userId = userId;
        this.username = username;
        
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_COLOR);
        
        // Header
        add(createHeader(), BorderLayout.NORTH);
        
        // Center - Card
        cardContainer = new JPanel(new BorderLayout());
        cardContainer.setBackground(BG_COLOR);
        cardContainer.setBorder(new EmptyBorder(20, 50, 20, 50));
        cardContainer.add(createProfileCard(), BorderLayout.CENTER);
        add(cardContainer, BorderLayout.CENTER);
        
        // Bottom - Action buttons
        add(createActionButtons(), BorderLayout.SOUTH);
        
        // Load data when created
        if (socket != null) {
            loadDiscoverUsers();
        }
    }
    
    private void loadDiscoverUsers() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("limit", "20");
        socket.send("GET_DISCOVER_USERS", data);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_BG);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JButton backBtn = new JButton("← Quay lại");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backBtn.setForeground(TEXT_COLOR);
        backBtn.setBackground(CARD_BG);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            if (mainFrame != null) mainFrame.showCard("LOBBY");
        });
        header.add(backBtn, BorderLayout.WEST);
        
        JLabel title = new JLabel("Kham Pha", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT_COLOR);
        header.add(title, BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("🔄");
        refreshBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        refreshBtn.setForeground(TEXT_COLOR);
        refreshBtn.setBackground(CARD_BG);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setToolTipText("Làm mới");
        refreshBtn.addActionListener(e -> requestProfiles());
        header.add(refreshBtn, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createProfileCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 2),
            new EmptyBorder(30, 30, 30, 30)
        ));
        
        // Avatar
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(200, 200));
        avatarLabel.setMaximumSize(new Dimension(200, 200));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 3));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(new Color(40, 40, 60));
        avatarLabel.setText("[User]");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        avatarLabel.setForeground(SECONDARY_TEXT);
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Name
        nameLabel = new JLabel("Đang tải...");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Age & Location
        ageLocationLabel = new JLabel("");
        ageLocationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        ageLocationLabel.setForeground(SECONDARY_TEXT);
        ageLocationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Compatibility
        compatibilityLabel = new JLabel("");
        compatibilityLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        compatibilityLabel.setForeground(ACCENT_COLOR);
        compatibilityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Bio
        bioLabel = new JLabel("");
        bioLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        bioLabel.setForeground(SECONDARY_TEXT);
        bioLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bioLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Interests
        interestsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        interestsPanel.setBackground(CARD_BG);
        interestsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Empty state
        emptyLabel = new JLabel("Không còn ai để khám phá. Quay lại sau nhé! 💫");
        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        emptyLabel.setForeground(SECONDARY_TEXT);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setVisible(false);
        
        // Invite to game button
        inviteGameBtn = new JButton("Moi choi game");
        styleButton(inviteGameBtn, new Color(155, 89, 182));
        inviteGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        inviteGameBtn.addActionListener(e -> inviteToGame());
        
        card.add(avatarLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(ageLocationLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(compatibilityLabel);
        card.add(Box.createVerticalStrut(15));
        card.add(bioLabel);
        card.add(Box.createVerticalStrut(15));
        card.add(interestsPanel);
        card.add(Box.createVerticalStrut(20));
        card.add(inviteGameBtn);
        card.add(emptyLabel);
        
        return card;
    }
    
    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(10, 0, 30, 0));
        
        // Pass button
        passBtn = createCircleButton("✕", PASS_COLOR, 70);
        passBtn.setToolTipText("Bỏ qua");
        passBtn.addActionListener(e -> swipe("PASS"));
        
        // Super Like button
        superLikeBtn = createCircleButton("⭐", SUPER_LIKE_COLOR, 55);
        superLikeBtn.setToolTipText("Super Like");
        superLikeBtn.addActionListener(e -> swipe("SUPER_LIKE"));
        
        // Like button
        likeBtn = createCircleButton("♥", LIKE_COLOR, 70);
        likeBtn.setToolTipText("Thích");
        likeBtn.addActionListener(e -> swipe("LIKE"));
        
        panel.add(passBtn);
        panel.add(superLikeBtn);
        panel.add(likeBtn);
        
        return panel;
    }
    
    private JButton createCircleButton(String text, Color color, int size) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(color.brighter());
                } else {
                    g2.setColor(color);
                }
                
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(size, size));
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, size / 2));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return btn;
    }
    
    public void setSocket(ClientSocket socket) {
        this.socket = socket;
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public void requestProfiles() {
        if (socket != null) {
            socket.send("GET_DISCOVER_PROFILES", new HashMap<>());
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadProfiles(List<Map<String, Object>> profiles) {
        this.profiles = profiles;
        this.currentIndex = 0;
        
        SwingUtilities.invokeLater(() -> {
            if (profiles.isEmpty()) {
                showEmpty();
            } else {
                showProfile(profiles.get(0));
            }
        });
    }
    
    private void showProfile(Map<String, Object> profile) {
        this.currentProfile = profile;
        
        emptyLabel.setVisible(false);
        avatarLabel.setVisible(true);
        nameLabel.setVisible(true);
        ageLocationLabel.setVisible(true);
        bioLabel.setVisible(true);
        interestsPanel.setVisible(true);
        compatibilityLabel.setVisible(true);
        inviteGameBtn.setVisible(true);
        likeBtn.setEnabled(true);
        passBtn.setEnabled(true);
        superLikeBtn.setEnabled(true);
        
        // Name
        String nickname = (String) profile.get("nickname");
        nameLabel.setText(nickname != null ? nickname : "Người dùng");
        
        // Age & Location
        Object ageObj = profile.get("age");
        String location = (String) profile.get("location");
        StringBuilder info = new StringBuilder();
        if (ageObj != null && ((Number) ageObj).intValue() > 0) {
            info.append(((Number) ageObj).intValue()).append(" tuổi");
        }
        if (location != null && !location.isEmpty()) {
            if (info.length() > 0) info.append(" • ");
            info.append("📍 ").append(location);
        }
        ageLocationLabel.setText(info.toString());
        
        // Common interests / Compatibility hint
        Object commonObj = profile.get("commonInterests");
        if (commonObj != null && ((Number) commonObj).intValue() > 0) {
            compatibilityLabel.setText("🎯 " + ((Number) commonObj).intValue() + " sở thích chung");
        } else {
            compatibilityLabel.setText("");
        }
        
        // Bio
        String bio = (String) profile.get("bio");
        if (bio != null && !bio.isEmpty()) {
            bioLabel.setText("\"" + bio + "\"");
        } else {
            bioLabel.setText("");
        }
        
        // Interests
        interestsPanel.removeAll();
        Object interestsObj = profile.get("interests");
        if (interestsObj instanceof List) {
            List<String> interests = (List<String>) interestsObj;
            for (String interest : interests) {
                JLabel tag = new JLabel(interest);
                tag.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                tag.setForeground(TEXT_COLOR);
                tag.setBackground(new Color(70, 70, 90));
                tag.setOpaque(true);
                tag.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                interestsPanel.add(tag);
            }
        }
        interestsPanel.revalidate();
        interestsPanel.repaint();
    }
    
    private void showEmpty() {
        avatarLabel.setVisible(false);
        nameLabel.setVisible(false);
        ageLocationLabel.setVisible(false);
        bioLabel.setVisible(false);
        interestsPanel.setVisible(false);
        compatibilityLabel.setVisible(false);
        inviteGameBtn.setVisible(false);
        emptyLabel.setVisible(true);
        likeBtn.setEnabled(false);
        passBtn.setEnabled(false);
        superLikeBtn.setEnabled(false);
    }
    
    private void swipe(String action) {
        if (currentProfile == null) return;
        
        long targetId = ((Number) currentProfile.get("userId")).longValue();
        
        // Gửi action lên server
        Map<String, String> data = new HashMap<>();
        data.put("targetId", String.valueOf(targetId));
        data.put("action", action);
        socket.send("SWIPE", data);
        
        // Animation effect
        animateSwipe(action);
        
        // Next profile
        currentIndex++;
        if (currentIndex < profiles.size()) {
            showProfile(profiles.get(currentIndex));
        } else {
            showEmpty();
            // Request more profiles
            requestProfiles();
        }
    }
    
    private void animateSwipe(String action) {
        // Simple color flash effect
        Color flashColor = switch (action) {
            case "LIKE" -> LIKE_COLOR;
            case "SUPER_LIKE" -> SUPER_LIKE_COLOR;
            default -> PASS_COLOR;
        };
        
        cardContainer.setBackground(flashColor);
        javax.swing.Timer timer = new javax.swing.Timer(150, e -> cardContainer.setBackground(BG_COLOR));
        timer.setRepeats(false);
        timer.start();
    }
    
    private void inviteToGame() {
        if (currentProfile == null) return;
        
        String[] games = {"CARO", "WORD_CHAIN", "LOVE_QUIZ", "DRAW_GUESS"};
        String[] gameNames = {"Cờ Caro", "Nối Từ", "Quiz Tình Yêu", "Vẽ Hình Đoán"};
        
        String choice = (String) JOptionPane.showInputDialog(
            this, "Chọn game để mời:", "Mời chơi game",
            JOptionPane.QUESTION_MESSAGE, null, gameNames, gameNames[0]
        );
        
        if (choice != null) {
            int idx = java.util.Arrays.asList(gameNames).indexOf(choice);
            String gameCode = games[idx];
            
            long targetId = ((Number) currentProfile.get("userId")).longValue();
            Map<String, String> data = new HashMap<>();
            data.put("targetId", String.valueOf(targetId));
            data.put("gameCode", gameCode);
            socket.send("SEND_INVITE", data);
            
            JOptionPane.showMessageDialog(this, 
                "Đã gửi lời mời chơi " + choice + "!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Xử lý khi match thành công
     */
    public void onMatch(String partnerName) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                "Chuc mung! Ban va " + partnerName + " da match!\n\n" +
                "Hay moi nhau choi game de tim hieu them nhe!",
                "It's a Match!", JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Handle discover users data from server
     */
    @SuppressWarnings("unchecked")
    public void handleDiscoverUsers(com.friendzone.model.NetworkMessage msg) {
        java.util.Map<String, String> data = msg.getData();
        if (data == null || !data.containsKey("users")) return;
        
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> users = gson.fromJson(data.get("users"), listType);
            loadProfiles(users);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handle swipe recorded response
     */
    public void handleSwipeRecorded(com.friendzone.model.NetworkMessage msg) {
        // Swipe đã được ghi nhận - có thể hiển thị thông báo nhỏ
        java.util.Map<String, String> data = msg.getData();
        if (data != null && "true".equals(data.get("isMatch"))) {
            String partnerName = data.getOrDefault("partnerUsername", "");
            onMatch(partnerName);
        }
    }
    
    /**
     * Handle new match notification
     */
    public void handleNewMatch(com.friendzone.model.NetworkMessage msg) {
        java.util.Map<String, String> data = msg.getData();
        if (data != null) {
            String partnerName = data.getOrDefault("partnerUsername", "Someone");
            onMatch(partnerName);
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
    }
}
