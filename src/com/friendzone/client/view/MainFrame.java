package com.friendzone.client.view;

import java.awt.CardLayout;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.friendzone.client.controller.ClientSocket;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private ClientSocket socket;
    private RegisterPanel registerPanel;
    private LoginPanel loginPanel;
    private LobbyPanel lobbyPanel;
    private CaroPanel caroPanel;
    private CatchHeartPanel catchHeartPanel;
    private VideoCallPanel videoCallPanel;
    private WordChainPanel wordChainPanel;
    private LoveQuizPanel loveQuizPanel;
    private DrawGuessPanel drawGuessPanel;
    private ProfilePanel profilePanel;
    private DiscoverPanel discoverPanel;
    private MatchesPanel matchesPanel;
    private long myId;
    private String myUsername;
    private double myMoney = 0.0;
    private java.util.Map<Long, DMWindow> dmWindows = new java.util.HashMap<>(); // roomId -> DMWindow
    private java.util.Map<Long, GroupChatWindow> groupWindows = new java.util.HashMap<>(); // roomId -> GroupChatWindow
    
    // File transfer: Track active receiving transfers
    private java.util.Map<String, FileReceiveSession> activeFileReceives = new java.util.HashMap<>();

    public MainFrame() {
        setTitle("Hệ thống Game Friendzone");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        socket = new ClientSocket();
        
        // Ask for Server IP
        String serverIp = JOptionPane.showInputDialog(this, "Nhập địa chỉ IP Server:", "localhost");
        if (serverIp == null || serverIp.trim().isEmpty()) System.exit(0);
        
        try {
            socket.connect(serverIp, 12345); // Default port 12345 for TCP
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server tại " + serverIp);
            System.exit(1);
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(socket, this); // Pass MainFrame reference
        registerPanel = new RegisterPanel(socket, this); // Init RegisterPanel
        lobbyPanel = new LobbyPanel(socket);
        lobbyPanel.setMainFrame(this);
        caroPanel = new CaroPanel(socket);
        caroPanel.setMainFrame(this);
        catchHeartPanel = new CatchHeartPanel();
        catchHeartPanel.setSocket(socket);
        catchHeartPanel.setMainFrame(this);
        videoCallPanel = new VideoCallPanel();
        videoCallPanel.setSocket(socket); // Set socket để gửi VIDEO_CALL_END
        videoCallPanel.setOnCallEnd(() -> cardLayout.show(mainPanel, "LOBBY"));
        wordChainPanel = new WordChainPanel();
        wordChainPanel.setSocket(socket);
        wordChainPanel.setMainFrame(this);
        loveQuizPanel = new LoveQuizPanel();
        loveQuizPanel.setSocket(socket);
        loveQuizPanel.setMainFrame(this);
        drawGuessPanel = new DrawGuessPanel();
        drawGuessPanel.setSocket(socket);
        drawGuessPanel.setMainFrame(this);
        
        // Profile, Discover, Matches panels will be created after login
        // (need user ID)

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER"); // Add to CardLayout
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(caroPanel, "CARO");
        mainPanel.add(catchHeartPanel, "CATCH_HEART");
        mainPanel.add(videoCallPanel, "VIDEO_CALL");
        mainPanel.add(wordChainPanel, "WORD_CHAIN");
        mainPanel.add(loveQuizPanel, "LOVE_QUIZ");
        mainPanel.add(drawGuessPanel, "DRAW_GUESS");

        add(mainPanel);
        
        // Listen to server messages
        socket.setMessageListener(this::onMessageReceived);
    }
    
    public void showCard(String cardName) {
        cardLayout.show(mainPanel, cardName);
    }

    private void onMessageReceived(com.friendzone.model.NetworkMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String command = msg.getCommand();
            java.util.Map<String, String> data = msg.getData();
            
            switch (command) {
                case "SUCCESS":
                    myId = Long.parseLong(data.get("id"));
                    String nickname = data.get("nickname");
                    myUsername = nickname;
                    String moneyStr = data.getOrDefault("money", "0");
                    myMoney = Double.parseDouble(moneyStr);
                    lobbyPanel.setWelcomeText("Chào mừng, " + nickname + " (ELO: " + data.get("elo") + " | Ví: " + String.format("%.0f", myMoney) + " VND)");
                    
                    // Initialize dating panels after login
                    initDatingPanels();
                    
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "FAIL":
                    JOptionPane.showMessageDialog(this, data.get("message"));
                    break;
                case "REGISTER_SUCCESS":
                    JOptionPane.showMessageDialog(this, data.get("message"));
                    cardLayout.show(mainPanel, "LOGIN");
                    break;
                case "MSG":
                    lobbyPanel.appendChat(data.get("sender") + ": " + data.get("content"));
                    break;
                case "INVITE_RECEIVED":
                    int confirm = JOptionPane.showConfirmDialog(this, 
                        data.get("inviterName") + " mời bạn chơi " + data.get("gameCode"), 
                        "Lời mời chơi game", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        java.util.Map<String, String> acceptData = new java.util.HashMap<>();
                        acceptData.put("gameCode", data.get("gameCode"));
                        acceptData.put("inviterId", data.get("inviterId"));
                        socket.send("ACCEPT_INVITE", acceptData);
                    }
                    break;
                case "START_GAME":
                    String gameCode = data.get("gameCode");
                    long sessionId = Long.parseLong(data.get("sessionId"));
                    long p1 = Long.parseLong(data.get("p1"));
                    long p2 = Long.parseLong(data.get("p2"));
                    
                    long opponentId = (p1 == myId) ? p2 : p1;
                    boolean isFirst = (p1 == myId); 
                    
                    if (gameCode.equals("CARO")) {
                        caroPanel.startGame(sessionId, myId, opponentId, isFirst);
                        cardLayout.show(mainPanel, "CARO");
                    } else if (gameCode.equals("CATCH_HEART")) {
                        catchHeartPanel.startGame();
                        cardLayout.show(mainPanel, "CATCH_HEART");
                    } else if (gameCode.equals("WORD_CHAIN")) {
                        wordChainPanel.startGame(sessionId, myId, opponentId, isFirst);
                        cardLayout.show(mainPanel, "WORD_CHAIN");
                    } else if (gameCode.equals("LOVE_QUIZ")) {
                        loveQuizPanel.startGame(sessionId, myId, opponentId);
                        cardLayout.show(mainPanel, "LOVE_QUIZ");
                    } else if (gameCode.equals("DRAW_GUESS")) {
                        // Lấy thông tin UDP để stream vẽ
                        int p1Port = Integer.parseInt(data.get("p1DrawPort"));
                        int p2Port = Integer.parseInt(data.get("p2DrawPort"));
                        String p1Name = data.get("p1Name");
                        String p2Name = data.get("p2Name");
                        String p1Ip = data.getOrDefault("p1Ip", "localhost");
                        String p2Ip = data.getOrDefault("p2Ip", "localhost");
                        
                        // Xác định port nghe, port gửi và IP dựa trên vai trò
                        int listenPort, targetPort;
                        String targetIp, opponentName;
                        if (isFirst) {
                            // Tôi là p1, nghe ở p1Port, gửi đến p2Port của p2
                            listenPort = p1Port;
                            targetPort = p2Port;
                            targetIp = p2Ip;
                            opponentName = p2Name;
                        } else {
                            // Tôi là p2, nghe ở p2Port, gửi đến p1Port của p1
                            listenPort = p2Port;
                            targetPort = p1Port;
                            targetIp = p1Ip;
                            opponentName = p1Name;
                        }
                        
                        drawGuessPanel.startGame(sessionId, myId, opponentId, opponentName, isFirst, targetIp, targetPort, listenPort);
                        cardLayout.show(mainPanel, "DRAW_GUESS");
                    }
                    break;
                case "OPPONENT_MOVE":
                    int x = Integer.parseInt(data.get("x"));
                    int y = Integer.parseInt(data.get("y"));
                    caroPanel.onOpponentMove(x, y);
                    break;
                case "ONLINE_USERS":
                    String jsonUsers = data.get("users");
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> users = gson.fromJson(jsonUsers, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    lobbyPanel.updateOnlineUsers(users);
                    break;
                case "FRIEND_REQUEST_RECEIVED":
                    int friendConfirm = JOptionPane.showConfirmDialog(this, 
                        data.get("senderName") + " muốn kết bạn với bạn!", 
                        "Lời mời kết bạn", JOptionPane.YES_NO_OPTION);
                    
                    java.util.Map<String, String> respData = new java.util.HashMap<>();
                    respData.put("senderId", data.get("senderId"));
                    respData.put("accepted", String.valueOf(friendConfirm == JOptionPane.YES_OPTION));
                    socket.send("FRIEND_RESPONSE", respData);
                    break;
                case "MATCH_RESULTS":
                    String jsonMatches = data.get("matches");
                    com.google.gson.Gson gsonMatch = new com.google.gson.Gson();
                    java.util.List<com.friendzone.model.User> matches = gsonMatch.fromJson(jsonMatches, new com.google.gson.reflect.TypeToken<java.util.List<com.friendzone.model.User>>(){}.getType());
                    lobbyPanel.showMatchResults(matches);
                    break;
                case "DM_ROOM_READY":
                    long roomId = Long.parseLong(data.get("roomId"));
                    long targetId = Long.parseLong(data.get("targetId"));
                    openDMWindow(roomId, targetId);
                    break;
                case "NEW_DM":
                    long dmRoomId = Long.parseLong(data.get("roomId"));
                    String senderName = data.get("senderName");
                    String content = data.get("content");
                    
                    DMWindow window = dmWindows.get(dmRoomId);
                    if (window != null && window.isVisible()) {
                        window.appendMessage(senderName, content);
                    } else {
                        // Show notification or open new window
                        int choice = JOptionPane.showConfirmDialog(this, 
                            "Tin nhắn mới từ " + senderName + ": " + content + "\n\nMở cuộc trò chuyện?", 
                            "Tin nhắn mới", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            long senderId = Long.parseLong(data.get("senderId"));
                            openDMWindow(dmRoomId, senderId);
                        }
                    }
                    break;
                case "NEW_IMAGE":
                    // Nhận ảnh từ người gửi
                    long imgRoomId = Long.parseLong(data.get("roomId"));
                    String imgSenderName = data.get("senderName");
                    String imgFileName = data.get("fileName");
                    String imageData = data.get("imageData");
                    
                    DMWindow imgWindow = dmWindows.get(imgRoomId);
                    if (imgWindow != null && imgWindow.isVisible()) {
                        // Hiển thị ảnh trong cửa sổ chat
                        imgWindow.appendImageFromBase64(imgSenderName, imageData, false);
                    } else {
                        // Thong bao co anh moi
                        int imgChoice = JOptionPane.showConfirmDialog(this, 
                            "[Anh] Anh moi tu " + imgSenderName + ": " + imgFileName + "\n\nMo cuoc tro chuyen?", 
                            "Anh moi", JOptionPane.YES_NO_OPTION);
                        if (imgChoice == JOptionPane.YES_OPTION) {
                            long imgSenderId = Long.parseLong(data.get("senderId"));
                            openDMWindow(imgRoomId, imgSenderId);
                            // Sau khi mở window, hiển thị ảnh
                            DMWindow newImgWin = dmWindows.get(imgRoomId);
                            if (newImgWin != null) {
                                newImgWin.appendImageFromBase64(imgSenderName, imageData, false);
                            }
                        }
                    }
                    break;
                case "IMAGE_SENT":
                    // Xác nhận ảnh đã gửi thành công (không cần làm gì thêm)
                    break;
                case "NEW_STICKER":
                    // Nhận sticker từ người gửi
                    long stickerRoomId = Long.parseLong(data.get("roomId"));
                    String stickerSenderName = data.get("senderName");
                    String stickerFileName = data.get("fileName");
                    String stickerData = data.get("stickerData");
                    
                    DMWindow stickerWindow = dmWindows.get(stickerRoomId);
                    if (stickerWindow != null && stickerWindow.isVisible()) {
                        // Hiển thị sticker trong cửa sổ chat
                        stickerWindow.appendStickerFromBase64(stickerSenderName, stickerData, false);
                    } else {
                        // Thông báo có sticker mới
                        int stickerChoice = JOptionPane.showConfirmDialog(this, 
                            "[Sticker] Sticker moi tu " + stickerSenderName + "!\n\nMo cuoc tro chuyen?", 
                            "Sticker moi", JOptionPane.YES_NO_OPTION);
                        if (stickerChoice == JOptionPane.YES_OPTION) {
                            long stickerSenderId = Long.parseLong(data.get("senderId"));
                            openDMWindow(stickerRoomId, stickerSenderId);
                            // Sau khi mở window, hiển thị sticker
                            DMWindow newStickerWin = dmWindows.get(stickerRoomId);
                            if (newStickerWin != null) {
                                newStickerWin.appendStickerFromBase64(stickerSenderName, stickerData, false);
                            }
                        }
                    }
                    break;
                case "NEW_VOICE":
                    // Nhận voice message từ người gửi
                    long voiceRoomId = Long.parseLong(data.get("roomId"));
                    String voiceSenderName = data.get("senderName");
                    int voiceDuration = Integer.parseInt(data.get("duration"));
                    String voiceData = data.get("voiceData"); // Base64 audio data
                    
                    DMWindow voiceWindow = dmWindows.get(voiceRoomId);
                    if (voiceWindow != null && voiceWindow.isVisible()) {
                        voiceWindow.appendVoiceMessage(voiceSenderName, voiceDuration, voiceData, false);
                    } else {
                        int voiceChoice = JOptionPane.showConfirmDialog(this, 
                            "Tin nhắn thoại mới từ " + voiceSenderName + " (" + voiceDuration + "s)!\n\nMở cuộc trò chuyện?", 
                            "Voice message", JOptionPane.YES_NO_OPTION);
                        if (voiceChoice == JOptionPane.YES_OPTION) {
                            long voiceSenderId = Long.parseLong(data.get("senderId"));
                            openDMWindow(voiceRoomId, voiceSenderId);
                            DMWindow newVoiceWin = dmWindows.get(voiceRoomId);
                            if (newVoiceWin != null) {
                                newVoiceWin.appendVoiceMessage(voiceSenderName, voiceDuration, voiceData, false);
                            }
                        }
                    }
                    break;
                case "STICKER_SENT":
                    // Xác nhận sticker đã gửi thành công (không cần làm gì thêm)
                    break;
                case "DM_HISTORY":
                    long histRoomId = Long.parseLong(data.get("roomId"));
                    String jsonMsgs = data.get("messages");
                    com.google.gson.Gson gsonHist = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> messages = gsonHist.fromJson(jsonMsgs, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    
                    DMWindow dmWin = dmWindows.get(histRoomId);
                    if (dmWin != null) {
                        dmWin.loadHistory(messages);
                    }
                    break;
                case "LEADERBOARD_DATA":
                    String lbGameCode = data.get("gameCode");
                    String jsonLb = data.get("leaderboard");
                    com.google.gson.Gson gsonLb = new com.google.gson.Gson();
                    java.util.List<com.friendzone.model.User> topPlayers = gsonLb.fromJson(jsonLb, new com.google.gson.reflect.TypeToken<java.util.List<com.friendzone.model.User>>(){}.getType());
                    lobbyPanel.showLeaderboard(lbGameCode, topPlayers);
                    break;
                case "NOTIFICATIONS_DATA":
                    String jsonNotifs = data.get("notifications");
                    int notifCount = Integer.parseInt(data.get("count"));
                    com.google.gson.Gson gsonNotif = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> notifications = gsonNotif.fromJson(jsonNotifs, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    lobbyPanel.showNotifications(notifications, notifCount);
                    break;
                case "CALL_HISTORY_DATA":
                    String jsonCalls = data.get("calls");
                    com.google.gson.Gson gsonCalls = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> calls = gsonCalls.fromJson(jsonCalls, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    lobbyPanel.showCallHistory(calls);
                    break;
                case "GROUP_CREATED":
                    long newRoomId = Long.parseLong(data.get("roomId"));
                    String newRoomName = data.get("roomName");
                    openGroupWindow(newRoomId, newRoomName);
                    JOptionPane.showMessageDialog(this, "Nhóm '" + newRoomName + "' đã được tạo!");
                    // Reload group list
                    socket.send("GET_MY_GROUPS", new java.util.HashMap<>());
                    break;
                case "NEW_GROUP_MSG":
                    long groupRoomId = Long.parseLong(data.get("roomId"));
                    String groupSender = data.get("senderName");
                    String groupContent = data.get("content");
                    
                    GroupChatWindow groupWin = groupWindows.get(groupRoomId);
                    if (groupWin != null && groupWin.isVisible()) {
                        groupWin.appendMessage(groupSender, groupContent);
                    }
                    break;
                case "GROUP_HISTORY":
                    long histGroupId = Long.parseLong(data.get("roomId"));
                    String jsonGroupMsgs = data.get("messages");
                    com.google.gson.Gson gsonGroup = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> groupMessages = gsonGroup.fromJson(jsonGroupMsgs, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    
                    GroupChatWindow groupHistWin = groupWindows.get(histGroupId);
                    if (groupHistWin != null) {
                        groupHistWin.loadHistory(groupMessages);
                    }
                    break;
                case "MY_GROUPS_DATA":
                    String jsonGroups = data.get("groups");
                    com.google.gson.Gson gsonMyGroups = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> myGroups = gsonMyGroups.fromJson(jsonGroups, new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    lobbyPanel.showMyGroups(myGroups);
                    break;
                case "GAME_ENDED":
                    long endSessionId = Long.parseLong(data.get("sessionId"));
                    long winnerId = Long.parseLong(data.get("winnerId"));
                    String reason = data.getOrDefault("reason", "NORMAL");
                    
                    String endMsg = (winnerId == myId) ? "CHIEN THANG!" : "THUA CUOC!";
                    if (reason.equals("SURRENDER")) {
                        endMsg += "\n(Đối thủ đã đầu hàng)";
                    } else if (reason.equals("DISCONNECT")) {
                        endMsg += "\n(Đối thủ mất kết nối)";
                    }
                    
                    JOptionPane.showMessageDialog(this, endMsg, "Kết thúc game", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "VIDEO_CALL_REQUEST":
                    String callerName = data.get("callerName");
                    long callerId = Long.parseLong(data.get("callerId"));
                    long callId = Long.parseLong(data.get("callId"));
                    
                    int callChoice = JOptionPane.showConfirmDialog(this, 
                        callerName + " đang gọi video cho bạn!\n\nChấp nhận cuộc gọi?", 
                        "Cuộc gọi video đến", JOptionPane.YES_NO_OPTION);
                    
                    java.util.Map<String, String> callResp = new java.util.HashMap<>();
                    callResp.put("callId", String.valueOf(callId));
                    callResp.put("callerId", String.valueOf(callerId));
                    callResp.put("accepted", String.valueOf(callChoice == JOptionPane.YES_OPTION));
                    socket.send("VIDEO_CALL_RESPONSE", callResp);
                    break;
                case "INCOMING_CALL":
                    // Cuộc gọi đến (alias cho VIDEO_CALL_REQUEST)
                    String inCallerName = data.get("callerName");
                    long inCallerId = Long.parseLong(data.get("callerId"));
                    long inCallId = Long.parseLong(data.get("callId"));
                    String inCallType = data.getOrDefault("callType", "VIDEO");
                    
                    String callTypeText = inCallType.equals("AUDIO") ? "thoại" : "video";
                    int inCallChoice = JOptionPane.showConfirmDialog(this, 
                        inCallerName + " đang gọi " + callTypeText + " cho bạn!\n\nChấp nhận cuộc gọi?", 
                        "Cuộc gọi " + callTypeText + " đến", JOptionPane.YES_NO_OPTION);
                    
                    java.util.Map<String, String> inCallResp = new java.util.HashMap<>();
                    inCallResp.put("callId", String.valueOf(inCallId));
                    inCallResp.put("callerId", String.valueOf(inCallerId));
                    inCallResp.put("accepted", String.valueOf(inCallChoice == JOptionPane.YES_OPTION));
                    socket.send("VIDEO_CALL_RESPONSE", inCallResp);
                    break;
                case "CALL_INITIATED":
                    // Cuộc gọi đang đổ chuông
                    JOptionPane.showMessageDialog(this, "Đang gọi... Chờ người nhận trả lời.", 
                        "Đang gọi", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "CALL_ACCEPTED":
                    // Người nhận chấp nhận cuộc gọi - BÊN GỌI nhận được
                    {
                        String acceptTargetIp = data.get("targetIp");
                        String acceptTargetPortStr = data.get("targetPort");
                        String acceptListenPortStr = data.get("listenPort");
                        long acceptCallId = Long.parseLong(data.get("callId"));
                        long acceptTargetId = Long.parseLong(data.get("targetId"));
                        String acceptCallType = data.getOrDefault("callType", "VIDEO");
                        
                        if (acceptTargetIp != null && acceptTargetPortStr != null && acceptListenPortStr != null) {
                            int acceptTargetPort = Integer.parseInt(acceptTargetPortStr);
                            int acceptListenPort = Integer.parseInt(acceptListenPortStr);
                            
                            videoCallPanel.setCallInfo(acceptCallId, acceptTargetId);
                            videoCallPanel.setVideoCall(acceptCallType.equals("VIDEO"));
                            videoCallPanel.startCall(acceptTargetIp, acceptTargetPort, acceptListenPort);
                            cardLayout.show(mainPanel, "VIDEO_CALL");
                        } else {
                            JOptionPane.showMessageDialog(this, "Cuộc gọi đã được kết nối!", 
                                "Đã kết nối", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                    break;
                case "CALL_STARTED":
                    // Cuộc gọi bắt đầu - BÊN NHẬN nhận được
                    {
                        String startTargetIp = data.get("targetIp");
                        String startTargetPortStr = data.get("targetPort");
                        String startListenPortStr = data.get("listenPort");
                        long startCallId = Long.parseLong(data.get("callId"));
                        long startPeerId = Long.parseLong(data.get("peerId"));
                        String startCallType = data.getOrDefault("callType", "VIDEO");
                        
                        if (startTargetIp != null && startTargetPortStr != null && startListenPortStr != null) {
                            int startTargetPort = Integer.parseInt(startTargetPortStr);
                            int startListenPort = Integer.parseInt(startListenPortStr);
                            
                            videoCallPanel.setCallInfo(startCallId, startPeerId);
                            videoCallPanel.setVideoCall(startCallType.equals("VIDEO"));
                            videoCallPanel.startCall(startTargetIp, startTargetPort, startListenPort);
                            cardLayout.show(mainPanel, "VIDEO_CALL");
                        } else {
                            JOptionPane.showMessageDialog(this, "Cuộc gọi đã bắt đầu!", 
                                "Đã kết nối", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                    break;
                case "CALL_REJECTED":
                    JOptionPane.showMessageDialog(this, 
                        data.getOrDefault("message", "Cuộc gọi đã bị từ chối."), 
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "VIDEO_CALL_ACCEPTED":
                    // Cuộc gọi được chấp nhận - bắt đầu video
                    String targetIp = data.get("targetIp");
                    int targetPort = Integer.parseInt(data.get("targetPort"));
                    int listenPort = Integer.parseInt(data.get("listenPort"));
                    long vcCallId = Long.parseLong(data.get("callId"));
                    long vcOtherId = Long.parseLong(data.get("otherId"));
                    String vcCallType = data.getOrDefault("callType", "VIDEO");
                    
                    videoCallPanel.setCallInfo(vcCallId, vcOtherId);
                    videoCallPanel.setVideoCall(vcCallType.equals("VIDEO"));
                    videoCallPanel.startCall(targetIp, targetPort, listenPort);
                    cardLayout.show(mainPanel, "VIDEO_CALL");
                    break;
                case "VIDEO_CALL_REJECTED":
                    JOptionPane.showMessageDialog(this, "Cuộc gọi đã bị từ chối.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "VIDEO_CALL_ENDED":
                    videoCallPanel.stopCall();
                    JOptionPane.showMessageDialog(this, "Cuộc gọi đã kết thúc.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "CALL_ENDED":
                    // Cuộc gọi bị kết thúc bởi đối phương
                    String endedBy = data.getOrDefault("endedBy", "Đối phương");
                    String endReason = data.getOrDefault("reason", "ENDED");
                    videoCallPanel.stopCall();
                    JOptionPane.showMessageDialog(this, 
                        endedBy + " đã kết thúc cuộc gọi. (" + endReason + ")", 
                        "Cuộc gọi kết thúc", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "CALL_END_CONFIRMED":
                    // Xác nhận kết thúc cuộc gọi
                    videoCallPanel.stopCall();
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                    
                // ========== WORD CHAIN GAME ==========
                case "WORD_CHAIN_WORD":
                    // Nhận từ mới trong game nối từ
                    String word = data.get("word");
                    long wordSenderId = Long.parseLong(data.get("senderId"));
                    boolean wordValid = Boolean.parseBoolean(data.getOrDefault("valid", "true"));
                    wordChainPanel.onWordReceived(word, wordSenderId, wordValid);
                    break;
                case "WORD_CHAIN_END":
                    // Kết thúc game nối từ
                    long wordWinnerId = Long.parseLong(data.getOrDefault("winnerId", "0"));
                    String wordReason = data.getOrDefault("reason", "");
                    wordChainPanel.onGameEnd(wordWinnerId, wordReason);
                    break;
                    
                // ========== LOVE QUIZ GAME ==========
                case "QUIZ_QUESTION":
                    // Nhận câu hỏi quiz
                    int qNum = Integer.parseInt(data.get("questionNum"));
                    String question = data.get("question");
                    String[] answers = new String[4];
                    answers[0] = data.get("answer0");
                    answers[1] = data.get("answer1");
                    answers[2] = data.get("answer2");
                    answers[3] = data.get("answer3");
                    loveQuizPanel.showQuestion(qNum, question, answers);
                    break;
                case "QUIZ_RESULT":
                    // Kết quả sau mỗi câu
                    int myAns = Integer.parseInt(data.get("myAnswer"));
                    int oppAns = Integer.parseInt(data.get("opponentAnswer"));
                    boolean matched = Boolean.parseBoolean(data.get("matched"));
                    loveQuizPanel.showResult(myAns, oppAns, matched);
                    break;
                case "QUIZ_END":
                    // Kết thúc quiz
                    int finalScore = Integer.parseInt(data.get("score"));
                    int maxScore = Integer.parseInt(data.get("maxScore"));
                    loveQuizPanel.onGameEnd(finalScore, maxScore);
                    break;
                    
                // ========== DRAW GUESS GAME ==========
                case "DRAW_HINT":
                    // Nhận hint từ người vẽ (số ký tự)
                    int hintLength = Integer.parseInt(data.get("hint"));
                    drawGuessPanel.onHintReceived(hintLength);
                    break;
                case "DRAW_GUESS_RESULT":
                    // Kết quả đoán từ
                    String guess = data.get("guess");
                    boolean correct = Boolean.parseBoolean(data.get("correct"));
                    boolean guessIsMe = Long.parseLong(data.get("guesserId")) == myId;
                    drawGuessPanel.onGuessResult(guess, correct, guessIsMe);
                    break;
                case "DRAW_OPPONENT_GUESS":
                    // Người khác đoán (cho người vẽ thấy)
                    String oppGuess = data.get("guess");
                    drawGuessPanel.onOpponentGuess(oppGuess);
                    break;
                case "DRAW_GAME_ENDED":
                    // Đối phương thoát
                    JOptionPane.showMessageDialog(this, "Đối phương đã thoát game!");
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                    
                case "WALLET_UPDATE":
                    double newBalance = Double.parseDouble(data.get("balance"));
                    myMoney = newBalance;
                    String currentNick = data.getOrDefault("nickname", "");
                    String currentElo = data.getOrDefault("elo", "1000");
                    lobbyPanel.setWelcomeText("Chào mừng, " + currentNick + " (ELO: " + currentElo + " | Ví: " + String.format("%.0f", myMoney) + " VND)");
                    break;
                case "LOGOUT_SUCCESS":
                    myId = 0;
                    myMoney = 0.0;
                    cardLayout.show(mainPanel, "LOGIN");
                    break;
                case "REMATCH_REQUEST":
                    // Đối thủ muốn chơi lại
                    long rematchFromId = Long.parseLong(data.get("fromUserId"));
                    String rematchFromName = data.get("fromUserName");
                    String rematchGameType = data.get("gameType");
                    
                    if (rematchGameType.equals("CARO")) {
                        caroPanel.onRematchRequest(rematchFromId);
                    } else {
                        // Cho các game khác, hiển thị dialog thông thường
                        int rematchChoice = JOptionPane.showConfirmDialog(this,
                            rematchFromName + " muốn chơi lại!\nBạn có đồng ý không?",
                            "Yêu cầu chơi lại",
                            JOptionPane.YES_NO_OPTION);
                        
                        java.util.Map<String, String> rematchResp = new java.util.HashMap<>();
                        rematchResp.put("opponentId", String.valueOf(rematchFromId));
                        rematchResp.put("gameType", rematchGameType);
                        rematchResp.put("accepted", String.valueOf(rematchChoice == JOptionPane.YES_OPTION));
                        socket.send("REMATCH_RESPONSE", rematchResp);
                    }
                    break;
                case "REMATCH_ACCEPTED":
                    // Đối thủ đồng ý chơi lại
                    long newRematchSessionId = Long.parseLong(data.get("newSessionId"));
                    String acceptedGameType = data.get("gameType");
                    long remP1 = Long.parseLong(data.get("p1"));
                    long remP2 = Long.parseLong(data.get("p2"));
                    
                    long remOpponentId = (remP1 == myId) ? remP2 : remP1;
                    boolean remIsFirst = (remP1 == myId);
                    
                    if (acceptedGameType.equals("CARO")) {
                        caroPanel.onRematchResponse(true, newRematchSessionId);
                    } else if (acceptedGameType.equals("CATCH_HEART")) {
                        catchHeartPanel.startGame();
                        cardLayout.show(mainPanel, "CATCH_HEART");
                    }
                    break;
                case "REMATCH_REJECTED":
                    // Đối thủ từ chối chơi lại
                    String rejectMsg = data.get("message");
                    JOptionPane.showMessageDialog(this, rejectMsg != null ? rejectMsg : "Đối thủ từ chối chơi lại.",
                        "Từ chối", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "REMATCH_PENDING":
                    // Đang chờ đối thủ trả lời
                    // Không cần làm gì, CaroPanel đã hiển thị trạng thái chờ
                    break;
                case "FILE_SENT":
                    // File đã được gửi thành công
                    String sentFileName = data.get("fileName");
                    // Không cần thông báo vì đã hiển thị trong chat
                    break;
                case "FILE_TRANSFER_START":
                    // Bắt đầu nhận file với chunked transfer
                    handleFileTransferStart(data);
                    break;
                case "FILE_CHUNK":
                    // Nhận chunk của file
                    handleFileChunk(data);
                    break;
                case "FILE_TRANSFER_COMPLETE":
                    // File transfer hoàn tất
                    handleFileTransferComplete(data);
                    break;
                case "NEW_FILE":
                    // Nhận file mới trong DM (old method - for backward compatibility)
                    long fileRoomId = Long.parseLong(data.get("roomId"));
                    String fileSenderName = data.get("senderName");
                    String fileContent = data.get("content");
                    String fileName = data.get("fileName");
                    String fileDataBase64 = data.get("fileData");
                    
                    DMWindow fileDmWin = dmWindows.get(fileRoomId);
                    if (fileDmWin != null && fileDmWin.isVisible()) {
                        fileDmWin.appendFileMessage(fileSenderName, fileName, fileDataBase64, false);
                    } else {
                        int saveChoice = JOptionPane.showConfirmDialog(this, 
                            "[File] " + fileSenderName + " da gui file: " + fileName + "\n\nBan co muon luu file?", 
                            "File mới", JOptionPane.YES_NO_OPTION);
                        if (saveChoice == JOptionPane.YES_OPTION) {
                            saveReceivedFile(fileName, fileDataBase64);
                        }
                    }
                    break;
                case "NEW_GROUP_FILE":
                    // Nhận file mới trong nhóm
                    long groupFileRoomId = Long.parseLong(data.get("roomId"));
                    String groupFileSender = data.get("senderName");
                    String groupFileContent = data.get("content");
                    
                    GroupChatWindow groupFileWin = groupWindows.get(groupFileRoomId);
                    if (groupFileWin != null && groupFileWin.isVisible()) {
                        groupFileWin.appendMessage(groupFileSender, groupFileContent);
                    }
                    break;
                case "INCOMING_GROUP_CALL":
                    // Cuộc gọi nhóm đến
                    String groupCallRoom = data.get("roomName");
                    String groupCallerName = data.get("callerName");
                    
                    JOptionPane.showMessageDialog(this, 
                        groupCallerName + " đang bắt đầu cuộc gọi nhóm trong '" + groupCallRoom + "'", 
                        "Cuộc gọi nhóm", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "GROUP_CALL_INITIATED":
                    JOptionPane.showMessageDialog(this, "Đã bắt đầu cuộc gọi nhóm!", 
                        "Cuộc gọi nhóm", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "ADDED_TO_GROUP":
                    String addedRoomName = data.get("roomName");
                    String addedBy = data.get("addedBy");
                    long addedRoomId = Long.parseLong(data.get("roomId"));
                    
                    JOptionPane.showMessageDialog(this, 
                        addedBy + " đã thêm bạn vào nhóm '" + addedRoomName + "'", 
                        "Thêm vào nhóm", JOptionPane.INFORMATION_MESSAGE);
                    openGroupWindow(addedRoomId, addedRoomName);
                    // Reload group list
                    socket.send("GET_MY_GROUPS", new java.util.HashMap<>());
                    break;
                case "GROUP_MEMBER_ADDED":
                    String memberAddedMsg = data.get("message");
                    long memberAddedRoomId = Long.parseLong(data.get("roomId"));
                    
                    GroupChatWindow memberWin = groupWindows.get(memberAddedRoomId);
                    if (memberWin != null && memberWin.isVisible()) {
                        memberWin.appendMessage("Hệ thống", memberAddedMsg, false);
                    }
                    break;
                case "GROUP_MEMBER_LEFT":
                    String memberLeftMsg = data.get("message");
                    long memberLeftRoomId = Long.parseLong(data.get("roomId"));
                    
                    GroupChatWindow leftWin = groupWindows.get(memberLeftRoomId);
                    if (leftWin != null && leftWin.isVisible()) {
                        leftWin.appendMessage("Hệ thống", memberLeftMsg, false);
                    }
                    break;
                case "LEFT_GROUP":
                    // User successfully left group - reload group list
                    socket.send("GET_MY_GROUPS", new java.util.HashMap<>());
                    break;
                case "MEMBER_ADDED":
                    // Thành viên đã được thêm thành công
                    JOptionPane.showMessageDialog(this, "Đã thêm thành viên thành công!", 
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "GROUP_MEMBERS_DATA":
                    // Hiển thị danh sách thành viên nhóm
                    String jsonMembers = data.get("members");
                    com.google.gson.Gson gsonMembers = new com.google.gson.Gson();
                    java.util.List<java.util.Map<String, String>> membersList = gsonMembers.fromJson(jsonMembers, 
                        new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType());
                    
                    StringBuilder memberInfo = new StringBuilder("Thành viên nhóm:\n\n");
                    for (java.util.Map<String, String> member : membersList) {
                        String online = "true".equals(member.get("isOnline")) ? " 🟢" : " ⚫";
                        memberInfo.append("• ").append(member.get("nickname")).append(online).append("\n");
                    }
                    JOptionPane.showMessageDialog(this, memberInfo.toString(), 
                        "Danh sách thành viên", JOptionPane.INFORMATION_MESSAGE);
                    break;
                    
                // ========== DATING/MATCHING FEATURES ==========
                case "PROFILE_DATA":
                    if (profilePanel != null) {
                        profilePanel.handleProfileData(msg);
                    }
                    break;
                case "PROFILE_UPDATED":
                    JOptionPane.showMessageDialog(this, "Hồ sơ đã được cập nhật!", 
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "DISCOVER_USERS":
                    if (discoverPanel != null) {
                        discoverPanel.handleDiscoverUsers(msg);
                    }
                    break;
                case "SWIPE_RECORDED":
                    if (discoverPanel != null) {
                        discoverPanel.handleSwipeRecorded(msg);
                    }
                    break;
                case "NEW_MATCH":
                    if (discoverPanel != null) {
                        discoverPanel.handleNewMatch(msg);
                    }
                    if (matchesPanel != null) {
                        matchesPanel.handleNewMatch(msg);
                    }
                    break;
                case "MATCHES_DATA":
                    if (matchesPanel != null) {
                        matchesPanel.handleMatchesResponse(msg);
                    }
                    break;
                case "COMPATIBILITY_LIST_DATA":
                    if (matchesPanel != null) {
                        matchesPanel.handleCompatibilityResponse(msg);
                    }
                    break;
                case "COMPATIBILITY_UPDATED":
                    if (matchesPanel != null) {
                        matchesPanel.handleCompatibilityUpdate(msg);
                    }
                    // Also show notification
                    String compatPartner = data.getOrDefault("partnerUsername", "");
                    int compatScore = Integer.parseInt(data.getOrDefault("newScore", "0"));
                    JOptionPane.showMessageDialog(this, 
                        "Độ tương thích với " + compatPartner + " đã cập nhật: " + compatScore + "%",
                        "Cập nhật tương thích", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "USER_PROFILE_VIEW":
                    // Show profile in dialog
                    showUserProfileDialog(msg);
                    break;
            }
        });
    }
    
    private void saveReceivedFile(String fileName, String fileDataBase64) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file");
        chooser.setSelectedFile(new java.io.File(fileName));
        int result = chooser.showSaveDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] fileBytes = java.util.Base64.getDecoder().decode(fileDataBase64);
                java.nio.file.Files.write(chooser.getSelectedFile().toPath(), fileBytes);
                JOptionPane.showMessageDialog(this, 
                    "[OK] Da luu file: " + chooser.getSelectedFile().getName(), 
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "[Loi] Khong the luu file: " + e.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void openDMWindow(long roomId, long otherId) {
        if (dmWindows.containsKey(roomId) && dmWindows.get(roomId).isVisible()) {
            dmWindows.get(roomId).toFront();
            return;
        }
        
        // Find nickname from online users (simplified - should query from server)
        String otherNickname = "User#" + otherId;
        
        DMWindow window = new DMWindow(socket, roomId, myId, otherId, otherNickname);
        dmWindows.put(roomId, window);
        window.setVisible(true);
    }
    
    public void openGroupWindow(long roomId, String roomName) {
        if (groupWindows.containsKey(roomId) && groupWindows.get(roomId).isVisible()) {
            groupWindows.get(roomId).toFront();
            return;
        }
        GroupChatWindow window = new GroupChatWindow(socket, roomId, myId, roomName);
        groupWindows.put(roomId, window);
        window.setVisible(true);
    }
    
    public void logout() {
        // Close all DM windows
        for (DMWindow dmWindow : dmWindows.values()) {
            if (dmWindow != null && dmWindow.isVisible()) {
                dmWindow.dispose();
            }
        }
        dmWindows.clear();
        
        // Close all group chat windows
        for (GroupChatWindow groupWindow : groupWindows.values()) {
            if (groupWindow != null && groupWindow.isVisible()) {
                groupWindow.dispose();
            }
        }
        groupWindows.clear();
        
        // Send logout message to server
        java.util.Map<String, String> logoutData = new java.util.HashMap<>();
        socket.send("LOGOUT", logoutData);
        
        // Reset user state
        myId = 0;
        myMoney = 0.0;
        
        // Show login panel
        cardLayout.show(mainPanel, "LOGIN");
    }
    
    public long getMyId() {
        return myId;
    }
    
    public String getMyUsername() {
        return myUsername;
    }
    
    public double getMyMoney() {
        return myMoney;
    }
    
    public ClientSocket getSocket() {
        return socket;
    }
    
    private void initDatingPanels() {
        // Remove old panels if exist
        if (profilePanel != null) {
            mainPanel.remove(profilePanel);
        }
        if (discoverPanel != null) {
            mainPanel.remove(discoverPanel);
        }
        if (matchesPanel != null) {
            mainPanel.remove(matchesPanel);
        }
        
        // Create new panels with user info
        profilePanel = new ProfilePanel(socket, (int) myId, myUsername);
        profilePanel.setMainFrame(this);
        discoverPanel = new DiscoverPanel(socket, (int) myId, myUsername);
        discoverPanel.setMainFrame(this);
        matchesPanel = new MatchesPanel(socket, (int) myId, myUsername);
        matchesPanel.setMainFrame(this);
        
        // Add to card layout
        mainPanel.add(profilePanel, "PROFILE");
        mainPanel.add(discoverPanel, "DISCOVER");
        mainPanel.add(matchesPanel, "MATCHES");
        
        mainPanel.revalidate();
    }
    
    private void showUserProfileDialog(com.friendzone.model.NetworkMessage msg) {
        java.util.Map<String, String> data = msg.getData();
        
        String username = data.get("username");
        String bio = data.getOrDefault("bio", "Chưa có bio");
        int age = Integer.parseInt(data.getOrDefault("age", "0"));
        String location = data.getOrDefault("location", "Chua co dia chi");
        String interests = data.getOrDefault("interests", "");
        int compatibility = Integer.parseInt(data.getOrDefault("compatibility", "0"));
        
        StringBuilder profileInfo = new StringBuilder();
        profileInfo.append("[User] ").append(username).append("\n\n");
        if (age > 0) profileInfo.append("Tuoi: ").append(age).append("\n");
        profileInfo.append("Dia chi: ").append(location).append("\n\n");
        profileInfo.append("Bio:\n").append(bio).append("\n\n");
        if (!interests.isEmpty()) {
            profileInfo.append("So thich: ").append(interests).append("\n\n");
        }
        profileInfo.append("Do tuong thich: ").append(compatibility).append("%");
        
        JOptionPane.showMessageDialog(this, profileInfo.toString(),
            "Ho so cua " + username, JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ========== FILE TRANSFER PROTOCOL HANDLERS ==========
    
    /**
     * Inner class để track file đang nhận
     */
    private static class FileReceiveSession {
        String transferId;
        long roomId;
        String fileName;
        long fileSize;
        int totalChunks;
        String senderName;
        java.util.Map<Integer, byte[]> receivedChunks = new java.util.HashMap<>();
        javax.swing.JDialog progressDialog;
        javax.swing.JProgressBar progressBar;
        javax.swing.JLabel statusLabel;
        
        public boolean isComplete() {
            return receivedChunks.size() == totalChunks;
        }
    }
    
    /**
     * Handle FILE_TRANSFER_START - tạo progress dialog cho file đang nhận
     */
    private void handleFileTransferStart(java.util.Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            String transferId = data.get("transferId");
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            long fileSize = Long.parseLong(data.get("fileSize"));
            int totalChunks = Integer.parseInt(data.get("totalChunks"));
            String senderName = data.get("senderName");
            
            // Create receive session
            FileReceiveSession session = new FileReceiveSession();
            session.transferId = transferId;
            session.roomId = roomId;
            session.fileName = fileName;
            session.fileSize = fileSize;
            session.totalChunks = totalChunks;
            session.senderName = senderName;
            
            // Create progress dialog
            session.progressDialog = new javax.swing.JDialog(this, "Đang nhận file", false);
            session.progressDialog.setSize(450, 150);
            session.progressDialog.setLocationRelativeTo(this);
            session.progressDialog.setLayout(new java.awt.BorderLayout(10, 10));
            
            javax.swing.JPanel contentPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 10));
            contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            javax.swing.JLabel fileLabel = new javax.swing.JLabel("📥 " + fileName + " từ " + senderName);
            fileLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
            contentPanel.add(fileLabel, java.awt.BorderLayout.NORTH);
            
            session.progressBar = new javax.swing.JProgressBar(0, 100);
            session.progressBar.setStringPainted(true);
            session.progressBar.setString("0%");
            session.progressBar.setPreferredSize(new java.awt.Dimension(400, 30));
            contentPanel.add(session.progressBar, java.awt.BorderLayout.CENTER);
            
            session.statusLabel = new javax.swing.JLabel("Đang nhận...");
            session.statusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            contentPanel.add(session.statusLabel, java.awt.BorderLayout.SOUTH);
            
            session.progressDialog.add(contentPanel);
            session.progressDialog.setVisible(true);
            
            activeFileReceives.put(transferId, session);
        });
    }
    
    /**
     * Handle FILE_CHUNK - update progress
     */
    private void handleFileChunk(java.util.Map<String, String> data) {
        String transferId = data.get("transferId");
        int chunkIndex = Integer.parseInt(data.get("chunkIndex"));
        String chunkDataB64 = data.get("chunkData");
        
        FileReceiveSession session = activeFileReceives.get(transferId);
        if (session == null) return;
        
        // Decode and store chunk
        byte[] chunkData = java.util.Base64.getDecoder().decode(chunkDataB64);
        session.receivedChunks.put(chunkIndex, chunkData);
        
        // Update UI
        SwingUtilities.invokeLater(() -> {
            int percent = (chunkIndex + 1) * 100 / session.totalChunks;
            session.progressBar.setValue(percent);
            session.progressBar.setString(percent + "%");
            session.statusLabel.setText("Chunk " + (chunkIndex + 1) + "/" + session.totalChunks);
        });
    }
    
    /**
     * Handle FILE_TRANSFER_COMPLETE - assemble file and show in chat
     */
    private void handleFileTransferComplete(java.util.Map<String, String> data) {
        String transferId = data.get("transferId");
        String fileName = data.get("fileName");
        long fileSize = Long.parseLong(data.get("fileSize"));
        long roomId = Long.parseLong(data.get("roomId"));
        
        FileReceiveSession session = activeFileReceives.get(transferId);
        if (session == null || !session.isComplete()) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Assemble file from chunks
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                for (int i = 0; i < session.totalChunks; i++) {
                    byte[] chunk = session.receivedChunks.get(i);
                    if (chunk != null) {
                        baos.write(chunk);
                    }
                }
                byte[] fileData = baos.toByteArray();
                String base64Data = java.util.Base64.getEncoder().encodeToString(fileData);
                
                // Update progress
                session.progressBar.setValue(100);
                session.progressBar.setString("✓ Hoàn thành!");
                session.statusLabel.setText("File đã nhận thành công");
                
                // Show in chat
                DMWindow dmWin = dmWindows.get(roomId);
                if (dmWin != null && dmWin.isVisible()) {
                    dmWin.appendFileMessage(session.senderName, fileName, base64Data, false);
                }
                
                // Close dialog after 1 second
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        SwingUtilities.invokeLater(() -> {
                            session.progressDialog.dispose();
                            activeFileReceives.remove(transferId);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Lỗi khi nhận file: " + e.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                session.progressDialog.dispose();
                activeFileReceives.remove(transferId);
            }
        });
    }

    /**
     * Method to append folder message in DMWindow
     */
    public void appendFolderMessage(long roomId, String senderName, String folderName, 
                                    int totalFiles, long totalSize, String folderPath) {
        DMWindow dmWin = dmWindows.get(roomId);
        if (dmWin != null && dmWin.isVisible()) {
            dmWin.appendFolderMessage(senderName, folderName, totalFiles, totalSize, folderPath, false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
