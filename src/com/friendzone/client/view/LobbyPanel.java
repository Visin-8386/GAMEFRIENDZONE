package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.friendzone.client.controller.ClientSocket;

public class LobbyPanel extends JPanel {
    private MainFrame mainFrame; // reference to MainFrame
    private ClientSocket socket;
    private long myId; // Current user ID
    private JLabel welcomeLabel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private JPanel gamePanel;

    private javax.swing.DefaultListModel<String> userListModel;
    private javax.swing.JList<String> userList;
    private java.util.Map<String, Long> userMap = new java.util.HashMap<>(); // Nickname -> ID
    private JButton notificationBtn;
    private int unreadNotifCount = 0;
    
    // Group list
    private javax.swing.DefaultListModel<String> groupListModel;
    private javax.swing.JList<String> groupList;
    private java.util.Map<String, Long> groupMap = new java.util.HashMap<>(); // Display name -> room_id

    // Modern Theme Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230); // Off-White
    private static final Color ACCENT_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color BTN_COLOR = new Color(60, 60, 80); // Dark Button BG
    private static final Color BTN_HOVER = new Color(80, 80, 100);
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);

    public LobbyPanel(ClientSocket socket) {
        this.socket = socket;
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Top: Welcome + Notifications
        welcomeLabel = new JLabel("Chào mừng đến với Friendzone!");
        welcomeLabel.setFont(TITLE_FONT);
        welcomeLabel.setForeground(new Color(241, 196, 15)); // Yellow
        welcomeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Notification bell button
        notificationBtn = new JButton("[!]");
        notificationBtn.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        notificationBtn.setFocusPainted(false);
        notificationBtn.setBackground(new Color(230, 126, 34));
        notificationBtn.setForeground(Color.WHITE);
        notificationBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        notificationBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        notificationBtn.setToolTipText("Thông báo");
        notificationBtn.addActionListener(e -> {
            socket.send("GET_NOTIFICATIONS", new java.util.HashMap<>());
        });
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(notificationBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        
        // Center: Chat, Group List, and User List
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        centerPanel.setOpaque(false);
        
        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(MAIN_FONT);
        chatArea.setBackground(PANEL_BG);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR), "Trò chuyện chung", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
            MAIN_FONT, TEXT_COLOR));
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        centerPanel.add(chatScroll);
        
        // Group List
        groupListModel = new javax.swing.DefaultListModel<>();
        groupList = new javax.swing.JList<>(groupListModel);
        groupList.setFont(MAIN_FONT);
        groupList.setBackground(PANEL_BG);
        groupList.setForeground(TEXT_COLOR);
        groupList.setSelectionBackground(ACCENT_COLOR);
        groupList.setSelectionForeground(Color.WHITE);
        
        // Double-click to open group chat
        groupList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = groupList.getSelectedValue();
                    if (selected != null) {
                        Long roomId = groupMap.get(selected);
                        if (roomId != null && mainFrame != null) {
                            mainFrame.openGroupWindow(roomId, selected);
                        }
                    }
                }
            }
        });
        
        JScrollPane groupScroll = new JScrollPane(groupList);
        groupScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR), "Nhóm của tôi", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
            MAIN_FONT, TEXT_COLOR));
        groupScroll.setOpaque(false);
        groupScroll.getViewport().setOpaque(false);
        centerPanel.add(groupScroll);
        
        // User List
        userListModel = new javax.swing.DefaultListModel<>();
        userList = new javax.swing.JList<>(userListModel);
        userList.setFont(MAIN_FONT);
        userList.setBackground(PANEL_BG);
        userList.setForeground(TEXT_COLOR);
        userList.setSelectionBackground(ACCENT_COLOR);
        userList.setSelectionForeground(Color.WHITE);
        userList.setCellRenderer(new UserListCellRenderer()); // Custom renderer with avatars
        
        // Add right-click menu
        setupUserListMenu();
        
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR), "Người dùng đang online", 
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
            MAIN_FONT, TEXT_COLOR));
        userScroll.setOpaque(false);
        userScroll.getViewport().setOpaque(false);
        centerPanel.add(userScroll);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom: Input and Action Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        
        chatInput = new JTextField();
        chatInput.setFont(MAIN_FONT);
        chatInput.setBackground(Color.WHITE);
        chatInput.setForeground(Color.BLACK);
        chatInput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        sendButton = new JButton("GỮI");
        styleButton(sendButton, ACCENT_COLOR);
        sendButton.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());
        
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(inputPanel, BorderLayout.NORTH); // Chat input at the top of bottomPanel

        // Action buttons panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.setOpaque(false);
        
        JButton findMatchBtn = new JButton("Tìm đối thủ");
        findMatchBtn.setFont(MAIN_FONT);
        findMatchBtn.setFocusPainted(false);
        findMatchBtn.setBackground(new Color(0, 150, 136));
        findMatchBtn.setForeground(Color.WHITE);
        findMatchBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        findMatchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        findMatchBtn.addActionListener(e -> {
            socket.send("FIND_MATCH", new java.util.HashMap<>());
        });
        
        JButton leaderboardBtn = new JButton("Bảng xếp hạng");
        leaderboardBtn.setFont(MAIN_FONT);
        leaderboardBtn.setFocusPainted(false);
        leaderboardBtn.setBackground(new Color(255, 152, 0));
        leaderboardBtn.setForeground(Color.WHITE);
        leaderboardBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        leaderboardBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        leaderboardBtn.addActionListener(e -> {
            String[] games = {"CARO", "CATCH_HEART", "WORD_CHAIN", "LOVE_QUIZ", "DRAW_GUESS"};
            String choice = (String) javax.swing.JOptionPane.showInputDialog(
                this, "Chọn trò chơi:", "Bảng xếp hạng",
                javax.swing.JOptionPane.QUESTION_MESSAGE, null, games, games[0]
            );
            if (choice != null) {
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("gameCode", choice);
                data.put("limit", "10");
                socket.send("GET_LEADERBOARD", data);
            }
        });
        
        JButton callHistoryBtn = new JButton("Lịch sử cuộc gọi");
        callHistoryBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        callHistoryBtn.setFocusPainted(false);
        callHistoryBtn.setBackground(new Color(155, 89, 182));
        callHistoryBtn.setForeground(Color.WHITE);
        callHistoryBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        callHistoryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        callHistoryBtn.addActionListener(e -> {
            socket.send("GET_CALL_HISTORY", new java.util.HashMap<>());
        });
        
        JButton logoutBtn = new JButton("Đăng xuất");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(192, 57, 43)); // Dark Red
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        
        actionPanel.add(findMatchBtn);
        actionPanel.add(leaderboardBtn);
        actionPanel.add(callHistoryBtn);
        actionPanel.add(logoutBtn);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH); // Action buttons at the bottom of bottomPanel
        
        add(bottomPanel, BorderLayout.SOUTH); // Add the combined bottomPanel to the main frame

        // Right: Games and Dating - wrapped in scroll pane for better visibility
        gamePanel = new JPanel();
        gamePanel.setLayout(new javax.swing.BoxLayout(gamePanel, javax.swing.BoxLayout.Y_AXIS));
        gamePanel.setOpaque(false);
        gamePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        // Dating section label
        JLabel datingLabel = new JLabel("[<3] GHEP DOI", SwingConstants.CENTER);
        datingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        datingLabel.setForeground(new Color(255, 107, 129));
        datingLabel.setOpaque(true);
        datingLabel.setBackground(new Color(60, 60, 80));
        datingLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        datingLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        datingLabel.setMaximumSize(new java.awt.Dimension(200, 35));
        
        JButton btnProfile = new JButton("Hồ sơ của tôi");
        styleButton(btnProfile, new Color(255, 107, 129)); // Pink
        
        JButton btnDiscover = new JButton("Khám phá");
        styleButton(btnDiscover, new Color(255, 75, 110)); // Red-pink
        
        JButton btnMatches = new JButton("Matches");
        styleButton(btnMatches, new Color(255, 142, 83)); // Orange
        
        // Games section label
        JLabel gamesLabel = new JLabel("[GAME] TRO CHOI", SwingConstants.CENTER);
        gamesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gamesLabel.setForeground(new Color(52, 152, 219));
        gamesLabel.setOpaque(true);
        gamesLabel.setBackground(new Color(60, 60, 80));
        gamesLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        gamesLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        gamesLabel.setMaximumSize(new java.awt.Dimension(200, 35));
        
        JButton btnCaro = new JButton("Chơi Caro");
        styleButton(btnCaro, new Color(231, 76, 60)); // Red
        
        JButton btnHeart = new JButton("Bắt trái tim");
        styleButton(btnHeart, new Color(230, 126, 34)); // Orange
        
        JButton btnWordChain = new JButton("Nối từ");
        styleButton(btnWordChain, new Color(46, 204, 113)); // Green
        
        JButton btnLoveQuiz = new JButton("Quiz tình yêu");
        styleButton(btnLoveQuiz, new Color(233, 30, 99)); // Pink
        
        JButton btnDrawGuess = new JButton("Vẽ hình đoán chữ");
        styleButton(btnDrawGuess, new Color(156, 39, 176)); // Deep Purple
        
        JButton btnFriend = new JButton("Thêm bạn bè");
        styleButton(btnFriend, new Color(155, 89, 182)); // Purple
        
        JButton btnCreateGroup = new JButton("Tạo nhóm");
        styleButton(btnCreateGroup, new Color(0, 150, 136)); // Teal
        
        JButton btnMyGroups = new JButton("Nhóm của tôi");
        styleButton(btnMyGroups, new Color(52, 152, 219)); // Blue
        
        JButton btnWallet = new JButton("Ví của tôi");
        styleButton(btnWallet, new Color(241, 196, 15)); // Yellow
        
        // Dating actions
        btnProfile.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.showCard("PROFILE");
            }
        });
        btnDiscover.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.showCard("DISCOVER");
            }
        });
        btnMatches.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.showCard("MATCHES");
            }
        });
        
        // Game actions
        btnCaro.addActionListener(e -> inviteSelectedUser("CARO"));
        btnHeart.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.showCard("CATCH_HEART");
            }
        });
        btnWordChain.addActionListener(e -> inviteSelectedUser("WORD_CHAIN"));
        btnLoveQuiz.addActionListener(e -> inviteSelectedUser("LOVE_QUIZ"));
        btnDrawGuess.addActionListener(e -> inviteSelectedUser("DRAW_GUESS"));
        btnFriend.addActionListener(e -> addFriend());
        btnCreateGroup.addActionListener(e -> createGroup());
        btnMyGroups.addActionListener(e -> socket.send("GET_MY_GROUPS", new java.util.HashMap<>()));
        btnWallet.addActionListener(e -> showWallet());
        
        // Add spacing helper
        java.awt.Dimension btnSize = new java.awt.Dimension(180, 35);
        java.awt.Component spacing = javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5));
        
        // Configure button sizes for BoxLayout
        JButton[] allButtons = {btnProfile, btnDiscover, btnMatches, btnCaro, btnHeart, 
                                btnWordChain, btnLoveQuiz, btnDrawGuess, btnFriend, 
                                btnCreateGroup, btnMyGroups, btnWallet};
        for (JButton btn : allButtons) {
            btn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(btnSize);
            btn.setPreferredSize(btnSize);
        }
        
        // Add dating section
        gamePanel.add(datingLabel);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5)));
        gamePanel.add(btnProfile);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnDiscover);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnMatches);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 10)));
        
        // Add games section
        gamePanel.add(gamesLabel);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5)));
        gamePanel.add(btnCaro);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnHeart);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnWordChain);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnLoveQuiz);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnDrawGuess);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 10)));
        gamePanel.add(btnFriend);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnCreateGroup);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnMyGroups);
        gamePanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 3)));
        gamePanel.add(btnWallet);
        
        // Wrap in scroll pane for better visibility
        JScrollPane gameScrollPane = new JScrollPane(gamePanel);
        gameScrollPane.setOpaque(false);
        gameScrollPane.getViewport().setOpaque(false);
        gameScrollPane.setBorder(null);
        gameScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameScrollPane.setPreferredSize(new java.awt.Dimension(200, 0));
        
        add(gameScrollPane, BorderLayout.EAST);
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.myId = mainFrame.getMyId();
    }
    
    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(10, 20, 10, 20));
        
        // Make it look flat and modern
        btn.setBorder(new javax.swing.border.LineBorder(bg.darker(), 1, true)); // Rounded border hint
        
        // Add hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
    }
    
    private void inviteSelectedUser(String gameCode) {
        String selectedNick = userList.getSelectedValue();
        if (selectedNick == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để mời!");
            return;
        }
        
        Long targetId = userMap.get(selectedNick);
        if (targetId != null) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("gameCode", gameCode);
            data.put("targetId", String.valueOf(targetId));
            socket.send("INVITE", data);
            appendChat("Đã mời " + selectedNick + " chơi " + gameCode);
        }
    }
    
    private void addFriend() {
        String selectedNick = userList.getSelectedValue();
        if (selectedNick == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để kết bạn!");
            return;
        }
        
        Long targetId = userMap.get(selectedNick);
        if (targetId != null) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("targetId", String.valueOf(targetId));
            socket.send("FRIEND_REQUEST", data);
        }
    }

    public void updateOnlineUsers(java.util.List<java.util.Map<String, String>> users) {
        userListModel.clear();
        userMap.clear();
        for (java.util.Map<String, String> u : users) {
            String nick = u.get("nickname");
            long id = Long.parseLong(u.get("id"));
            userListModel.addElement(nick);
            userMap.put(nick, id);
        }
    }
    
    private void sendChat() {
        String msg = chatInput.getText();
        if (!msg.isEmpty()) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("message", msg);
            socket.send("CHAT", data);
            chatInput.setText("");
        }
    }
    
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    public void setWelcomeText(String text) {
        welcomeLabel.setText(text);
        // Auto-load groups on login
        socket.send("GET_MY_GROUPS", new java.util.HashMap<>());
    }
    
    public void showMatchResults(java.util.List<com.friendzone.model.User> matches) {
        if (matches.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Không tìm thấy đối thủ phù hợp!");
            return;
        }
        
        StringBuilder sb = new StringBuilder("<html><body style='width: 300px;'>");
        sb.append("<h3>Tìm thấy ").append(matches.size()).append(" đối thủ:</h3>");
        sb.append("<table border='1' cellpadding='5'>");
        sb.append("<tr><th>ID</th><th>Biệt danh</th><th>ELO</th></tr>");
        for (com.friendzone.model.User u : matches) {
            sb.append("<tr><td>").append(u.getId()).append("</td><td>")
              .append(u.getNickname()).append("</td><td>")
              .append(u.getElo()).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        
        javax.swing.JOptionPane.showMessageDialog(this, sb.toString(), "Kết quả tìm kiếm", 
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void setupUserListMenu() {
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem sendDM = new javax.swing.JMenuItem("Gửi tin nhắn");
        sendDM.addActionListener(e -> {
            String selectedNick = userList.getSelectedValue();
            if (selectedNick != null) {
                Long targetId = userMap.get(selectedNick);
                if (targetId != null) {
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("targetId", String.valueOf(targetId));
                    socket.send("START_DM", data);
                }
            }
        });
        popup.add(sendDM);
        
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
            private void showPopup(java.awt.event.MouseEvent e) {
                int index = userList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    userList.setSelectedIndex(index);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    public void showLeaderboard(String gameCode, java.util.List<com.friendzone.model.User> topPlayers) {
        if (topPlayers.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Không có dữ liệu!");
            return;
        }
        
        StringBuilder sb = new StringBuilder("<html><body style='width: 400px;'>");
        sb.append("<h2>🏆 Bảng xếp hạng ").append(gameCode).append("</h2>");
        sb.append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
        sb.append("<tr style='background-color: #2196F3; color: white;'>")
          .append("<th>Hạng</th><th>Biệt danh</th><th>ELO</th></tr>");
        
        int rank = 1;
        for (com.friendzone.model.User u : topPlayers) {
            String bgColor = rank <= 3 ? "#FFD700" : "#FFFFFF";
            sb.append("<tr style='background-color: ").append(bgColor).append(";'>");
            sb.append("<td><b>").append(rank++).append("</b></td>")
              .append("<td>").append(u.getNickname()).append("</td>")
              .append("<td>").append(u.getElo()).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        
        javax.swing.JOptionPane.showMessageDialog(this, sb.toString(), 
            "Top người chơi " + gameCode, javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void showNotifications(java.util.List<java.util.Map<String, String>> notifications, int count) {
        unreadNotifCount = count;
        updateNotificationBadge();
        
        if (notifications.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Không có thông báo!");
            return;
        }
        
        StringBuilder sb = new StringBuilder("<html><body style='width: 400px;'>");
        sb.append("<h2>[!] Thong bao (").append(count).append(" chua doc)</h2>");
        sb.append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
        
        for (java.util.Map<String, String> notif : notifications) {
            String type = notif.get("type");
            String content = notif.get("content");
            String from = notif.get("from_nickname");
            String time = notif.get("created_at");
            
            String emoji = getNotificationEmoji(type);
            sb.append("<tr><td>").append(emoji).append(" <b>").append(type).append("</b><br>")
              .append("Từ: ").append(from != null ? from : "Hệ thống").append("<br>")
              .append(content).append("<br><small>").append(time).append("</small></td></tr>");
        }
        sb.append("</table></body></html>");
        
        javax.swing.JOptionPane.showMessageDialog(this, sb.toString(), 
            "Thông báo", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            
        // Mark all as read
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("notificationId", "ALL");
        socket.send("MARK_READ", data);
    }
    
    private void updateNotificationBadge() {
        if (unreadNotifCount > 0) {
            notificationBtn.setText("[!] " + unreadNotifCount);
        } else {
            notificationBtn.setText("[!]");
        }
    }
    
    private String getNotificationEmoji(String type) {
        switch (type) {
            case "FRIEND_REQUEST": return "[+]";
            case "GAME_INVITE": return "[G]";
            case "MESSAGE": return "[M]";
            case "ACHIEVEMENT": return "[*]";
            default: return "[i]";
        }
    }
    
    public void showCallHistory(java.util.List<java.util.Map<String, String>> calls) {
        if (calls.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Không có lịch sử cuộc gọi!");
            return;
        }
        
        StringBuilder sb = new StringBuilder("<html><body style='width: 500px;'>");
        sb.append("<h2>Lich su cuoc goi</h2>");
        sb.append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
        sb.append("<tr style='background-color: #9B59B6; color: white;'>")
          .append("<th>Loại</th><th>Với</th><th>Trạng thái</th><th>Thời lượng</th><th>Thời gian</th></tr>");
        
        for (java.util.Map<String, String> call : calls) {
            String callType = call.get("call_type");
            String status = call.get("status");
            String duration = call.get("duration");
            String startTime = call.get("start_time");
            String callerName = call.get("caller_name");
            String receiverName = call.get("receiver_name");
            
            // Determine who the "other party" is
            String otherParty = callerName; // Default assume we are receiver
            // This is simplified - in real impl, compare with current user ID
            
            String typeEmoji = "📹";
            String statusEmoji = getCallStatusEmoji(status);
            String durationText = duration.equals("0") ? "N/A" : duration + "s";
            
            sb.append("<tr><td>").append(typeEmoji).append(" ").append(callType).append("</td>")
              .append("<td>").append(otherParty).append("</td>")
              .append("<td>").append(statusEmoji).append(" ").append(status).append("</td>")
              .append("<td>").append(durationText).append("</td>")
              .append("<td><small>").append(startTime).append("</small></td></tr>");
        }
        sb.append("</table></body></html>");
        
        javax.swing.JOptionPane.showMessageDialog(this, sb.toString(), 
            "Lịch sử cuộc gọi", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String getCallStatusEmoji(String status) {
        switch (status) {
            case "ANSWERED": return "[OK]";
            case "MISSED": return "[X]";
            case "ENDED": return "[-]";
            case "RINGING": return "[...]";
            default: return "[?]";
        }
    }
    
    private void createGroup() {
        // Lấy danh sách người dùng online (trừ bản thân)
        if (userListModel.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Không có người dùng nào online để thêm vào nhóm!", 
                "Lỗi", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Tạo dialog chọn thành viên
        javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new java.awt.Dimension(300, 400));
        
        // Tên nhóm
        javax.swing.JTextField nameField = new javax.swing.JTextField();
        javax.swing.JPanel namePanel = new javax.swing.JPanel(new BorderLayout(5, 5));
        namePanel.add(new javax.swing.JLabel("Tên nhóm:"), BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);
        panel.add(namePanel, BorderLayout.NORTH);
        
        // Danh sách chọn thành viên (loại trừ bản thân)
        javax.swing.DefaultListModel<String> selectModel = new javax.swing.DefaultListModel<>();
        for (int i = 0; i < userListModel.size(); i++) {
            String nickname = userListModel.get(i);
            Long userId = userMap.get(nickname);
            // Chỉ thêm nếu không phải bản thân
            if (userId != null && userId != myId) {
                selectModel.addElement(nickname);
            }
        }
        javax.swing.JList<String> memberList = new javax.swing.JList<>(selectModel);
        memberList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(memberList);
        scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Chọn thành viên (Ctrl+Click để chọn nhiều):"));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        int result = javax.swing.JOptionPane.showConfirmDialog(this, panel, 
            "Tạo nhóm mới", javax.swing.JOptionPane.OK_CANCEL_OPTION, 
            javax.swing.JOptionPane.PLAIN_MESSAGE);
        
        if (result == javax.swing.JOptionPane.OK_OPTION) {
            String groupName = nameField.getText().trim();
            java.util.List<String> selectedMembers = memberList.getSelectedValuesList();
            
            if (groupName.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Vui lòng nhập tên nhóm!", "Lỗi", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (selectedMembers.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Vui lòng chọn ít nhất 1 thành viên!", "Lỗi", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Lấy danh sách ID của các thành viên được chọn
            StringBuilder memberIds = new StringBuilder();
            for (int i = 0; i < selectedMembers.size(); i++) {
                String nick = selectedMembers.get(i);
                Long id = userMap.get(nick);
                if (id != null) {
                    if (memberIds.length() > 0) memberIds.append(",");
                    memberIds.append(id);
                }
            }
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomName", groupName);
            data.put("memberIds", memberIds.toString());
            socket.send("CREATE_GROUP", data);
        }
    }
    
    public void showMyGroups(java.util.List<java.util.Map<String, String>> groups) {
        groupListModel.clear();
        groupMap.clear();
        
        if (groups.isEmpty()) {
            groupListModel.addElement("(Chưa có nhóm nào)");
            return;
        }
        
        for (java.util.Map<String, String> group : groups) {
            String roomId = group.get("room_id");
            String roomName = group.get("room_name");
            String memberCount = group.get("member_count");
            
            // Handle null room_name
            if (roomName == null || roomName.isEmpty()) {
                roomName = "Nhóm #" + roomId;
            }
            
            // Display format: "Room Name (3 người)"
            String displayName = roomName + " (" + memberCount + " người)";
            
            groupListModel.addElement(displayName);
            groupMap.put(displayName, Long.parseLong(roomId));
        }
    }
    
    private void showWallet() {
        if (mainFrame != null) {
            double balance = mainFrame.getMyMoney();
            javax.swing.JOptionPane.showMessageDialog(this,
                "<html><body style='width: 250px; text-align: center;'>" +
                "<h2>💰 Ví của bạn</h2>" +
                "<hr>" +
                "<h1 style='color: green;'>" + String.format("%,.0f", balance) + " VND</h1>" +
                "<hr>" +
                "<p><i>Nạp tiền: Liên hệ admin</i></p>" +
                "</body></html>",
                "Ví tiền", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void deleteAccount() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn XÓA tài khoản?\nHành động này không thể hoàn tác!",
            "Xóa tài khoản", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
            
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            socket.send("DELETE_ACCOUNT", new java.util.HashMap<>());
            // For now, let's assume we should logout immediately after sending
            logout();
        }
    }
    
    private void logout() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", 
            javax.swing.JOptionPane.YES_NO_OPTION);
            
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            if (mainFrame != null) {
                mainFrame.logout();
            }
        }
    }
}

