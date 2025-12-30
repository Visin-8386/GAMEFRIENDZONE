package com.friendzone.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.friendzone.dao.GameDAO;
import com.friendzone.dao.UserDAO;
import com.friendzone.model.User;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;
    private UserDAO userDAO;
    
    // Heartbeat: Track last ping time
    private volatile long lastPingTime = System.currentTimeMillis();
    private static final long PING_TIMEOUT = 60000; // 60 seconds
    
    // File transfer: Track active transfers
    private java.util.Map<String, FileTransferSession> activeTransfers = new java.util.concurrent.ConcurrentHashMap<>();
    
    private GameDAO gameDAO;
    private com.friendzone.dao.ChatDAO chatDAO;
    private com.friendzone.dao.StatsDAO statsDAO;
    private com.friendzone.dao.NotificationDAO notificationDAO;
    private com.friendzone.dao.CallDAO callDAO;
    private com.friendzone.dao.StickerDAO stickerDAO;
    private com.friendzone.dao.ProfileDAO profileDAO;
    private com.friendzone.dao.MatchingDAO matchingDAO;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userDAO = new UserDAO();
        this.gameDAO = new GameDAO();
        this.chatDAO = new com.friendzone.dao.ChatDAO();
        this.statsDAO = new com.friendzone.dao.StatsDAO();
        this.notificationDAO = new com.friendzone.dao.NotificationDAO();
        this.callDAO = new com.friendzone.dao.CallDAO();
        this.stickerDAO = new com.friendzone.dao.StickerDAO();
        this.profileDAO = new com.friendzone.dao.ProfileDAO();
        this.matchingDAO = new com.friendzone.dao.MatchingDAO();
    }

    // Gson instance
    private com.google.gson.Gson gson = new com.google.gson.Gson();

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received JSON: " + line);
                try {
                    com.friendzone.model.NetworkMessage msg = gson.fromJson(line, com.friendzone.model.NetworkMessage.class);
                    if (msg == null || msg.getCommand() == null) continue;

                    switch (msg.getCommand()) {
                        case "LOGIN":
                            handleLogin(msg.getData());
                            break;
                        case "REGISTER":
                            handleRegister(msg.getData());
                            break;
                        case "LOGOUT":
                            handleLogout();
                            break;
                        case "CHAT":
                            handleChat(msg.getData());
                            break;
                        case "INVITE":
                        case "SEND_INVITE":
                        case "GAME_INVITE":
                            handleInvite(msg.getData());
                            break;
                        case "ACCEPT_INVITE":
                            handleAcceptInvite(msg.getData());
                            break;
                        case "MOVE_CARO":
                            handleCaroMove(msg.getData());
                            break;
                        case "GAME_END":
                            handleGameEnd(msg.getData());
                            break;
                        case "GAME_SCORE":
                            handleGameScore(msg.getData());
                            break;
                        case "FRIEND_REQUEST":
                            handleFriendRequest(msg.getData());
                            break;
                        case "FRIEND_RESPONSE":
                            handleFriendResponse(msg.getData());
                            break;
                        case "FIND_MATCH":
                            handleFindMatch();
                            break;
                        case "START_DM":
                            handleStartDM(msg.getData());
                            break;
                        case "SEND_DM":
                            handleSendDM(msg.getData());
                            break;
                        case "FETCH_HISTORY":
                            handleFetchHistory(msg.getData());
                            break;
                        case "GET_LEADERBOARD":
                            handleGetLeaderboard(msg.getData());
                            break;
                        case "GET_NOTIFICATIONS":
                            handleGetNotifications();
                            break;
                        case "MARK_READ":
                            handleMarkRead(msg.getData());
                            break;
                        case "GET_CALL_HISTORY":
                            handleGetCallHistory();
                            break;
                        case "CREATE_GROUP":
                            handleCreateGroup(msg.getData());
                            break;
                        case "SEND_GROUP_MSG":
                            handleSendGroupMsg(msg.getData());
                            break;
                        case "GET_MY_GROUPS":
                            handleGetMyGroups();
                            break;
                        case "FETCH_GROUP_HISTORY":
                            handleFetchGroupHistory(msg.getData());
                            break;
                        case "GET_STICKER_PACKS":
                            handleGetStickerPacks();
                            break;
                        case "DELETE_ACCOUNT":
                            handleDeleteAccount();
                            break;
                        case "REMATCH_REQUEST":
                            handleRematchRequest(msg.getData());
                            break;
                        case "REMATCH_RESPONSE":
                            handleRematchResponse(msg.getData());
                            break;
                        case "VIDEO_CALL_REQUEST":
                            handleVideoCallRequest(msg.getData());
                            break;
                        case "VIDEO_CALL_RESPONSE":
                            handleVideoCallResponse(msg.getData());
                            break;
                        case "SEND_FILE":
                            handleSendFile(msg.getData());
                            break;
                        case "SEND_GROUP_FILE":
                            handleSendGroupFile(msg.getData());
                            break;
                        case "SEND_IMAGE":
                            handleSendImage(msg.getData());
                            break;
                        case "SEND_STICKER":
                            handleSendSticker(msg.getData());
                            break;
                        case "SEND_VOICE":
                            handleSendVoice(msg.getData());
                            break;
                        case "ADD_GROUP_MEMBER":
                            handleAddGroupMember(msg.getData());
                            break;
                        case "LEAVE_GROUP":
                            handleLeaveGroup(msg.getData());
                            break;
                        case "GET_GROUP_MEMBERS":
                            handleGetGroupMembers(msg.getData());
                            break;
                        case "GROUP_CALL_REQUEST":
                            handleGroupCallRequest(msg.getData());
                            break;
                        case "VIDEO_CALL_END":
                            handleVideoCallEnd(msg.getData());
                            break;
                        // ========== FILE TRANSFER PROTOCOL ==========
                        case "FILE_TRANSFER_START":
                            handleFileTransferStart(msg.getData());
                            break;
                        case "FILE_CHUNK":
                            handleFileChunk(msg.getData());
                            break;
                        case "FILE_TRANSFER_COMPLETE":
                            handleFileTransferComplete(msg.getData());
                            break;
                        // ========== HEARTBEAT ==========
                        case "PING":
                            handlePing();
                            break;
                        case "PONG":
                            handlePong();
                            break;
                        // ========== WORD CHAIN GAME ==========
                        case "WORD_CHAIN_MOVE":
                            handleWordChainMove(msg.getData());
                            break;
                        case "WORD_CHAIN_TIMEOUT":
                            handleWordChainTimeout(msg.getData());
                            break;
                        case "WORD_CHAIN_QUIT":
                            handleWordChainQuit(msg.getData());
                            break;
                        // ========== LOVE QUIZ GAME ==========
                        case "QUIZ_READY":
                            handleQuizReady(msg.getData());
                            break;
                        case "QUIZ_ANSWER":
                            handleQuizAnswer(msg.getData());
                            break;
                        case "QUIZ_NEXT":
                            handleQuizNext(msg.getData());
                            break;
                        case "QUIZ_QUIT":
                            handleQuizQuit(msg.getData());
                            break;
                        // ========== DRAW GUESS GAME ==========
                        case "DRAW_START_ROUND":
                            handleDrawStartRound(msg.getData());
                            break;
                        case "DRAW_HINT":
                            handleDrawHint(msg.getData());
                            break;
                        case "DRAW_GUESS":
                            handleDrawGuess(msg.getData());
                            break;
                        case "DRAW_GAME_END":
                            handleDrawGameEnd(msg.getData());
                            break;
                        case "DRAW_QUIT":
                            handleDrawQuit(msg.getData());
                            break;
                        // ========== DATING/MATCHING FEATURES ==========
                        case "GET_PROFILE":
                            handleGetProfile(msg.getData());
                            break;
                        case "UPDATE_PROFILE":
                            handleUpdateProfile(msg.getData());
                            break;
                        case "GET_DISCOVER_USERS":
                        case "GET_DISCOVER_PROFILES":
                            handleGetDiscoverUsers(msg.getData());
                            break;
                        case "SWIPE":
                        case "RECORD_SWIPE":
                            handleRecordSwipe(msg.getData());
                            break;
                        case "GET_MATCHES":
                            handleGetMatches(msg.getData());
                            break;
                        case "GET_COMPATIBILITY_LIST":
                            handleGetCompatibilityList(msg.getData());
                            break;
                        case "GET_USER_PROFILE":
                            handleGetUserProfile(msg.getData());
                            break;
                        default:
                            sendError("Unknown command");
                    }
                } catch (Exception e) {
                    System.err.println("Invalid JSON: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + (currentUser != null ? currentUser.getUsername() : "Unknown"));
        } finally {
            if (currentUser != null) {
                // Cập nhật trạng thái OFFLINE trong database
                userDAO.setOffline(currentUser.getId());
                // Xóa khỏi danh sách online
                Server.removeClient(currentUser.getId());
                // Log logout
                String ip = socket.getInetAddress().getHostAddress();
                userDAO.logConnection(currentUser.getId(), ip, "LOGOUT");
                // Thông báo cho tất cả client còn lại
                Server.broadcastOnlineUsers();
                System.out.println("User " + currentUser.getNickname() + " is now OFFLINE");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRegister(java.util.Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");
        String nickname = data.get("nickname");
        String gender = data.get("gender");
        
        if (username == null || password == null || nickname == null || gender == null) {
            sendError("Missing fields");
            return;
        }
        
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setNickname(nickname);
        
        if (userDAO.register(newUser, password, gender)) {
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Registration successful! Please login.");
            sendMessage("REGISTER_SUCCESS", response);
        } else {
            sendError("Registration failed (Username might be taken)");
        }
    }

    private void handleLogin(java.util.Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");
        
        User user = userDAO.login(username, password);
        if (user != null) {
            this.currentUser = user;
            Server.addClient(user.getId(), this); // Assuming Server.addClient handles onlineClients.put
            
            // Log connection
            String ip = socket.getInetAddress().getHostAddress();
            userDAO.logConnection(user.getId(), ip, "LOGIN");
            
            // Send success response
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("id", String.valueOf(user.getId()));
            response.put("nickname", user.getNickname());
            response.put("elo", String.valueOf(user.getElo()));
            response.put("money", String.valueOf(user.getMoney()));
            
            sendMessage("SUCCESS", response);
            
            // Broadcast new user list
            Server.broadcastOnlineUsers();
        } else {
            sendError("Invalid credentials");
        }
    }

    private void handleChat(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        String msg = data.get("message");
        
        java.util.Map<String, String> broadcastData = new java.util.HashMap<>();
        broadcastData.put("sender", currentUser.getNickname());
        broadcastData.put("content", msg);
        
        String json = gson.toJson(new com.friendzone.model.NetworkMessage("MSG", broadcastData));
        for (ClientHandler client : Server.onlineClients.values()) {
            client.sendJson(json);
        }
    }
    
    private void handleInvite(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        String gameCode = data.get("gameCode");
        try {
            long targetId = Long.parseLong(data.get("targetId"));
            ClientHandler target = Server.onlineClients.get(targetId);
            if (target != null) {
                java.util.Map<String, String> inviteData = new java.util.HashMap<>();
                inviteData.put("gameCode", gameCode);
                inviteData.put("inviterId", String.valueOf(currentUser.getId()));
                inviteData.put("inviterName", currentUser.getNickname());
                
                target.sendMessage("INVITE_RECEIVED", inviteData);
            } else {
                sendError("User not online");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid ID");
        }
    }
    
    private void handleAcceptInvite(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        String gameCode = data.get("gameCode");
        try {
            long inviterId = Long.parseLong(data.get("inviterId"));
            ClientHandler inviter = Server.onlineClients.get(inviterId);
            
            if (inviter != null) {
                long sessionId = gameDAO.createSession(currentUser.getId(), inviterId, gameCode);
                if (sessionId != -1) {
                    java.util.Map<String, String> startData = new java.util.HashMap<>();
                    startData.put("gameCode", gameCode);
                    startData.put("sessionId", String.valueOf(sessionId));
                    startData.put("p1", String.valueOf(inviterId));
                    startData.put("p2", String.valueOf(currentUser.getId()));
                    
                    // Thêm thông tin UDP cho game vẽ hình
                    if ("DRAW_GUESS".equals(gameCode)) {
                        int p1Port = 8000 + (int)(inviterId % 1000);
                        int p2Port = 8000 + (int)(currentUser.getId() % 1000);
                        startData.put("p1DrawPort", String.valueOf(p1Port));
                        startData.put("p2DrawPort", String.valueOf(p2Port));
                        startData.put("p1Name", inviter.currentUser.getNickname());
                        startData.put("p2Name", currentUser.getNickname());
                        // Gửi IP thật của 2 client
                        startData.put("p1Ip", inviter.getClientIp());
                        startData.put("p2Ip", this.getClientIp());
                    }
                    
                    inviter.sendMessage("START_GAME", startData);
                    this.sendMessage("START_GAME", startData);
                } else {
                    sendError("Could not create session");
                    inviter.sendError("Could not create session");
                }
            } else {
                sendError("Inviter no longer online");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid ID");
        }
    }
    
    private void handleCaroMove(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            int x = Integer.parseInt(data.get("x"));
            int y = Integer.parseInt(data.get("y"));
            long opponentId = Long.parseLong(data.get("opponentId"));
            
            ClientHandler opponent = Server.onlineClients.get(opponentId);
            if (opponent != null) {
                java.util.Map<String, String> moveData = new java.util.HashMap<>();
                moveData.put("x", String.valueOf(x));
                moveData.put("y", String.valueOf(y));
                opponent.sendMessage("OPPONENT_MOVE", moveData);
            }
            
            String moveJson = "{\"x\":" + x + ", \"y\":" + y + "}";
            gameDAO.saveMove(sessionId, currentUser.getId(), moveJson);
            
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private com.friendzone.dao.FriendDAO friendDAO = new com.friendzone.dao.FriendDAO();

    private void handleFriendRequest(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long targetId = Long.parseLong(data.get("targetId"));
            
            // Kiểm tra đã là bạn bè chưa
            if (friendDAO.checkFriendship(currentUser.getId(), targetId)) {
                sendError("Các bạn đã là bạn bè rồi!");
                return;
            }
            
            // Kiểm tra đã gửi lời mời chưa
            if (friendDAO.hasPendingRequest(currentUser.getId(), targetId)) {
                sendError("Bạn đã gửi lời mời kết bạn rồi!");
                return;
            }
            
            ClientHandler target = Server.onlineClients.get(targetId);
            
            if (target != null) {
                if (friendDAO.sendFriendRequest(currentUser.getId(), targetId)) {
                    java.util.Map<String, String> reqData = new java.util.HashMap<>();
                    reqData.put("senderId", String.valueOf(currentUser.getId()));
                    reqData.put("senderName", currentUser.getNickname());
                    target.sendMessage("FRIEND_REQUEST_RECEIVED", reqData);
                    
                    sendMessage("MSG", java.util.Map.of("sender", "Hệ thống", "content", "Đã gửi lời mời kết bạn!"));
                } else {
                    sendError("Không thể gửi lời mời kết bạn");
                }
            } else {
                sendError("Người dùng không trực tuyến");
            }
        } catch (NumberFormatException e) {
            sendError("ID không hợp lệ");
        }
    }
    
    private void handleFriendResponse(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long senderId = Long.parseLong(data.get("senderId"));
            boolean accepted = Boolean.parseBoolean(data.get("accepted"));
            
            if (accepted) {
                if (friendDAO.acceptFriendRequest(senderId, currentUser.getId())) {
                    ClientHandler sender = Server.onlineClients.get(senderId);
                    if (sender != null) {
                        sender.sendMessage("MSG", java.util.Map.of("sender", "System", "content", currentUser.getNickname() + " accepted your friend request!"));
                    }
                    sendMessage("MSG", java.util.Map.of("sender", "System", "content", "You are now friends!"));
                }
            }
            // If rejected, we just don't update DB (remain PENDING or delete? For now keep simple)
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    private void handleFindMatch() {
        if (currentUser == null) return;
        String myGender = currentUser.getGender();
        if (myGender == null || myGender.equals("OTHER")) {
            sendError("Cannot find matches for OTHER gender");
            return;
        }
        
        java.util.List<User> matches = userDAO.findRandomMatches(myGender, 10);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("matches", gson.toJson(matches));
        sendMessage("MATCH_RESULTS", response);
    }
    
    private void handleStartDM(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long targetId = Long.parseLong(data.get("targetId"));
            long roomId = chatDAO.getOrCreatePrivateRoom(currentUser.getId(), targetId);
            
            if (roomId != -1) {
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("roomId", String.valueOf(roomId));
                response.put("targetId", String.valueOf(targetId));
                sendMessage("DM_ROOM_READY", response);
            } else {
                sendError("Failed to create chat room");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid target ID");
        }
    }
    
    private void handleSendDM(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String content = data.get("content");
            
            if (chatDAO.sendMessage(roomId, currentUser.getId(), content, "TEXT")) {
                // Notify the other user if online
                long otherId = chatDAO.getOtherUserId(roomId, currentUser.getId());
                ClientHandler other = Server.onlineClients.get(otherId);
                
                if (other != null) {
                    java.util.Map<String, String> notification = new java.util.HashMap<>();
                    notification.put("roomId", String.valueOf(roomId));
                    notification.put("senderId", String.valueOf(currentUser.getId()));
                    notification.put("senderName", currentUser.getNickname());
                    notification.put("content", content);
                    other.sendMessage("NEW_DM", notification);
                }
            }
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleFetchHistory(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            java.util.List<java.util.Map<String, String>> messages = chatDAO.getMessages(roomId, 50);
            
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("roomId", String.valueOf(roomId));
            response.put("messages", gson.toJson(messages));
            sendMessage("DM_HISTORY", response);
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleGetLeaderboard(java.util.Map<String, String> data) {
        String gameCode = data.get("gameCode");
        int limit = Integer.parseInt(data.getOrDefault("limit", "10"));
        
        java.util.List<User> topPlayers = statsDAO.getTopPlayers(gameCode, limit);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("gameCode", gameCode);
        response.put("leaderboard", gson.toJson(topPlayers));
        sendMessage("LEADERBOARD_DATA", response);
    }
    
    private void handleGetNotifications() {
        if (currentUser == null) return;
        
        java.util.List<java.util.Map<String, String>> notifications = 
            notificationDAO.getUnreadNotifications(currentUser.getId());
        int unreadCount = notificationDAO.getUnreadCount(currentUser.getId());
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("notifications", gson.toJson(notifications));
        response.put("count", String.valueOf(unreadCount));
        sendMessage("NOTIFICATIONS_DATA", response);
    }
    
    private void handleMarkRead(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        
        String notifId = data.get("notificationId");
        if ("ALL".equals(notifId)) {
            notificationDAO.markAllAsRead(currentUser.getId());
        } else {
            try {
                long id = Long.parseLong(notifId);
                notificationDAO.markAsRead(id);
            } catch (NumberFormatException e) {
                sendError("Invalid notification ID");
            }
        }
    }
    
    private void handleGetCallHistory() {
        if (currentUser == null) return;
        
        java.util.List<java.util.Map<String, String>> calls = 
            callDAO.getCallHistory(currentUser.getId(), 20);
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("calls", gson.toJson(calls));
        sendMessage("CALL_HISTORY_DATA", response);
    }
    
    private void handleCreateGroup(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        String roomName = data.get("roomName");
        String memberIdsStr = data.get("memberIds"); // Danh sách ID thành viên (comma separated)
        
        if (memberIdsStr == null || memberIdsStr.isEmpty()) {
            sendError("Vui lòng chọn ít nhất 1 thành viên khác!");
            return;
        }
        
        String[] memberIdArray = memberIdsStr.split(",");
        if (memberIdArray.length < 1) {
            sendError("Nhóm phải có ít nhất 2 người (bao gồm bạn)!");
            return;
        }
        
        long roomId = chatDAO.createGroupRoom(roomName, currentUser.getId());
        if (roomId != -1) {
            // Thêm các thành viên được chọn vào nhóm
            for (String idStr : memberIdArray) {
                try {
                    long memberId = Long.parseLong(idStr.trim());
                    chatDAO.addMemberToRoom(roomId, memberId);
                    
                    // Thông báo cho thành viên được thêm vào
                    ClientHandler member = Server.onlineClients.get(memberId);
                    if (member != null) {
                        java.util.Map<String, String> notifData = new java.util.HashMap<>();
                        notifData.put("roomId", String.valueOf(roomId));
                        notifData.put("roomName", roomName);
                        notifData.put("addedBy", currentUser.getNickname());
                        member.sendMessage("ADDED_TO_GROUP", notifData);
                    }
                } catch (NumberFormatException e) {
                    // Bỏ qua ID không hợp lệ
                }
            }
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("roomId", String.valueOf(roomId));
            response.put("roomName", roomName);
            response.put("memberCount", String.valueOf(memberIdArray.length + 1)); // +1 cho creator
            sendMessage("GROUP_CREATED", response);
        } else {
            sendError("Không thể tạo nhóm");
        }
    }
    
    private void handleSendGroupMsg(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String content = data.get("content");
            if (chatDAO.sendMessage(roomId, currentUser.getId(), content, "TEXT")) {
                java.util.List<Long> members = chatDAO.getRoomMembers(roomId);
                for (Long memberId : members) {
                    // Không gửi lại cho người gửi (tránh duplicate message)
                    if (memberId.equals(currentUser.getId())) {
                        continue;
                    }
                    ClientHandler member = Server.onlineClients.get(memberId);
                    if (member != null) {
                        java.util.Map<String, String> notification = new java.util.HashMap<>();
                        notification.put("roomId", String.valueOf(roomId));
                        notification.put("senderId", String.valueOf(currentUser.getId()));
                        notification.put("senderName", currentUser.getNickname());
                        notification.put("content", content);
                        member.sendMessage("NEW_GROUP_MSG", notification);
                    }
                }
            }
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleGetMyGroups() {
        if (currentUser == null) return;
        java.util.List<java.util.Map<String, String>> groups = chatDAO.getUserGroups(currentUser.getId());
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("groups", gson.toJson(groups));
        sendMessage("MY_GROUPS_DATA", response);
    }
    
    private void handleFetchGroupHistory(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            java.util.List<java.util.Map<String, String>> messages = chatDAO.getMessages(roomId, 50);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("roomId", String.valueOf(roomId));
            response.put("messages", gson.toJson(messages));
            sendMessage("GROUP_HISTORY", response);
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }

    private void handleGetStickerPacks() {
        java.util.List<java.util.Map<String, String>> packs = stickerDAO.getAllPacks();
        if (!packs.isEmpty()) {
            int packId = Integer.parseInt(packs.get(0).get("pack_id"));
            java.util.List<java.util.Map<String, String>> stickers = stickerDAO.getStickersByPack(packId);
            
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("stickers", gson.toJson(stickers));
            sendMessage("STICKER_DATA", response);
        }
    }
    
    private void handleDeleteAccount() {
        if (currentUser == null) return;
        if (userDAO.deleteUser(currentUser.getId())) {
            sendMessage("ACCOUNT_DELETED", new java.util.HashMap<>());
        } else {
            sendError("Failed to delete account");
        }
    }
    
    private void handleLogout() {
        if (currentUser != null) {
            String ip = socket.getInetAddress().getHostAddress();
            userDAO.logConnection(currentUser.getId(), ip, "LOGOUT");
            userDAO.setOffline(currentUser.getId());
            Server.removeClient(currentUser.getId());
            Server.broadcastOnlineUsers();
            
            sendMessage("LOGOUT_SUCCESS", new java.util.HashMap<>());
            currentUser = null;
        }
    }
    
    private void handleGameEnd(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            long winnerId = Long.parseLong(data.get("winnerId"));
            String reason = data.getOrDefault("reason", "NORMAL");
            String gameType = data.getOrDefault("gameType", "CARO");
            
            // Gọi stored procedure để kết thúc game
            gameDAO.finishGame(sessionId, winnerId);
            
            // Thông báo cho đối thủ
            String opponentIdStr = data.get("opponentId");
            if (opponentIdStr != null) {
                long opponentId = Long.parseLong(opponentIdStr);
                ClientHandler opponent = Server.onlineClients.get(opponentId);
                if (opponent != null) {
                    java.util.Map<String, String> endData = new java.util.HashMap<>();
                    endData.put("sessionId", String.valueOf(sessionId));
                    endData.put("winnerId", String.valueOf(winnerId));
                    endData.put("reason", reason);
                    opponent.sendMessage("GAME_ENDED", endData);
                }
                
                // Cập nhật compatibility sau game
                updateCompatibilityAfterGame(currentUser.getId(), opponentId, gameType, winnerId);
            }
            
            // Phản hồi cho người gửi
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "OK");
            response.put("winnerId", String.valueOf(winnerId));
            sendMessage("GAME_END_CONFIRMED", response);
            
        } catch (NumberFormatException e) {
            sendError("Invalid game data");
        }
    }
    
    private void handleGameScore(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        String gameCode = data.get("gameCode");
        int score = Integer.parseInt(data.getOrDefault("score", "0"));
        
        // Lưu điểm cao cho game single-player như Catch Heart
        statsDAO.updateHighScore(currentUser.getId(), gameCode, score);
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("status", "OK");
        response.put("score", String.valueOf(score));
        sendMessage("SCORE_SAVED", response);
    }
    
    private void handleRematchRequest(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long opponentId = Long.parseLong(data.get("opponentId"));
            String gameType = data.get("gameType");
            
            ClientHandler opponent = Server.onlineClients.get(opponentId);
            if (opponent != null) {
                java.util.Map<String, String> requestData = new java.util.HashMap<>();
                requestData.put("fromUserId", String.valueOf(currentUser.getId()));
                requestData.put("fromUserName", currentUser.getNickname());
                requestData.put("gameType", gameType);
                opponent.sendMessage("REMATCH_REQUEST", requestData);
                
                // Phản hồi cho người gửi
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "SENT");
                sendMessage("REMATCH_PENDING", response);
            } else {
                sendError("Đối thủ không còn trực tuyến");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid opponent ID");
        }
    }
    
    private void handleRematchResponse(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long opponentId = Long.parseLong(data.get("opponentId"));
            String gameType = data.get("gameType");
            boolean accepted = Boolean.parseBoolean(data.get("accepted"));
            
            ClientHandler opponent = Server.onlineClients.get(opponentId);
            if (opponent != null) {
                if (accepted) {
                    // Tạo session mới cho game
                    long newSessionId = gameDAO.createSession(currentUser.getId(), opponentId, gameType);
                    
                    if (newSessionId != -1) {
                        // Gửi cho cả 2 người chơi
                        java.util.Map<String, String> startData = new java.util.HashMap<>();
                        startData.put("accepted", "true");
                        startData.put("newSessionId", String.valueOf(newSessionId));
                        startData.put("gameType", gameType);
                        startData.put("p1", String.valueOf(opponentId));
                        startData.put("p2", String.valueOf(currentUser.getId()));
                        
                        opponent.sendMessage("REMATCH_ACCEPTED", startData);
                        this.sendMessage("REMATCH_ACCEPTED", startData);
                    } else {
                        sendError("Không thể tạo phiên game mới");
                        opponent.sendError("Không thể tạo phiên game mới");
                    }
                } else {
                    // Gửi thông báo từ chối
                    java.util.Map<String, String> rejectData = new java.util.HashMap<>();
                    rejectData.put("accepted", "false");
                    rejectData.put("message", currentUser.getNickname() + " từ chối chơi lại");
                    opponent.sendMessage("REMATCH_REJECTED", rejectData);
                }
            } else {
                sendError("Đối thủ không còn trực tuyến");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleVideoCallRequest(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long targetId = Long.parseLong(data.get("targetId"));
            String callType = data.getOrDefault("callType", "VIDEO"); // VIDEO or VOICE
            
            ClientHandler target = Server.onlineClients.get(targetId);
            if (target != null) {
                // Tạo bản ghi cuộc gọi
                long callId = callDAO.createCall(currentUser.getId(), targetId, callType);
                
                java.util.Map<String, String> callData = new java.util.HashMap<>();
                callData.put("callId", String.valueOf(callId));
                callData.put("callerId", String.valueOf(currentUser.getId()));
                callData.put("callerName", currentUser.getNickname());
                callData.put("callType", callType);
                target.sendMessage("INCOMING_CALL", callData);
                
                // Phản hồi cho người gọi
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("callId", String.valueOf(callId));
                response.put("status", "RINGING");
                sendMessage("CALL_INITIATED", response);
            } else {
                sendError("Người dùng không trực tuyến");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid target ID");
        }
    }
    
    private void handleVideoCallResponse(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long callId = Long.parseLong(data.get("callId"));
            long callerId = Long.parseLong(data.get("callerId"));
            boolean accepted = Boolean.parseBoolean(data.get("accepted"));
            
            ClientHandler caller = Server.onlineClients.get(callerId);
            
            if (accepted) {
                // Lấy callType từ database
                String callType = callDAO.getCallType(callId);
                if (callType == null) callType = "VIDEO";
                
                // ONGOING = đang trong cuộc gọi (đã kết nối)
                callDAO.updateCallStatus(callId, "ONGOING");
                
                if (caller != null) {
                    // Lấy IP của 2 bên từ socket
                    String callerIp = caller.getClientIp();
                    String receiverIp = this.getClientIp();
                    
                    // Port cho video UDP - mỗi bên lắng nghe 1 port
                    int callerListenPort = 6000 + (int)(callerId % 1000);
                    int receiverListenPort = 6000 + (int)(currentUser.getId() % 1000);
                    
                    // Gửi cho người gọi (caller): IP + Port của người nhận
                    java.util.Map<String, String> callerData = new java.util.HashMap<>();
                    callerData.put("callId", String.valueOf(callId));
                    callerData.put("accepted", "true");
                    callerData.put("targetId", String.valueOf(currentUser.getId()));
                    callerData.put("targetName", currentUser.getNickname());
                    callerData.put("targetIp", receiverIp);
                    callerData.put("targetPort", String.valueOf(receiverListenPort));
                    callerData.put("listenPort", String.valueOf(callerListenPort));
                    callerData.put("callType", callType); // Thêm callType
                    caller.sendMessage("CALL_ACCEPTED", callerData);
                    
                    // Gửi cho người nhận (receiver/this): IP + Port của người gọi
                    java.util.Map<String, String> receiverData = new java.util.HashMap<>();
                    receiverData.put("callId", String.valueOf(callId));
                    receiverData.put("peerId", String.valueOf(callerId));
                    receiverData.put("peerName", caller.getCurrentUser().getNickname());
                    receiverData.put("targetIp", callerIp);
                    receiverData.put("targetPort", String.valueOf(callerListenPort));
                    receiverData.put("listenPort", String.valueOf(receiverListenPort));
                    receiverData.put("callType", callType); // Thêm callType
                    sendMessage("CALL_STARTED", receiverData);
                }
            } else {
                callDAO.updateCallStatus(callId, "REJECTED");
                
                if (caller != null) {
                    java.util.Map<String, String> rejectData = new java.util.HashMap<>();
                    rejectData.put("callId", String.valueOf(callId));
                    rejectData.put("accepted", "false");
                    rejectData.put("message", currentUser.getNickname() + " từ chối cuộc gọi");
                    caller.sendMessage("CALL_REJECTED", rejectData);
                }
            }
        } catch (NumberFormatException e) {
            sendError("Invalid call data");
        }
    }
    
    /**
     * Get client IP address
     */
    public String getClientIp() {
        return socket.getInetAddress().getHostAddress();
    }
    
    private void handleSendFile(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            String fileSize = data.get("fileSize");
            String fileData = data.get("fileData");  // Base64 encoded
            String fileType = data.getOrDefault("fileType", "FILE");
            
            // Lưu thông tin file vào database (content là thông tin file)
            String fileInfo = "[FILE] " + fileName + " (" + formatFileSize(Long.parseLong(fileSize)) + ")";
            if (chatDAO.sendMessage(roomId, currentUser.getId(), fileInfo, fileType)) {
                // Thông báo cho người nhận
                long otherId = chatDAO.getOtherUserId(roomId, currentUser.getId());
                ClientHandler other = Server.onlineClients.get(otherId);
                
                if (other != null) {
                    java.util.Map<String, String> notification = new java.util.HashMap<>();
                    notification.put("roomId", String.valueOf(roomId));
                    notification.put("senderId", String.valueOf(currentUser.getId()));
                    notification.put("senderName", currentUser.getNickname());
                    notification.put("content", fileInfo);
                    notification.put("fileName", fileName);
                    notification.put("fileSize", fileSize);
                    notification.put("fileData", fileData);  // Gửi dữ liệu Base64
                    notification.put("fileType", fileType);
                    other.sendMessage("NEW_FILE", notification);
                }
                
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "OK");
                response.put("fileName", fileName);
                sendMessage("FILE_SENT", response);
            } else {
                sendError("Không thể gửi file");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleSendGroupFile(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            String fileSize = data.get("fileSize");
            String fileType = data.getOrDefault("fileType", "FILE");
            
            String fileInfo = "[FILE] " + fileName + " (" + formatFileSize(Long.parseLong(fileSize)) + ")";
            if (chatDAO.sendMessage(roomId, currentUser.getId(), fileInfo, fileType)) {
                // Thông báo cho tất cả thành viên
                java.util.List<Long> members = chatDAO.getRoomMembers(roomId);
                for (Long memberId : members) {
                    if (memberId != currentUser.getId()) {
                        ClientHandler member = Server.onlineClients.get(memberId);
                        if (member != null) {
                            java.util.Map<String, String> notification = new java.util.HashMap<>();
                            notification.put("roomId", String.valueOf(roomId));
                            notification.put("senderId", String.valueOf(currentUser.getId()));
                            notification.put("senderName", currentUser.getNickname());
                            notification.put("content", fileInfo);
                            notification.put("fileName", fileName);
                            notification.put("fileType", fileType);
                            member.sendMessage("NEW_GROUP_FILE", notification);
                        }
                    }
                }
                
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "OK");
                response.put("fileName", fileName);
                sendMessage("FILE_SENT", response);
            } else {
                sendError("Không thể gửi file");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleAddGroupMember(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            long userId = Long.parseLong(data.get("userId"));
            
            if (chatDAO.addMemberToRoom(roomId, userId)) {
                // Thông báo cho người được thêm
                ClientHandler newMember = Server.onlineClients.get(userId);
                if (newMember != null) {
                    String roomName = chatDAO.getRoomName(roomId);
                    java.util.Map<String, String> notifData = new java.util.HashMap<>();
                    notifData.put("roomId", String.valueOf(roomId));
                    notifData.put("roomName", roomName);
                    notifData.put("addedBy", currentUser.getNickname());
                    newMember.sendMessage("ADDED_TO_GROUP", notifData);
                }
                
                // Thông báo cho các thành viên khác
                java.util.List<Long> members = chatDAO.getRoomMembers(roomId);
                User newUser = userDAO.getUserById(userId);
                String newUserName = newUser != null ? newUser.getNickname() : "Người dùng mới";
                
                for (Long memberId : members) {
                    ClientHandler member = Server.onlineClients.get(memberId);
                    if (member != null) {
                        java.util.Map<String, String> notification = new java.util.HashMap<>();
                        notification.put("roomId", String.valueOf(roomId));
                        notification.put("message", currentUser.getNickname() + " đã thêm " + newUserName + " vào nhóm");
                        member.sendMessage("GROUP_MEMBER_ADDED", notification);
                    }
                }
                
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "OK");
                response.put("userId", String.valueOf(userId));
                sendMessage("MEMBER_ADDED", response);
            } else {
                sendError("Không thể thêm thành viên");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleLeaveGroup(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            
            if (chatDAO.removeMemberFromRoom(roomId, currentUser.getId())) {
                // Thông báo cho các thành viên còn lại
                java.util.List<Long> members = chatDAO.getRoomMembers(roomId);
                for (Long memberId : members) {
                    ClientHandler member = Server.onlineClients.get(memberId);
                    if (member != null) {
                        java.util.Map<String, String> notification = new java.util.HashMap<>();
                        notification.put("roomId", String.valueOf(roomId));
                        notification.put("message", currentUser.getNickname() + " đã rời khỏi nhóm");
                        member.sendMessage("GROUP_MEMBER_LEFT", notification);
                    }
                }
                
                // Xác nhận với người rời
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "OK");
                response.put("roomId", String.valueOf(roomId));
                sendMessage("LEFT_GROUP", response);
            } else {
                sendError("Không thể rời nhóm");
            }
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleGetGroupMembers(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            java.util.List<java.util.Map<String, String>> members = chatDAO.getRoomMembersInfo(roomId);
            
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("roomId", String.valueOf(roomId));
            response.put("members", gson.toJson(members));
            sendMessage("GROUP_MEMBERS_DATA", response);
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleGroupCallRequest(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String callType = data.getOrDefault("callType", "GROUP_VIDEO");
            
            // Tạo cuộc gọi nhóm
            long callId = callDAO.createGroupCall(roomId, currentUser.getId(), callType);
            
            // Thông báo cho tất cả thành viên
            java.util.List<Long> members = chatDAO.getRoomMembers(roomId);
            String roomName = chatDAO.getRoomName(roomId);
            
            for (Long memberId : members) {
                if (memberId != currentUser.getId()) {
                    ClientHandler member = Server.onlineClients.get(memberId);
                    if (member != null) {
                        java.util.Map<String, String> callData = new java.util.HashMap<>();
                        callData.put("callId", String.valueOf(callId));
                        callData.put("roomId", String.valueOf(roomId));
                        callData.put("roomName", roomName);
                        callData.put("callerId", String.valueOf(currentUser.getId()));
                        callData.put("callerName", currentUser.getNickname());
                        callData.put("callType", callType);
                        member.sendMessage("INCOMING_GROUP_CALL", callData);
                    }
                }
            }
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("callId", String.valueOf(callId));
            response.put("status", "RINGING");
            sendMessage("GROUP_CALL_INITIATED", response);
        } catch (NumberFormatException e) {
            sendError("Invalid room ID");
        }
    }
    
    private void handleVideoCallEnd(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long callId = Long.parseLong(data.get("callId"));
            String reason = data.getOrDefault("reason", "ENDED");
            
            // Cập nhật trạng thái cuộc gọi trong database
            callDAO.updateCallStatus(callId, "COMPLETED");
            callDAO.endCall(callId);
            
            // Thông báo cho đối phương nếu có
            String peerIdStr = data.get("peerId");
            if (peerIdStr != null) {
                long peerId = Long.parseLong(peerIdStr);
                ClientHandler peer = Server.onlineClients.get(peerId);
                if (peer != null) {
                    java.util.Map<String, String> endData = new java.util.HashMap<>();
                    endData.put("callId", String.valueOf(callId));
                    endData.put("reason", reason);
                    endData.put("endedBy", currentUser.getNickname());
                    peer.sendMessage("CALL_ENDED", endData);
                }
            }
            
            // Phản hồi cho người kết thúc
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("callId", String.valueOf(callId));
            response.put("status", "ENDED");
            sendMessage("CALL_END_CONFIRMED", response);
        } catch (NumberFormatException e) {
            sendError("Invalid call ID");
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private void handleSendImage(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            String imageData = data.get("imageData"); // Base64 encoded image
            
            // Lưu thông tin vào database (không lưu ảnh thực, chỉ tên file)
            String fileInfo = "[IMAGE] " + fileName;
            chatDAO.sendMessage(roomId, currentUser.getId(), fileInfo, "IMAGE");
            
            // Gửi ảnh cho người nhận
            long otherId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler other = Server.onlineClients.get(otherId);
            
            if (other != null) {
                java.util.Map<String, String> notification = new java.util.HashMap<>();
                notification.put("roomId", String.valueOf(roomId));
                notification.put("senderId", String.valueOf(currentUser.getId()));
                notification.put("senderName", currentUser.getNickname());
                notification.put("fileName", fileName);
                notification.put("imageData", imageData); // Gửi Base64 image data
                other.sendMessage("NEW_IMAGE", notification);
            }
            
            // Phản hồi cho người gửi
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "OK");
            response.put("fileName", fileName);
            sendMessage("IMAGE_SENT", response);
            
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleSendSticker(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            String stickerData = data.get("stickerData"); // Base64 encoded sticker
            
            // Lưu thông tin vào database
            String stickerInfo = "[STICKER] " + fileName;
            chatDAO.sendMessage(roomId, currentUser.getId(), stickerInfo, "STICKER");
            
            // Gửi sticker cho người nhận
            long otherId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler other = Server.onlineClients.get(otherId);
            
            if (other != null) {
                java.util.Map<String, String> notification = new java.util.HashMap<>();
                notification.put("roomId", String.valueOf(roomId));
                notification.put("senderId", String.valueOf(currentUser.getId()));
                notification.put("senderName", currentUser.getNickname());
                notification.put("fileName", fileName);
                notification.put("stickerData", stickerData); // Gửi Base64 sticker data
                other.sendMessage("NEW_STICKER", notification);
            }
            
            // Phản hồi cho người gửi
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "OK");
            response.put("fileName", fileName);
            sendMessage("STICKER_SENT", response);
            
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }
    
    private void handleSendVoice(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            String duration = data.get("duration");
            String voiceData = data.get("voiceData"); // Base64 encoded WAV audio
            
            // Lưu thông tin vào database với metadata duration
            String voiceInfo = "[VOICE] " + fileName + " (" + duration + "s)";
            
            // Lưu duration vào file_meta JSON
            String fileMeta = "{\"duration\":" + duration + ",\"fileName\":\"" + fileName + "\"}";
            chatDAO.sendMessage(roomId, currentUser.getId(), voiceInfo, "VOICE");
            
            // Gửi voice message cho người nhận
            long otherId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler other = Server.onlineClients.get(otherId);
            
            if (other != null) {
                java.util.Map<String, String> notification = new java.util.HashMap<>();
                notification.put("roomId", String.valueOf(roomId));
                notification.put("senderId", String.valueOf(currentUser.getId()));
                notification.put("senderName", currentUser.getNickname());
                notification.put("fileName", fileName);
                notification.put("duration", duration);
                notification.put("voiceData", voiceData); // Gửi Base64 audio data
                other.sendMessage("NEW_VOICE", notification);
            }
            
            // Phản hồi cho người gửi
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "OK");
            response.put("fileName", fileName);
            sendMessage("VOICE_SENT", response);
            
        } catch (NumberFormatException e) {
            sendError("Invalid data");
        }
    }

    public void sendMessage(String command, java.util.Map<String, String> data) {
        out.println(gson.toJson(new com.friendzone.model.NetworkMessage(command, data)));
    }
    
    public void sendJson(String json) {
        out.println(json);
    }
    
    public void sendError(String msg) {
        java.util.Map<String, String> err = new java.util.HashMap<>();
        err.put("message", msg);
        sendMessage("FAIL", err);
    }
    public User getCurrentUser() {
        return currentUser;
    }
    
    // =====================================================
    // WORD CHAIN GAME HANDLERS
    // =====================================================
    
    // Lưu trạng thái game Word Chain cho mỗi session
    private static java.util.Map<Long, WordChainGameState> wordChainGames = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static class WordChainGameState {
        long p1, p2;
        String lastWord = "";
        java.util.Set<String> usedWords = new java.util.HashSet<>();
        long currentTurn; // ID của người đang đi
        int p1Score = 0, p2Score = 0;
        
        WordChainGameState(long p1, long p2, boolean p1First) {
            this.p1 = p1;
            this.p2 = p2;
            this.currentTurn = p1First ? p1 : p2;
        }
    }
    
    private void handleWordChainMove(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            String word = data.get("word").trim().toLowerCase();
            
            WordChainGameState state = wordChainGames.get(sessionId);
            if (state == null) {
                // Tạo state mới nếu chưa có
                long[] players = gameDAO.getSessionPlayers(sessionId);
                if (players == null) {
                    sendError("Session không tồn tại");
                    return;
                }
                state = new WordChainGameState(players[0], players[1], players[0] == currentUser.getId());
                wordChainGames.put(sessionId, state);
            }
            
            // Kiểm tra đúng lượt không
            if (state.currentTurn != currentUser.getId()) {
                sendError("Không phải lượt của bạn!");
                return;
            }
            
            // Validate từ
            boolean valid = true;
            String invalidReason = "";
            
            if (state.usedWords.contains(word)) {
                valid = false;
                invalidReason = "Từ đã được sử dụng";
            } else if (!state.lastWord.isEmpty()) {
                // Lấy từ cuối cùng của cụm từ trước (sau khoảng trắng cuối)
                String[] lastWords = state.lastWord.trim().split("\\s+");
                String lastWordInPhrase = lastWords[lastWords.length - 1];
                
                // Lấy từ đầu tiên của cụm từ hiện tại
                String[] currentWords = word.trim().split("\\s+");
                String firstWordInPhrase = currentWords[0];
                
                if (!firstWordInPhrase.equals(lastWordInPhrase)) {
                    valid = false;
                    invalidReason = "Từ phải bắt đầu bằng '" + lastWordInPhrase + "'";
                }
            }
            
            if (valid) {
                state.usedWords.add(word);
                state.lastWord = word;
                
                // Cập nhật điểm
                if (currentUser.getId() == state.p1) {
                    state.p1Score++;
                } else {
                    state.p2Score++;
                }
                
                // Đổi lượt
                state.currentTurn = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                
                // Gửi cho cả 2 người chơi
                java.util.Map<String, String> moveData = new java.util.HashMap<>();
                moveData.put("word", word);
                moveData.put("senderId", String.valueOf(currentUser.getId()));
                moveData.put("valid", "true");
                
                ClientHandler p1Handler = Server.onlineClients.get(state.p1);
                ClientHandler p2Handler = Server.onlineClients.get(state.p2);
                if (p1Handler != null) p1Handler.sendMessage("WORD_CHAIN_WORD", moveData);
                if (p2Handler != null) p2Handler.sendMessage("WORD_CHAIN_WORD", moveData);
            } else {
                // Từ không hợp lệ - người chơi thua
                long winnerId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                endWordChainGame(sessionId, winnerId, invalidReason);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Lỗi xử lý từ");
        }
    }
    
    private void handleWordChainTimeout(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            WordChainGameState state = wordChainGames.get(sessionId);
            
            if (state != null && state.currentTurn == currentUser.getId()) {
                // Hết giờ = thua
                long winnerId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                endWordChainGame(sessionId, winnerId, "Hết thời gian suy nghĩ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleWordChainQuit(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            WordChainGameState state = wordChainGames.get(sessionId);
            
            if (state != null) {
                long winnerId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                endWordChainGame(sessionId, winnerId, currentUser.getNickname() + " đã thoát");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void endWordChainGame(long sessionId, long winnerId, String reason) {
        WordChainGameState state = wordChainGames.get(sessionId);
        if (state == null) return;
        
        // Kết thúc game trong DB
        gameDAO.finishGame(sessionId, winnerId);
        
        // Cập nhật compatibility sau game
        updateCompatibilityAfterGame(state.p1, state.p2, "WORD_CHAIN", winnerId);
        
        // Gửi kết quả cho cả 2
        java.util.Map<String, String> endData = new java.util.HashMap<>();
        endData.put("winnerId", String.valueOf(winnerId));
        endData.put("reason", reason);
        endData.put("p1Score", String.valueOf(state.p1Score));
        endData.put("p2Score", String.valueOf(state.p2Score));
        
        ClientHandler p1Handler = Server.onlineClients.get(state.p1);
        ClientHandler p2Handler = Server.onlineClients.get(state.p2);
        if (p1Handler != null) p1Handler.sendMessage("WORD_CHAIN_END", endData);
        if (p2Handler != null) p2Handler.sendMessage("WORD_CHAIN_END", endData);
        
        // Xóa state
        wordChainGames.remove(sessionId);
    }
    
    // =====================================================
    // LOVE QUIZ GAME HANDLERS
    // =====================================================
    
    private static java.util.Map<Long, LoveQuizGameState> quizGames = new java.util.concurrent.ConcurrentHashMap<>();
    private com.friendzone.dao.QuizDAO quizDAO = new com.friendzone.dao.QuizDAO();
    
    private static class LoveQuizGameState {
        long p1, p2;
        int currentQuestion = 0;
        int totalQuestions = 10;
        int score = 0; // Số câu trùng đáp án
        int p1Answer = -1, p2Answer = -1;
        boolean p1Answered = false, p2Answered = false;
        
        // Câu hỏi được load từ database
        java.util.List<com.friendzone.dao.QuizDAO.QuizQuestion> questions;
        
        LoveQuizGameState(long p1, long p2, java.util.List<com.friendzone.dao.QuizDAO.QuizQuestion> questions) {
            this.p1 = p1;
            this.p2 = p2;
            this.questions = questions;
            this.totalQuestions = Math.min(10, questions.size());
        }
    }
    
    private void handleQuizReady(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            
            LoveQuizGameState state = quizGames.get(sessionId);
            if (state == null) {
                long[] players = gameDAO.getSessionPlayers(sessionId);
                if (players == null) {
                    sendError("Session không tồn tại");
                    return;
                }
                // Load 10 câu hỏi ngẫu nhiên từ database
                java.util.List<com.friendzone.dao.QuizDAO.QuizQuestion> questions = quizDAO.getRandomQuestions(10, "LOVE");
                if (questions.isEmpty()) {
                    sendError("Không có câu hỏi trong database!");
                    return;
                }
                state = new LoveQuizGameState(players[0], players[1], questions);
                quizGames.put(sessionId, state);
            }
            
            // Gửi câu hỏi đầu tiên
            sendQuizQuestion(sessionId, state);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendQuizQuestion(long sessionId, LoveQuizGameState state) {
        if (state.currentQuestion >= state.totalQuestions) {
            endQuizGame(sessionId);
            return;
        }
        
        state.currentQuestion++;
        state.p1Answer = -1;
        state.p2Answer = -1;
        state.p1Answered = false;
        state.p2Answered = false;
        
        // Lấy câu hỏi từ list đã load
        com.friendzone.dao.QuizDAO.QuizQuestion q = state.questions.get(state.currentQuestion - 1);
        
        java.util.Map<String, String> qData = new java.util.HashMap<>();
        qData.put("questionNum", String.valueOf(state.currentQuestion));
        qData.put("question", q.getQuestion());
        qData.put("answer0", q.getAnswerA());
        qData.put("answer1", q.getAnswerB());
        qData.put("answer2", q.getAnswerC());
        qData.put("answer3", q.getAnswerD());
        
        ClientHandler p1Handler = Server.onlineClients.get(state.p1);
        ClientHandler p2Handler = Server.onlineClients.get(state.p2);
        if (p1Handler != null) p1Handler.sendMessage("QUIZ_QUESTION", qData);
        if (p2Handler != null) p2Handler.sendMessage("QUIZ_QUESTION", qData);
    }
    
    private void handleQuizAnswer(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            int answer = Integer.parseInt(data.get("answer"));
            
            LoveQuizGameState state = quizGames.get(sessionId);
            if (state == null) return;
            
            // Lưu câu trả lời
            if (currentUser.getId() == state.p1) {
                state.p1Answer = answer;
                state.p1Answered = true;
            } else {
                state.p2Answer = answer;
                state.p2Answered = true;
            }
            
            // Nếu cả 2 đã trả lời
            if (state.p1Answered && state.p2Answered) {
                boolean matched = (state.p1Answer == state.p2Answer);
                if (matched) state.score++;
                
                // Gửi kết quả
                ClientHandler p1Handler = Server.onlineClients.get(state.p1);
                ClientHandler p2Handler = Server.onlineClients.get(state.p2);
                
                // Cho P1
                java.util.Map<String, String> resultP1 = new java.util.HashMap<>();
                resultP1.put("myAnswer", String.valueOf(state.p1Answer));
                resultP1.put("opponentAnswer", String.valueOf(state.p2Answer));
                resultP1.put("matched", String.valueOf(matched));
                if (p1Handler != null) p1Handler.sendMessage("QUIZ_RESULT", resultP1);
                
                // Cho P2
                java.util.Map<String, String> resultP2 = new java.util.HashMap<>();
                resultP2.put("myAnswer", String.valueOf(state.p2Answer));
                resultP2.put("opponentAnswer", String.valueOf(state.p1Answer));
                resultP2.put("matched", String.valueOf(matched));
                if (p2Handler != null) p2Handler.sendMessage("QUIZ_RESULT", resultP2);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleQuizNext(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            LoveQuizGameState state = quizGames.get(sessionId);
            
            if (state != null && state.currentQuestion < state.totalQuestions) {
                // Chỉ gửi câu tiếp theo nếu cả 2 đã hoàn thành câu trước
                if (state.p1Answered && state.p2Answered) {
                    sendQuizQuestion(sessionId, state);
                }
            } else if (state != null) {
                endQuizGame(sessionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleQuizQuit(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            LoveQuizGameState state = quizGames.get(sessionId);
            
            if (state != null) {
                // Thông báo cho đối phương
                long otherId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                ClientHandler other = Server.onlineClients.get(otherId);
                if (other != null) {
                    java.util.Map<String, String> quitData = new java.util.HashMap<>();
                    quitData.put("message", currentUser.getNickname() + " đã thoát quiz");
                    quitData.put("score", String.valueOf(state.score));
                    quitData.put("maxScore", String.valueOf(state.currentQuestion));
                    other.sendMessage("QUIZ_END", quitData);
                }
                
                quizGames.remove(sessionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void endQuizGame(long sessionId) {
        LoveQuizGameState state = quizGames.get(sessionId);
        if (state == null) return;
        
        java.util.Map<String, String> endData = new java.util.HashMap<>();
        endData.put("score", String.valueOf(state.score));
        endData.put("maxScore", String.valueOf(state.totalQuestions));
        
        ClientHandler p1Handler = Server.onlineClients.get(state.p1);
        ClientHandler p2Handler = Server.onlineClients.get(state.p2);
        if (p1Handler != null) p1Handler.sendMessage("QUIZ_END", endData);
        if (p2Handler != null) p2Handler.sendMessage("QUIZ_END", endData);
        
        // Cập nhật DB - ai cũng thắng vì là game hợp tác
        gameDAO.finishGame(sessionId, state.p1);
        
        // Cập nhật compatibility - dựa trên số câu trùng
        // Nếu trùng >= 7/10 thì cả hai đều "thắng"
        long winnerId = state.score >= 7 ? state.p1 : 0;
        updateCompatibilityAfterGame(state.p1, state.p2, "LOVE_QUIZ", winnerId);
        
        quizGames.remove(sessionId);
    }
    
    // =====================================================
    // DRAW GUESS GAME HANDLERS
    // =====================================================
    
    private static java.util.Map<Long, DrawGuessGameState> drawGames = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static class DrawGuessGameState {
        long p1, p2;
        long drawerId; // Người đang vẽ
        String currentWord = "";
        int p1Score = 0, p2Score = 0;
        int currentRound = 1;
        int totalRounds = 6;
        
        DrawGuessGameState(long p1, long p2, long firstDrawer) {
            this.p1 = p1;
            this.p2 = p2;
            this.drawerId = firstDrawer;
        }
    }
    
    private void handleDrawStartRound(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            String word = data.get("word").trim().toLowerCase();
            int hintLength = Integer.parseInt(data.get("hint"));
            
            DrawGuessGameState state = drawGames.get(sessionId);
            if (state == null) {
                long[] players = gameDAO.getSessionPlayers(sessionId);
                if (players == null) return;
                state = new DrawGuessGameState(players[0], players[1], currentUser.getId());
                drawGames.put(sessionId, state);
            }
            
            // Lưu từ hiện tại và người vẽ
            state.currentWord = word;
            state.drawerId = currentUser.getId();
            
            // Gửi hint cho người đoán
            long guesserId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
            ClientHandler guesser = Server.onlineClients.get(guesserId);
            if (guesser != null) {
                java.util.Map<String, String> hintData = new java.util.HashMap<>();
                hintData.put("hint", String.valueOf(hintLength));
                guesser.sendMessage("DRAW_HINT", hintData);
            }
            
            System.out.println("Draw round started, word: " + word);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleDrawHint(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            int hintLength = Integer.parseInt(data.get("hint"));
            
            DrawGuessGameState state = drawGames.get(sessionId);
            if (state == null) {
                long[] players = gameDAO.getSessionPlayers(sessionId);
                if (players == null) return;
                state = new DrawGuessGameState(players[0], players[1], currentUser.getId());
                drawGames.put(sessionId, state);
            }
            
            // Gửi hint cho người đoán
            long guesserId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
            ClientHandler guesser = Server.onlineClients.get(guesserId);
            if (guesser != null) {
                java.util.Map<String, String> hintData = new java.util.HashMap<>();
                hintData.put("hint", String.valueOf(hintLength));
                guesser.sendMessage("DRAW_HINT", hintData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleDrawGuess(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            String guess = data.get("guess").trim().toLowerCase();
            
            DrawGuessGameState state = drawGames.get(sessionId);
            if (state == null) return;
            
            // Gửi cho người vẽ biết ai đó đoán
            ClientHandler drawer = Server.onlineClients.get(state.drawerId);
            if (drawer != null && state.drawerId != currentUser.getId()) {
                java.util.Map<String, String> guessData = new java.util.HashMap<>();
                guessData.put("guess", guess);
                drawer.sendMessage("DRAW_OPPONENT_GUESS", guessData);
            }
            
            // Kiểm tra đáp án với từ được lưu trong state
            boolean isCorrect = guess.equals(state.currentWord);
            
            java.util.Map<String, String> resultData = new java.util.HashMap<>();
            resultData.put("guess", guess);
            resultData.put("guesserId", String.valueOf(currentUser.getId()));
            resultData.put("correct", String.valueOf(isCorrect));
            
            // Nếu đúng, gửi cả từ đúng
            if (isCorrect) {
                resultData.put("correctWord", state.currentWord);
                System.out.println("Correct guess: " + guess + " = " + state.currentWord);
            }
            
            ClientHandler p1Handler = Server.onlineClients.get(state.p1);
            ClientHandler p2Handler = Server.onlineClients.get(state.p2);
            if (p1Handler != null) p1Handler.sendMessage("DRAW_GUESS_RESULT", resultData);
            if (p2Handler != null) p2Handler.sendMessage("DRAW_GUESS_RESULT", resultData);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleDrawGameEnd(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            int myScore = Integer.parseInt(data.get("myScore"));
            
            DrawGuessGameState state = drawGames.get(sessionId);
            if (state != null) {
                // Cập nhật điểm
                if (currentUser.getId() == state.p1) {
                    state.p1Score = myScore;
                } else {
                    state.p2Score = myScore;
                }
                
                // Xác định người thắng
                long winnerId = state.p1Score > state.p2Score ? state.p1 : state.p2;
                gameDAO.finishGame(sessionId, winnerId);
                
                // Cập nhật compatibility sau game
                updateCompatibilityAfterGame(state.p1, state.p2, "DRAW_GUESS", winnerId);
                
                drawGames.remove(sessionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleDrawQuit(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long sessionId = Long.parseLong(data.get("sessionId"));
            
            DrawGuessGameState state = drawGames.get(sessionId);
            if (state != null) {
                // Thông báo cho đối phương
                long otherId = (currentUser.getId() == state.p1) ? state.p2 : state.p1;
                ClientHandler other = Server.onlineClients.get(otherId);
                if (other != null) {
                    java.util.Map<String, String> quitData = new java.util.HashMap<>();
                    quitData.put("message", currentUser.getNickname() + " đã thoát game");
                    other.sendMessage("DRAW_GAME_ENDED", quitData);
                }
                
                // Người còn lại thắng
                gameDAO.finishGame(sessionId, otherId);
                drawGames.remove(sessionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // =====================================================
    // DATING/MATCHING HANDLERS
    // =====================================================
    
    private void handleGetProfile(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            java.util.Map<String, Object> profile = profileDAO.getProfile(currentUser.getId());
            java.util.List<java.util.Map<String, Object>> interests = profileDAO.getUserInterests(currentUser.getId());
            java.util.List<java.util.Map<String, Object>> photos = profileDAO.getUserPhotos(currentUser.getId());
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("bio", String.valueOf(profile.getOrDefault("bio", "")));
            response.put("location", String.valueOf(profile.getOrDefault("location", "")));
            response.put("occupation", String.valueOf(profile.getOrDefault("occupation", "")));
            response.put("education", String.valueOf(profile.getOrDefault("education", "")));
            response.put("lookingFor", String.valueOf(profile.getOrDefault("lookingFor", "DATING")));
            response.put("preferredGender", String.valueOf(profile.getOrDefault("preferredGender", "BOTH")));
            response.put("ageMin", String.valueOf(profile.getOrDefault("ageMin", 18)));
            response.put("ageMax", String.valueOf(profile.getOrDefault("ageMax", 99)));
            response.put("interests", gson.toJson(interests));
            response.put("photos", gson.toJson(photos));
            
            sendMessage("PROFILE_DATA", response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể tải hồ sơ");
        }
    }
    
    private void handleUpdateProfile(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            // Tạo map cho updateProfile
            java.util.Map<String, String> profileData = new java.util.HashMap<>();
            if (data.containsKey("bio")) profileData.put("bio", data.get("bio"));
            if (data.containsKey("location")) profileData.put("location", data.get("location"));
            if (data.containsKey("occupation")) profileData.put("occupation", data.get("occupation"));
            if (data.containsKey("education")) profileData.put("education", data.get("education"));
            if (data.containsKey("lookingFor")) profileData.put("lookingFor", data.get("lookingFor"));
            if (data.containsKey("preferredGender")) profileData.put("preferredGender", data.get("preferredGender"));
            if (data.containsKey("ageMin")) profileData.put("ageMin", data.get("ageMin"));
            if (data.containsKey("ageMax")) profileData.put("ageMax", data.get("ageMax"));
            
            boolean success = profileDAO.updateProfile(currentUser.getId(), profileData);
            
            // Xử lý interests nếu có
            String interestsStr = data.get("interests");
            if (interestsStr != null && !interestsStr.isEmpty()) {
                java.util.List<Integer> interestIds = new java.util.ArrayList<>();
                for (String idStr : interestsStr.split(",")) {
                    try {
                        interestIds.add(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException e) {}
                }
                profileDAO.updateInterests(currentUser.getId(), interestIds);
            }
            
            if (success) {
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("status", "OK");
                sendMessage("PROFILE_UPDATED", response);
            } else {
                sendError("Không thể cập nhật hồ sơ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Lỗi cập nhật hồ sơ");
        }
    }
    
    private void handleGetDiscoverUsers(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            int limit = Integer.parseInt(data.getOrDefault("limit", "20"));
            
            java.util.List<java.util.Map<String, Object>> users = matchingDAO.getDiscoverUsers(currentUser.getId(), limit);
            
            // Thêm thông tin compatibility cho mỗi user
            for (java.util.Map<String, Object> user : users) {
                long otherId = ((Number) user.get("userId")).longValue();
                int compatibility = matchingDAO.calculateCompatibility(currentUser.getId(), otherId);
                user.put("compatibility", compatibility);
            }
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("users", gson.toJson(users));
            sendMessage("DISCOVER_USERS", response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể tải danh sách khám phá");
        }
    }
    
    private void handleRecordSwipe(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long targetId = Long.parseLong(data.get("targetId"));
            String action = data.get("action"); // LIKE, PASS, SUPER_LIKE
            
            matchingDAO.recordSwipe(currentUser.getId(), targetId, action);
            
            // Kiểm tra match
            java.util.Map<String, Object> match = matchingDAO.checkMutualLike(currentUser.getId(), targetId);
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "OK");
            response.put("targetId", String.valueOf(targetId));
            
            if (match != null) {
                // Có match!
                response.put("matched", "true");
                response.put("matchId", String.valueOf(match.get("matchId")));
                
                // Thông báo cho cả 2 người
                String targetUsername = userDAO.getUserById(targetId).getNickname();
                
                // Cho người swipe
                response.put("partnerUsername", targetUsername);
                response.put("isSuperLike", String.valueOf(match.getOrDefault("isSuperLike", false)));
                sendMessage("NEW_MATCH", response);
                
                // Cho người được swipe (nếu online)
                ClientHandler targetHandler = Server.onlineClients.get(targetId);
                if (targetHandler != null) {
                    java.util.Map<String, String> matchNotif = new java.util.HashMap<>();
                    matchNotif.put("matchId", String.valueOf(match.get("matchId")));
                    matchNotif.put("partnerUsername", currentUser.getNickname());
                    matchNotif.put("partnerId", String.valueOf(currentUser.getId()));
                    matchNotif.put("isSuperLike", String.valueOf(match.getOrDefault("isSuperLike", false)));
                    targetHandler.sendMessage("NEW_MATCH", matchNotif);
                }
            } else {
                response.put("matched", "false");
                sendMessage("SWIPE_RECORDED", response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể ghi nhận swipe");
        }
    }
    
    private void handleGetMatches(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            java.util.List<java.util.Map<String, Object>> matches = matchingDAO.getUserMatches(currentUser.getId());
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("matches", gson.toJson(matches));
            sendMessage("MATCHES_DATA", response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể tải danh sách matches");
        }
    }
    
    private void handleGetCompatibilityList(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            java.util.List<java.util.Map<String, Object>> compatList = matchingDAO.getCompatibilityList(currentUser.getId());
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("compatibilityList", gson.toJson(compatList));
            sendMessage("COMPATIBILITY_LIST_DATA", response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể tải danh sách tương thích");
        }
    }
    
    private void handleGetUserProfile(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            long userId = Long.parseLong(data.get("userId"));
            
            User user = userDAO.getUserById(userId);
            java.util.Map<String, Object> profile = profileDAO.getProfile(userId);
            java.util.List<java.util.Map<String, Object>> interests = profileDAO.getUserInterests(userId);
            int compatibility = matchingDAO.calculateCompatibility(currentUser.getId(), userId);
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("userId", String.valueOf(userId));
            response.put("username", user != null ? user.getNickname() : "Unknown");
            response.put("bio", String.valueOf(profile.getOrDefault("bio", "")));
            response.put("location", String.valueOf(profile.getOrDefault("location", "")));
            response.put("interests", gson.toJson(interests));
            response.put("compatibility", String.valueOf(compatibility));
            
            sendMessage("USER_PROFILE_VIEW", response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Không thể tải hồ sơ người dùng");
        }
    }
    
    /**
     * Cập nhật compatibility sau khi chơi game
     * Được gọi sau khi kết thúc mỗi game
     */
    public void updateCompatibilityAfterGame(long p1Id, long p2Id, String gameType, long winnerId) {
        try {
            matchingDAO.updateCompatibilityAfterGame(p1Id, p2Id, gameType, winnerId);
            
            // Thông báo cho cả 2 người
            int newScore1 = matchingDAO.calculateCompatibility(p1Id, p2Id);
            int newScore2 = matchingDAO.calculateCompatibility(p2Id, p1Id);
            
            ClientHandler p1Handler = Server.onlineClients.get(p1Id);
            ClientHandler p2Handler = Server.onlineClients.get(p2Id);
            
            String p1Name = userDAO.getUserById(p1Id).getNickname();
            String p2Name = userDAO.getUserById(p2Id).getNickname();
            
            if (p1Handler != null) {
                java.util.Map<String, String> updateData = new java.util.HashMap<>();
                updateData.put("partnerId", String.valueOf(p2Id));
                updateData.put("partnerUsername", p2Name);
                updateData.put("newScore", String.valueOf(newScore1));
                updateData.put("gameType", gameType);
                p1Handler.sendMessage("COMPATIBILITY_UPDATED", updateData);
            }
            
            if (p2Handler != null) {
                java.util.Map<String, String> updateData = new java.util.HashMap<>();
                updateData.put("partnerId", String.valueOf(p1Id));
                updateData.put("partnerUsername", p1Name);
                updateData.put("newScore", String.valueOf(newScore2));
                updateData.put("gameType", gameType);
                p2Handler.sendMessage("COMPATIBILITY_UPDATED", updateData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ========== HEARTBEAT HANDLERS ==========
    
    /**
     * Server sends PING to check if client is alive
     */
    private void handlePing() {
        // Client received PING, respond with PONG
        sendMessage("PONG", new java.util.HashMap<>());
    }
    
    /**
     * Client responds with PONG
     */
    private void handlePong() {
        // Update last ping time
        lastPingTime = System.currentTimeMillis();
        System.out.println("PONG received from user: " + (currentUser != null ? currentUser.getNickname() : "unknown"));
    }
    
    /**
     * Check if this client is still alive
     */
    public boolean isAlive() {
        long timeSinceLastPing = System.currentTimeMillis() - lastPingTime;
        return timeSinceLastPing < PING_TIMEOUT;
    }
    
    /**
     * Send PING to client
     */
    public void sendPing() {
        sendMessage("PING", new java.util.HashMap<>());
    }
    
    // ========== FILE TRANSFER PROTOCOL HANDLERS ==========
    
    /**
     * Inner class to track file transfer session
     */
    private static class FileTransferSession {
        String transferId;
        long roomId;
        String fileName;
        long fileSize;
        int totalChunks;
        java.util.Map<Integer, byte[]> receivedChunks = new java.util.HashMap<>();
        long startTime = System.currentTimeMillis();
        
        public FileTransferSession(String transferId, long roomId, String fileName, long fileSize, int totalChunks) {
            this.transferId = transferId;
            this.roomId = roomId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.totalChunks = totalChunks;
        }
        
        public boolean isComplete() {
            return receivedChunks.size() == totalChunks;
        }
        
        public byte[] assembleFile() {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                for (int i = 0; i < totalChunks; i++) {
                    byte[] chunk = receivedChunks.get(i);
                    if (chunk != null) {
                        baos.write(chunk);
                    }
                }
                return baos.toByteArray();
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    /**
     * Handle FILE_TRANSFER_START
     */
    private void handleFileTransferStart(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            String transferId = data.get("transferId");
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            long fileSize = Long.parseLong(data.get("fileSize"));
            int totalChunks = Integer.parseInt(data.get("totalChunks"));
            
            // Create transfer session
            FileTransferSession session = new FileTransferSession(transferId, roomId, fileName, fileSize, totalChunks);
            activeTransfers.put(transferId, session);
            
            // Notify recipient about incoming file
            long recipientId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler recipient = Server.onlineClients.get(recipientId);
            
            if (recipient != null) {
                java.util.Map<String, String> notif = new java.util.HashMap<>();
                notif.put("transferId", transferId);
                notif.put("roomId", String.valueOf(roomId));
                notif.put("senderId", String.valueOf(currentUser.getId()));
                notif.put("senderName", currentUser.getNickname());
                notif.put("fileName", fileName);
                notif.put("fileSize", String.valueOf(fileSize));
                notif.put("totalChunks", String.valueOf(totalChunks));
                recipient.sendMessage("FILE_TRANSFER_START", notif);
                
                // Create session for recipient too
                recipient.activeTransfers.put(transferId, session);
            }
            
            System.out.println("File transfer started: " + transferId + " - " + fileName);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Invalid file transfer start data");
        }
    }
    
    /**
     * Handle FILE_CHUNK
     */
    private void handleFileChunk(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            String transferId = data.get("transferId");
            int chunkIndex = Integer.parseInt(data.get("chunkIndex"));
            String chunkDataB64 = data.get("chunkData");
            long roomId = Long.parseLong(data.get("roomId"));
            
            FileTransferSession session = activeTransfers.get(transferId);
            if (session == null) {
                System.err.println("No active transfer session for: " + transferId);
                return;
            }
            
            // Decode and store chunk
            byte[] chunkData = java.util.Base64.getDecoder().decode(chunkDataB64);
            session.receivedChunks.put(chunkIndex, chunkData);
            
            // Forward chunk to recipient
            long recipientId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler recipient = Server.onlineClients.get(recipientId);
            
            if (recipient != null) {
                recipient.sendMessage("FILE_CHUNK", data);
            }
            
            System.out.println("Chunk received: " + transferId + " [" + chunkIndex + "/" + session.totalChunks + "]");
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Invalid file chunk data");
        }
    }
    
    /**
     * Handle FILE_TRANSFER_COMPLETE
     */
    private void handleFileTransferComplete(java.util.Map<String, String> data) {
        if (currentUser == null) return;
        try {
            String transferId = data.get("transferId");
            String checksum = data.get("checksum");
            long roomId = Long.parseLong(data.get("roomId"));
            String fileName = data.get("fileName");
            long fileSize = Long.parseLong(data.get("fileSize"));
            
            FileTransferSession session = activeTransfers.get(transferId);
            if (session == null || !session.isComplete()) {
                sendError("Transfer incomplete or not found");
                return;
            }
            
            // Save to database
            String fileInfo = "[FILE] " + fileName + " (" + 
                com.friendzone.util.FileTransferProtocol.formatFileSize(fileSize) + ")";
            chatDAO.sendMessage(roomId, currentUser.getId(), fileInfo, "FILE");
            
            // Notify recipient
            long recipientId = chatDAO.getOtherUserId(roomId, currentUser.getId());
            ClientHandler recipient = Server.onlineClients.get(recipientId);
            
            if (recipient != null) {
                recipient.sendMessage("FILE_TRANSFER_COMPLETE", data);
            }
            
            // Cleanup
            activeTransfers.remove(transferId);
            if (recipient != null) {
                recipient.activeTransfers.remove(transferId);
            }
            
            long duration = System.currentTimeMillis() - session.startTime;
            System.out.println("File transfer completed: " + transferId + " in " + duration + "ms");
            
            // Send success response
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("transferId", transferId);
            response.put("status", "SUCCESS");
            sendMessage("FILE_TRANSFER_SUCCESS", response);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError("File transfer completion failed");
        }
    }
}

