package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.friendzone.client.audio.MulticastAudioReceiver;
import com.friendzone.client.audio.MulticastAudioSender;
import com.friendzone.client.controller.ClientSocket;

/**
 * Cửa sổ chat nhóm
 */
public class GroupChatWindow extends JFrame {
    
    private JTextPane chatPane;
    private StyledDocument chatDoc;
    private JTextField inputField;
    private JButton sendButton;
    private ClientSocket socket;
    private long roomId;
    private long myId;
    private String roomName;
    private MainFrame mainFrame;
    
    // Multicast audio for group voice call
    private MulticastAudioSender audioSender;
    private MulticastAudioReceiver audioReceiver;
    private boolean isInCall = false;
    private JButton btnGroupCall;
    
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(46, 204, 113); // Green
    private static final Color MY_MSG_COLOR = new Color(52, 152, 219); // Blue
    private static final Color OTHER_MSG_COLOR = new Color(60, 60, 80); // Darker Grey
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    public GroupChatWindow(ClientSocket socket, long roomId, long myId, String roomName) {
        this.socket = socket;
        this.roomId = roomId;
        this.myId = myId;
        this.roomName = roomName;
        
        setTitle("Nhóm: " + roomName);
        setSize(550, 650);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout(10, 10));
        
        // Top toolbar với các nút chức năng
        JPanel topToolbar = new JPanel(new BorderLayout());
        topToolbar.setBackground(PANEL_BG);
        topToolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        JLabel titleLabel = new JLabel("Nhóm: " + roomName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        topToolbar.add(titleLabel, BorderLayout.WEST);
        
        JPanel toolButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        toolButtons.setOpaque(false);
        
        JButton btnAddMember = new JButton("Thêm");
        styleToolButton(btnAddMember, new Color(52, 152, 219));
        btnAddMember.addActionListener(e -> addMember());
        
        btnGroupCall = new JButton("🎙️ Gọi nhóm");
        styleToolButton(btnGroupCall, new Color(46, 204, 113));
        btnGroupCall.addActionListener(e -> toggleGroupCall());
        
        JButton btnMembers = new JButton("[+]");
        btnMembers.setToolTipText("Thành viên");
        styleToolButton(btnMembers, new Color(155, 89, 182));
        btnMembers.addActionListener(e -> showMembers());
        
        JButton btnLeave = new JButton("Rời");
        btnLeave.setToolTipText("Rời khỏi nhóm");
        styleToolButton(btnLeave, new Color(231, 76, 60));
        btnLeave.addActionListener(e -> leaveGroup());
        
        toolButtons.add(btnAddMember);
        toolButtons.add(btnGroupCall);
        toolButtons.add(btnMembers);
        toolButtons.add(btnLeave);
        topToolbar.add(toolButtons, BorderLayout.EAST);
        
        add(topToolbar, BorderLayout.NORTH);
        
        // Chat area với JTextPane để hỗ trợ styled text
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(40, 40, 60));
        chatPane.setForeground(TEXT_COLOR);
        chatDoc = chatPane.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        scrollPane.getViewport().setBackground(new Color(40, 40, 60));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with input
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        inputField = new JTextField();
        inputField.setFont(MAIN_FONT);
        inputField.setBackground(new Color(60, 60, 80));
        inputField.setForeground(TEXT_COLOR);
        inputField.setCaretColor(TEXT_COLOR);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 100)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        inputField.addActionListener(e -> sendMessage());
        
        bottomPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        btnPanel.setBackground(BG_COLOR);
        
        JButton btnFile = new JButton("File");
        btnFile.setToolTipText("Gửi file");
        styleToolButton(btnFile, new Color(155, 89, 182));
        btnFile.addActionListener(e -> sendFile());
        
        JButton btnImage = new JButton("Ảnh");
        btnImage.setToolTipText("Gửi ảnh");
        styleToolButton(btnImage, new Color(230, 126, 34));
        btnImage.addActionListener(e -> sendImage());
        
        JButton btnSticker = new JButton("Sticker");
        btnSticker.setToolTipText("Sticker");
        styleToolButton(btnSticker, new Color(241, 196, 15));
        btnSticker.addActionListener(e -> showStickerPicker());
        
        JButton btnSend = new JButton("Gửi");
        styleToolButton(btnSend, ACCENT_COLOR);
        btnSend.addActionListener(e -> sendMessage());
        
        btnPanel.add(btnFile);
        btnPanel.add(btnImage);
        btnPanel.add(btnSticker);
        btnPanel.add(btnSend);
        
        bottomPanel.add(btnPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Lấy lịch sử chat
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", String.valueOf(roomId));
        socket.send("FETCH_GROUP_HISTORY", data);
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    private void styleToolButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Dùng Segoe UI cho tiếng Việt
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }
    
    private void addMember() {
        String userIdStr = JOptionPane.showInputDialog(this, 
            "Nhập ID người dùng muốn thêm:", "Thêm thành viên", JOptionPane.PLAIN_MESSAGE);
        
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                long userId = Long.parseLong(userIdStr.trim());
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("roomId", String.valueOf(roomId));
                data.put("userId", String.valueOf(userId));
                socket.send("ADD_GROUP_MEMBER", data);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Toggle group voice call sử dụng Multicast
     * Multicast cho phép 1 sender broadcast tới N receivers hiệu quả
     */
    private void toggleGroupCall() {
        if (!isInCall) {
            // Bắt đầu call
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bắt đầu cuộc gọi nhóm bằng Multicast?\n" +
                "Tất cả thành viên sẽ nghe thấy nhau.",
                "Gọi nhóm", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                startMulticastCall();
            }
        } else {
            // Kết thúc call
            stopMulticastCall();
        }
    }
    
    /**
     * Bắt đầu Multicast group call
     * Port động dựa trên roomId để tránh conflict
     */
    private void startMulticastCall() {
        try {
            // Port riêng cho mỗi nhóm (base 10000 + roomId)
            int multicastPort = 10000 + (int)(roomId % 1000);
            
            // Khởi động receiver trước (join group)
            audioReceiver = new MulticastAudioReceiver(multicastPort);
            audioReceiver.start();
            
            // Sau đó khởi động sender (broadcast)
            audioSender = new MulticastAudioSender(multicastPort);
            audioSender.start();
            
            isInCall = true;
            btnGroupCall.setText("🔴 Kết thúc");
            btnGroupCall.setBackground(new Color(231, 76, 60)); // Red
            
            appendMessage("Hệ thống", "✅ Đã kết nối Multicast group call (port " + multicastPort + ")", false);
            
            // Thông báo cho server (optional - để notify members khác)
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("action", "JOIN_CALL");
            data.put("port", String.valueOf(multicastPort));
            socket.send("GROUP_CALL_STATUS", data);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Không thể bắt đầu cuộc gọi: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Dừng Multicast group call
     */
    private void stopMulticastCall() {
        try {
            if (audioSender != null) {
                audioSender.stop();
                audioSender = null;
            }
            
            if (audioReceiver != null) {
                audioReceiver.stop();
                audioReceiver = null;
            }
            
            isInCall = false;
            btnGroupCall.setText("🎙️ Gọi nhóm");
            btnGroupCall.setBackground(new Color(46, 204, 113)); // Green
            
            appendMessage("Hệ thống", "❌ Đã ngắt kết nối Multicast call", false);
            
            // Thông báo leave
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("action", "LEAVE_CALL");
            socket.send("GROUP_CALL_STATUS", data);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showMembers() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", String.valueOf(roomId));
        socket.send("GET_GROUP_MEMBERS", data);
    }
    
    private void leaveGroup() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn rời khỏi nhóm này?",
            "Rời nhóm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            socket.send("LEAVE_GROUP", data);
            
            JOptionPane.showMessageDialog(this, "Đã rời khỏi nhóm!");
            dispose(); // Close window
        }
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file để gửi");
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, 
                    "File quá lớn! Giới hạn 10MB.", 
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("fileName", selectedFile.getName());
            data.put("fileSize", String.valueOf(selectedFile.length()));
            data.put("filePath", selectedFile.getAbsolutePath());
            socket.send("SEND_GROUP_FILE", data);
            
            appendMessage("Bạn", "[File] Đã gửi file: " + selectedFile.getName(), true);
        }
    }
    
    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh để gửi");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Ảnh (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, 
                    "Ảnh quá lớn! Giới hạn 5MB.", 
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("fileName", selectedFile.getName());
            data.put("fileType", "IMAGE");
            data.put("filePath", selectedFile.getAbsolutePath());
            socket.send("SEND_GROUP_FILE", data);
            
            appendMessage("Bạn", "[Ảnh] Đã gửi ảnh: " + selectedFile.getName(), true);
        }
    }
    
    private void showStickerPicker() {
        JDialog picker = new JDialog(this, "Chọn Sticker", true);
        picker.setLayout(new GridLayout(6, 5, 5, 5));
        picker.setSize(400, 350);
        picker.setLocationRelativeTo(this);
        picker.getContentPane().setBackground(BG_COLOR);
        
        // Danh sach text emoticons
        String[] stickers = {
            // Cam xuc
            "<3", ":)", ":D", ";)", ":P",
            "^^", ":*", "xD", "T_T", "-_-",
            ":O", ":(", ":'(", ">.<", "O.O",
            // Phan hoi
            "(Y)", "(N)", "OK", "Hi", "Bye",
            "LOL", "GG", "THX", "PLZ", ":3"
        };
        
        for (String code : stickers) {
            JButton btn = new JButton(code);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setBackground(new Color(60, 60, 80));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                inputField.setText(inputField.getText() + code);
                picker.dispose();
            });
            picker.add(btn);
        }
        picker.setVisible(true);
    }
    
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("content", text);
            socket.send("SEND_GROUP_MSG", data);
            
            appendMessage("Bạn", text, true);
            inputField.setText("");
        }
    }
    
    /**
     * Thêm tin nhắn với style (bên phải nền xanh cho mình, bên trái nền xám cho người khác)
     */
    public void appendMessage(String senderName, String content, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                
                if (isMe) {
                    // Tin nhắn của mình - bên phải, nền xanh
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setBackground(attrs, MY_MSG_COLOR);
                } else {
                    // Tin nhắn người khác - bên trái, nền xám
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
                }
                
                StyleConstants.setForeground(attrs, Color.WHITE);
                StyleConstants.setFontFamily(attrs, "Segoe UI");
                StyleConstants.setFontSize(attrs, 13);
                
                // Thêm tên người gửi
                SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
                StyleConstants.setBold(nameAttrs, true);
                StyleConstants.setFontSize(nameAttrs, 11);
                
                int len = chatDoc.getLength();
                chatDoc.insertString(len, "\n", attrs);
                chatDoc.setParagraphAttributes(len, 1, attrs, false);
                
                len = chatDoc.getLength();
                chatDoc.insertString(len, senderName + "\n", nameAttrs);
                chatDoc.setParagraphAttributes(len, senderName.length() + 1, attrs, false);
                
                // Thêm nội dung tin nhắn
                len = chatDoc.getLength();
                chatDoc.insertString(len, content + "\n", attrs);
                chatDoc.setParagraphAttributes(len, content.length() + 1, attrs, false);
                
                // Cuộn xuống cuối
                chatPane.setCaretPosition(chatDoc.getLength());
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    // Backward compatibility
    public void appendMessage(String senderName, String content) {
        boolean isMe = senderName.equals("Bạn") || senderName.equals("Hệ thống");
        appendMessage(senderName, content, isMe);
    }
    
    public void loadHistory(java.util.List<java.util.Map<String, String>> messages) {
        SwingUtilities.invokeLater(() -> {
            try {
                chatDoc.remove(0, chatDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            
            for (java.util.Map<String, String> msg : messages) {
                String senderId = msg.get("senderId");
                String senderName = msg.get("senderName");
                String content = msg.get("content");
                
                boolean isMe = false;
                try {
                    isMe = Long.parseLong(senderId) == myId;
                } catch (NumberFormatException e) {
                    // ignore
                }
                
                appendMessage(senderName, content, isMe);
            }
        });
    }
    
    public long getRoomId() {
        return roomId;
    }
}
