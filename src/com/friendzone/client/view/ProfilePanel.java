package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Panel hiển thị và chỉnh sửa profile cá nhân
 */
public class ProfilePanel extends JPanel {
    private ClientSocket socket;
    private MainFrame mainFrame;
    private long userId;
    private String username;
    
    // UI Components
    private JLabel avatarLabel;
    private JTextField nicknameField;
    private JTextArea bioArea;
    private JTextField locationField;
    private JTextField occupationField;
    private JTextField educationField;
    private JComboBox<String> lookingForCombo;
    private JComboBox<String> preferredGenderCombo;
    private JSpinner ageMinSpinner;
    private JSpinner ageMaxSpinner;
    private JPanel interestsPanel;
    private JProgressBar completionBar;
    private JLabel completionLabel;
    
    // Data
    private Map<String, Object> currentProfile;
    private Set<Integer> selectedInterests = new HashSet<>();
    private List<Map<String, Object>> allCategories;
    
    // Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color CARD_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color ACCENT_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color SECONDARY_TEXT = new Color(170, 170, 190);
    
    public ProfilePanel() {
        this(null, 0, "");
    }
    
    public ProfilePanel(ClientSocket socket, int userId, String username) {
        this.socket = socket;
        this.userId = userId;
        this.username = username;
        
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_COLOR);
        
        // Header
        add(createHeader(), BorderLayout.NORTH);
        
        // Main content với scroll
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        // Avatar section
        contentPanel.add(createAvatarSection());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Profile completion
        contentPanel.add(createCompletionSection());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Basic info
        contentPanel.add(createBasicInfoSection());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Looking for section
        contentPanel.add(createPreferencesSection());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Interests section
        contentPanel.add(createInterestsSection());
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Save button
        contentPanel.add(createSaveButton());
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
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
        
        JLabel title = new JLabel("Hồ Sơ Của Tôi", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT_COLOR);
        header.add(title, BorderLayout.CENTER);
        
        return header;
    }
    
    private JPanel createAvatarSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Avatar
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(120, 120));
        avatarLabel.setMaximumSize(new Dimension(120, 120));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 3));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(CARD_BG);
        avatarLabel.setText("[User]");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));
        avatarLabel.setForeground(SECONDARY_TEXT);
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton changeAvatarBtn = new JButton("📷 Đổi ảnh");
        changeAvatarBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        changeAvatarBtn.setForeground(ACCENT_COLOR);
        changeAvatarBtn.setBackground(BG_COLOR); // Match bg
        changeAvatarBtn.setBorderPainted(false);
        changeAvatarBtn.setFocusPainted(false);
        changeAvatarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(avatarLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(changeAvatarBtn);
        
        return panel;
    }
    
    private JPanel createCompletionSection() {
        JPanel panel = createCard("📊 Mức độ hoàn thành hồ sơ");
        
        JPanel content = new JPanel(new BorderLayout(10, 5));
        content.setBackground(CARD_BG);
        
        completionBar = new JProgressBar(0, 100);
        completionBar.setStringPainted(true);
        completionBar.setForeground(ACCENT_COLOR);
        completionBar.setBackground(new Color(40, 40, 60));
        completionBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        completionLabel = new JLabel("Hoàn thành hồ sơ để được gợi ý nhiều hơn!");
        completionLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        completionLabel.setForeground(SECONDARY_TEXT);
        
        content.add(completionBar, BorderLayout.CENTER);
        content.add(completionLabel, BorderLayout.SOUTH);
        
        panel.add(content);
        return panel;
    }
    
    private JPanel createBasicInfoSection() {
        JPanel panel = createCard("Thong tin co ban");
        
        JPanel grid = new JPanel(new GridLayout(5, 2, 15, 15));
        grid.setBackground(CARD_BG);
        
        // Nickname
        grid.add(createLabel("Tên hiển thị:"));
        nicknameField = createTextField();
        grid.add(nicknameField);
        
        // Location
        grid.add(createLabel("Vị trí:"));
        locationField = createTextField();
        locationField.setText("Hồ Chí Minh");
        grid.add(locationField);
        
        // Occupation
        grid.add(createLabel("Nghề nghiệp:"));
        occupationField = createTextField();
        grid.add(occupationField);
        
        // Education
        grid.add(createLabel("Học vấn:"));
        educationField = createTextField();
        grid.add(educationField);
        
        // Bio
        grid.add(createLabel("Giới thiệu:"));
        bioArea = new JTextArea(3, 20);
        bioArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bioArea.setBackground(new Color(40, 40, 60));
        bioArea.setForeground(TEXT_COLOR);
        bioArea.setCaretColor(TEXT_COLOR);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        bioArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane bioScroll = new JScrollPane(bioArea);
        bioScroll.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100)));
        grid.add(bioScroll);
        
        panel.add(grid);
        return panel;
    }
    
    private JPanel createPreferencesSection() {
        JPanel panel = createCard("Tim kiem");
        
        JPanel grid = new JPanel(new GridLayout(3, 2, 15, 15));
        grid.setBackground(CARD_BG);
        
        // Looking for
        grid.add(createLabel("Mục đích:"));
        lookingForCombo = new JComboBox<>(new String[]{
            "DATING - Hẹn hò", 
            "FRIENDSHIP - Kết bạn", 
            "SERIOUS - Nghiêm túc", 
            "CASUAL - Thoải mái"
        });
        styleComboBox(lookingForCombo);
        grid.add(lookingForCombo);
        
        // Preferred gender
        grid.add(createLabel("Quan tâm:"));
        preferredGenderCombo = new JComboBox<>(new String[]{
            "BOTH - Tất cả", 
            "MALE - Nam", 
            "FEMALE - Nữ", 
            "OTHER - Khác"
        });
        styleComboBox(preferredGenderCombo);
        grid.add(preferredGenderCombo);
        
        // Age range
        grid.add(createLabel("Độ tuổi:"));
        JPanel agePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        agePanel.setBackground(CARD_BG);
        ageMinSpinner = new JSpinner(new SpinnerNumberModel(18, 18, 99, 1));
        ageMaxSpinner = new JSpinner(new SpinnerNumberModel(35, 18, 99, 1));
        styleSpinner(ageMinSpinner);
        styleSpinner(ageMaxSpinner);
        agePanel.add(ageMinSpinner);
        agePanel.add(new JLabel(" - ") {{ setForeground(TEXT_COLOR); }});
        agePanel.add(ageMaxSpinner);
        agePanel.add(new JLabel(" tuổi") {{ setForeground(SECONDARY_TEXT); }});
        grid.add(agePanel);
        
        panel.add(grid);
        return panel;
    }
    
    private JPanel createInterestsSection() {
        JPanel panel = createCard("🎯 Sở thích (chọn ít nhất 3)");
        
        interestsPanel = new JPanel();
        interestsPanel.setLayout(new BoxLayout(interestsPanel, BoxLayout.Y_AXIS));
        interestsPanel.setBackground(CARD_BG);
        
        // Placeholder - sẽ được populate khi load data
        JLabel loadingLabel = new JLabel("Đang tải sở thích...");
        loadingLabel.setForeground(SECONDARY_TEXT);
        interestsPanel.add(loadingLabel);
        
        panel.add(interestsPanel);
        return panel;
    }
    
    private JPanel createSaveButton() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BG_COLOR);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JButton saveBtn = new JButton("💾 Lưu thay đổi");
        styleButton(saveBtn, ACCENT_COLOR);
        saveBtn.setForeground(Color.BLACK); // Neon needs black text
        saveBtn.setPreferredSize(new Dimension(200, 45));
        saveBtn.addActionListener(e -> saveProfile());
        
        panel.add(saveBtn);
        return panel;
    }
    
    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1),
            new EmptyBorder(15, 20, 15, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(15));
        
        return card;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(new Color(60, 60, 80));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 100)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }
    
    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(new Color(60, 60, 80));
        combo.setForeground(TEXT_COLOR);
    }
    
    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.getEditor().getComponent(0).setBackground(new Color(60, 60, 80));
        spinner.getEditor().getComponent(0).setForeground(TEXT_COLOR);
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
    
    /**
     * Load profile data từ server
     */
    public void loadProfile(Map<String, Object> profileData) {
        this.currentProfile = profileData;
        
        SwingUtilities.invokeLater(() -> {
            if (profileData.get("nickname") != null) {
                nicknameField.setText((String) profileData.get("nickname"));
            }
            if (profileData.get("bio") != null) {
                bioArea.setText((String) profileData.get("bio"));
            }
            if (profileData.get("location") != null) {
                locationField.setText((String) profileData.get("location"));
            }
            if (profileData.get("occupation") != null) {
                occupationField.setText((String) profileData.get("occupation"));
            }
            if (profileData.get("education") != null) {
                educationField.setText((String) profileData.get("education"));
            }
            
            // Looking for
            String lookingFor = (String) profileData.get("lookingFor");
            if (lookingFor != null) {
                for (int i = 0; i < lookingForCombo.getItemCount(); i++) {
                    if (lookingForCombo.getItemAt(i).startsWith(lookingFor)) {
                        lookingForCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            // Preferred gender
            String preferredGender = (String) profileData.get("preferredGender");
            if (preferredGender != null) {
                for (int i = 0; i < preferredGenderCombo.getItemCount(); i++) {
                    if (preferredGenderCombo.getItemAt(i).startsWith(preferredGender)) {
                        preferredGenderCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            // Age range
            if (profileData.get("ageMin") != null) {
                ageMinSpinner.setValue(((Number) profileData.get("ageMin")).intValue());
            }
            if (profileData.get("ageMax") != null) {
                ageMaxSpinner.setValue(((Number) profileData.get("ageMax")).intValue());
            }
            
            // Completion
            updateCompletion();
        });
    }
    
    /**
     * Load interests categories
     */
    @SuppressWarnings("unchecked")
    public void loadInterests(List<Map<String, Object>> categories, List<Integer> userInterestIds) {
        this.allCategories = categories;
        this.selectedInterests.clear();
        if (userInterestIds != null) {
            this.selectedInterests.addAll(userInterestIds);
        }
        
        SwingUtilities.invokeLater(() -> {
            interestsPanel.removeAll();
            
            for (Map<String, Object> category : categories) {
                String catName = (String) category.get("name");
                String icon = (String) category.get("icon");
                
                JLabel catLabel = new JLabel(icon + " " + catName);
                catLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                catLabel.setForeground(SECONDARY_TEXT);
                catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                interestsPanel.add(catLabel);
                interestsPanel.add(Box.createVerticalStrut(8));
                
                JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
                tagsPanel.setBackground(CARD_BG);
                tagsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                List<Map<String, Object>> interests = (List<Map<String, Object>>) category.get("interests");
                for (Map<String, Object> interest : interests) {
                    int id = ((Number) interest.get("id")).intValue();
                    String name = (String) interest.get("name");
                    
                    JToggleButton tag = new JToggleButton(name);
                    tag.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    tag.setFocusPainted(false);
                    tag.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    tag.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                    
                    if (selectedInterests.contains(id)) {
                        tag.setSelected(true);
                        tag.setBackground(ACCENT_COLOR);
                        tag.setForeground(Color.WHITE);
                    } else {
                        tag.setBackground(new Color(70, 70, 90));
                        tag.setForeground(TEXT_COLOR);
                    }
                    
                    tag.addActionListener(e -> {
                        if (tag.isSelected()) {
                            selectedInterests.add(id);
                            tag.setBackground(ACCENT_COLOR);
                            tag.setForeground(Color.WHITE);
                        } else {
                            selectedInterests.remove(id);
                            tag.setBackground(new Color(70, 70, 90));
                            tag.setForeground(TEXT_COLOR);
                        }
                        updateCompletion();
                    });
                    
                    tagsPanel.add(tag);
                }
                
                interestsPanel.add(tagsPanel);
                interestsPanel.add(Box.createVerticalStrut(15));
            }
            
            interestsPanel.revalidate();
            interestsPanel.repaint();
        });
    }
    
    private void updateCompletion() {
        int completion = 0;
        
        if (!nicknameField.getText().trim().isEmpty()) completion += 10;
        if (!bioArea.getText().trim().isEmpty()) completion += 15;
        if (!locationField.getText().trim().isEmpty()) completion += 10;
        if (!occupationField.getText().trim().isEmpty()) completion += 10;
        if (!educationField.getText().trim().isEmpty()) completion += 10;
        if (selectedInterests.size() >= 3) completion += 20;
        else if (!selectedInterests.isEmpty()) completion += 10;
        // Avatar, photos bonus
        completion += 25; // Assume có avatar
        
        completion = Math.min(100, completion);
        completionBar.setValue(completion);
        
        if (completion < 50) {
            completionLabel.setText("Hoàn thành hồ sơ để được gợi ý nhiều hơn!");
            completionBar.setForeground(new Color(231, 76, 60));
        } else if (completion < 80) {
            completionLabel.setText("Tốt lắm! Thêm vài thông tin nữa nhé!");
            completionBar.setForeground(new Color(241, 196, 15));
        } else {
            completionLabel.setText("Ho so cua ban rat hap dan!");
            completionBar.setForeground(ACCENT_COLOR);
        }
    }
    
    private void saveProfile() {
        Map<String, String> data = new HashMap<>();
        data.put("bio", bioArea.getText().trim());
        data.put("location", locationField.getText().trim());
        data.put("occupation", occupationField.getText().trim());
        data.put("education", educationField.getText().trim());
        
        String lookingFor = ((String) lookingForCombo.getSelectedItem()).split(" - ")[0];
        data.put("lookingFor", lookingFor);
        
        String preferredGender = ((String) preferredGenderCombo.getSelectedItem()).split(" - ")[0];
        data.put("preferredGender", preferredGender);
        
        data.put("ageMin", String.valueOf(ageMinSpinner.getValue()));
        data.put("ageMax", String.valueOf(ageMaxSpinner.getValue()));
        
        // Convert interests to comma-separated string
        StringBuilder interestsStr = new StringBuilder();
        for (Integer id : selectedInterests) {
            if (interestsStr.length() > 0) interestsStr.append(",");
            interestsStr.append(id);
        }
        data.put("interests", interestsStr.toString());
        
        socket.send("UPDATE_PROFILE", data);
        
        JOptionPane.showMessageDialog(this, 
            "Đã lưu thay đổi!", "Thành công", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Handle profile data received from server
     */
    @SuppressWarnings("unchecked")
    public void handleProfileData(com.friendzone.model.NetworkMessage msg) {
        SwingUtilities.invokeLater(() -> {
            java.util.Map<String, String> data = msg.getData();
            if (data == null) return;
            
            // Load basic info
            bioArea.setText(data.getOrDefault("bio", ""));
            locationField.setText(data.getOrDefault("location", ""));
            occupationField.setText(data.getOrDefault("occupation", ""));
            educationField.setText(data.getOrDefault("education", ""));
            
            // Load preferences
            String lookingFor = data.getOrDefault("lookingFor", "DATING");
            for (int i = 0; i < lookingForCombo.getItemCount(); i++) {
                if (lookingForCombo.getItemAt(i).startsWith(lookingFor)) {
                    lookingForCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            String preferredGender = data.getOrDefault("preferredGender", "BOTH");
            for (int i = 0; i < preferredGenderCombo.getItemCount(); i++) {
                if (preferredGenderCombo.getItemAt(i).startsWith(preferredGender)) {
                    preferredGenderCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            try {
                ageMinSpinner.setValue(Integer.parseInt(data.getOrDefault("ageMin", "18")));
                ageMaxSpinner.setValue(Integer.parseInt(data.getOrDefault("ageMax", "99")));
            } catch (Exception e) {}
            
            // Load interests
            String interestsJson = data.get("interests");
            if (interestsJson != null && !interestsJson.isEmpty()) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> interests = gson.fromJson(interestsJson, listType);
                    selectedInterests.clear();
                    for (Map<String, Object> interest : interests) {
                        selectedInterests.add(((Number) interest.get("id")).intValue());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            
            updateCompletion();
        });
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
