package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.friendzone.client.audio.VoiceRecorder;
import com.friendzone.client.controller.ClientSocket;
import com.friendzone.util.FileTransferProtocol;

public class DMWindow extends JFrame {
    private ClientSocket socket;
    private long roomId;
    private long myId;
    private long otherId;
    private String otherNickname;
    private String myNickname;
    private MainFrame mainFrame;
    
    private JTextPane chatPane;
    private StyledDocument chatDoc;
    private JTextField inputField;
    private JButton sendButton;
    private VoiceRecorder voiceRecorder;
    
    // Store voice data for playback (voiceId -> Base64 data)
    private java.util.Map<String, String> voiceDataMap = new java.util.HashMap<>();
    
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(46, 204, 113); // Green
    private static final Color MY_MSG_COLOR = new Color(52, 152, 219); // Blue
    private static final Color OTHER_MSG_COLOR = new Color(60, 60, 80); // Darker Grey
    
    public DMWindow(ClientSocket socket, long roomId, long myId, long otherId, String otherNickname) {
        this.socket = socket;
        this.roomId = roomId;
        this.myId = myId;
        this.otherId = otherId;
        this.otherNickname = otherNickname;
        this.myNickname = "Bạn"; // Default, will be updated from server if needed
        
        initUI();
        fetchHistory();
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    private void initUI() {
        setTitle("Trò chuyện với " + otherNickname);
        setSize(500, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout(10, 10));
        
        // Top toolbar với các nút chức năng
        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        topToolbar.setBackground(PANEL_BG);
        topToolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("Chat: " + otherNickname);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        leftPanel.add(titleLabel);
        
        JButton btnVideoCall = new JButton("📹 Video");
        styleToolButton(btnVideoCall, new Color(46, 204, 113));
        btnVideoCall.addActionListener(e -> startVideoCall());
        
        JButton btnVoiceCall = new JButton("📞 Gọi");
        styleToolButton(btnVoiceCall, new Color(52, 152, 219));
        btnVoiceCall.addActionListener(e -> startVoiceCall());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_BG);
        headerPanel.add(leftPanel, BorderLayout.WEST);
        
        JPanel btnToolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnToolPanel.setOpaque(false);
        btnToolPanel.add(btnVoiceCall);
        btnToolPanel.add(btnVideoCall);
        headerPanel.add(btnToolPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Khu vực chat với JTextPane để hỗ trợ styled text
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(40, 40, 60));
        chatPane.setForeground(TEXT_COLOR);
        chatDoc = chatPane.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        scrollPane.getViewport().setBackground(new Color(40, 40, 60));
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel nhập tin nhắn
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(BG_COLOR);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(60, 60, 80));
        inputField.setForeground(TEXT_COLOR);
        inputField.setCaretColor(TEXT_COLOR);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 100)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        inputField.addActionListener(e -> sendMessage());
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 6, 5, 0));
        btnPanel.setBackground(BG_COLOR);
        
        JButton btnFile = new JButton("File");
        btnFile.setToolTipText("Gửi file");
        styleToolButton(btnFile, new Color(155, 89, 182));
        btnFile.addActionListener(e -> sendFile());
        
        JButton btnFolder = new JButton("Folder");
        btnFolder.setToolTipText("Gửi thư mục");
        styleToolButton(btnFolder, new Color(52, 152, 219));
        btnFolder.addActionListener(e -> sendFolder());
        
        JButton btnImage = new JButton("Ảnh");
        btnImage.setToolTipText("Gửi ảnh");
        styleToolButton(btnImage, new Color(230, 126, 34));
        btnImage.addActionListener(e -> sendImage());
        
        JButton btnSticker = new JButton("Sticker");
        btnSticker.setToolTipText("Sticker");
        styleToolButton(btnSticker, new Color(241, 196, 15));
        btnSticker.addActionListener(e -> showStickerPicker());
        
        JButton btnVoice = new JButton("🎤");
        btnVoice.setToolTipText("Giữ để ghi âm");
        styleToolButton(btnVoice, new Color(231, 76, 60));
        setupVoiceButton(btnVoice);
        
        JButton btnSend = new JButton("Gửi");
        styleToolButton(btnSend, new Color(46, 204, 113));
        btnSend.addActionListener(e -> sendMessage());
        
        btnPanel.add(btnFile);
        btnPanel.add(btnFolder);
        btnPanel.add(btnImage);
        btnPanel.add(btnSticker);
        btnPanel.add(btnVoice);
        btnPanel.add(btnSend);
        
        inputPanel.add(btnPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
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
    
    private void startVideoCall() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn muốn gọi video cho " + otherNickname + "?",
            "Gọi Video", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("targetId", String.valueOf(otherId));
            data.put("callType", "VIDEO");
            // Gửi kèm IP thực của client
            try {
                String myLocalIp = java.net.InetAddress.getLocalHost().getHostAddress();
                data.put("myIp", myLocalIp);
            } catch (java.net.UnknownHostException e) {
                data.put("myIp", "127.0.0.1");
            }
            socket.send("VIDEO_CALL_REQUEST", data);
            appendMessage("Hệ thống", "Đang gọi video cho " + otherNickname + "...", false);
        }
    }
    
    private void startVoiceCall() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn muốn gọi thoại cho " + otherNickname + "?",
            "Gọi thoại", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("targetId", String.valueOf(otherId));
            data.put("callType", "AUDIO"); // Đổi từ VOICE thành AUDIO để khớp với database
            // Gửi kèm IP thực của client
            try {
                String myLocalIp = java.net.InetAddress.getLocalHost().getHostAddress();
                data.put("myIp", myLocalIp);
            } catch (java.net.UnknownHostException e) {
                data.put("myIp", "127.0.0.1");
            }
            socket.send("VIDEO_CALL_REQUEST", data);
            appendMessage("Hệ thống", "Đang gọi thoại cho " + otherNickname + "...", false);
        }
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file để gửi");
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Warning for very large files (500MB+)
            if (selectedFile.length() > FileTransferProtocol.WARNING_FILE_SIZE) {
                int warning = JOptionPane.showConfirmDialog(this,
                    "File rất lớn (" + FileTransferProtocol.formatFileSize(selectedFile.length()) + ").\n" +
                    "Có thể mất nhiều thời gian. Vẫn muốn gửi?",
                    "Cảnh báo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (warning != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Show confirmation
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Gửi file: " + selectedFile.getName() + "\n" +
                "Kích thước: " + com.friendzone.util.FileTransferProtocol.formatFileSize(selectedFile.length()) + "\n\n" +
                "Tiếp tục?",
                "Xác nhận gửi file", JOptionPane.YES_NO_OPTION);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            // Send file with progress bar in background thread
            sendFileWithProgress(selectedFile);
        }
    }
    
    /**
     * Gửi file với progress bar (chunked transfer)
     */
    private void sendFileWithProgress(File file) {
        // Create progress dialog
        JDialog progressDialog = new JDialog(this, "Đang gửi file", true);
        progressDialog.setSize(450, 150);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel contentPanel = new JPanel(new BorderLayout(5, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(PANEL_BG);
        
        JLabel fileLabel = new JLabel("📎 " + file.getName());
        fileLabel.setForeground(TEXT_COLOR);
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(fileLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(new Color(40, 40, 60));
        progressBar.setPreferredSize(new Dimension(400, 30));
        contentPanel.add(progressBar, BorderLayout.CENTER);
        
        JLabel statusLabel = new JLabel("Đang chuẩn bị...");
        statusLabel.setForeground(new Color(150, 150, 170));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        
        progressDialog.add(contentPanel);
        
        // Background thread to send file
        new Thread(() -> {
            try {
                String transferId = com.friendzone.util.FileTransferProtocol.generateTransferId(myId, otherId);
                long fileSize = file.length();
                int totalChunks = com.friendzone.util.FileTransferProtocol.calculateTotalChunks(fileSize);
                
                // Send START message
                SwingUtilities.invokeLater(() -> statusLabel.setText("Bắt đầu truyền file..."));
                java.util.Map<String, String> startData = 
                    com.friendzone.util.FileTransferProtocol.createStartMessage(
                        transferId, roomId, file.getName(), fileSize);
                socket.send("FILE_TRANSFER_START", startData);
                
                // Send chunks
                for (int i = 0; i < totalChunks; i++) {
                    byte[] chunkData = com.friendzone.util.FileTransferProtocol.readChunk(file, i);
                    java.util.Map<String, String> chunkMsg = 
                        com.friendzone.util.FileTransferProtocol.createChunkMessage(
                            transferId, i, totalChunks, chunkData);
                    chunkMsg.put("roomId", String.valueOf(roomId));
                    socket.send("FILE_CHUNK", chunkMsg);
                    
                    // Update progress
                    int percent = (i + 1) * 100 / totalChunks;
                    int finalI = i;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(percent);
                        progressBar.setString(percent + "%");
                        statusLabel.setText("Chunk " + (finalI + 1) + "/" + totalChunks + 
                            " (" + com.friendzone.util.FileTransferProtocol.formatFileSize(
                                (long)(finalI + 1) * com.friendzone.util.FileTransferProtocol.CHUNK_SIZE) + ")");
                    });
                    
                    Thread.sleep(10); // Small delay to prevent overwhelming
                }
                
                // Send COMPLETE message
                String checksum = com.friendzone.util.FileTransferProtocol.calculateChecksum(file);
                java.util.Map<String, String> completeData = 
                    com.friendzone.util.FileTransferProtocol.createCompleteMessage(transferId, checksum);
                completeData.put("roomId", String.valueOf(roomId));
                completeData.put("fileName", file.getName());
                completeData.put("fileSize", String.valueOf(fileSize));
                socket.send("FILE_TRANSFER_COMPLETE", completeData);
                
                // Show success and close dialog
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("✓ Hoàn thành!");
                    statusLabel.setText("File đã được gửi thành công");
                    appendMessage("Bạn", "[File] " + file.getName() + " (" + 
                        com.friendzone.util.FileTransferProtocol.formatFileSize(fileSize) + ")", true);
                });
                
                Thread.sleep(1000); // Show success for 1 second
                SwingUtilities.invokeLater(() -> progressDialog.dispose());
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(DMWindow.this, 
                        "Không thể gửi file: " + e.getMessage(), 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
        
        // Show dialog (blocks until closed)
        progressDialog.setVisible(true);
    }
    
    /**
     * Gửi thư mục với tất cả files bên trong
     */
    private void sendFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setDialogTitle("Chọn thư mục để gửi");
        int result = folderChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            
            try {
                // Scan folder
                int fileCount = com.friendzone.util.FolderTransferProtocol.countFiles(selectedFolder);
                long totalSize = com.friendzone.util.FolderTransferProtocol.calculateFolderSize(selectedFolder);
                
                if (fileCount == 0) {
                    JOptionPane.showMessageDialog(this,
                        "Thư mục rỗng!",
                        "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Warning for large folders
                if (totalSize > FileTransferProtocol.WARNING_FILE_SIZE) {
                    int warning = JOptionPane.showConfirmDialog(this,
                        "Thư mục rất lớn:\n" +
                        "- Số file: " + fileCount + "\n" +
                        "- Tổng dung lượng: " + FileTransferProtocol.formatFileSize(totalSize) + "\n\n" +
                        "Có thể mất nhiều thời gian. Vẫn muốn gửi?",
                        "Cảnh báo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (warning != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                
                // Show confirmation
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Gửi thư mục: " + selectedFolder.getName() + "\n" +
                    "Số file: " + fileCount + "\n" +
                    "Tổng dung lượng: " + FileTransferProtocol.formatFileSize(totalSize) + "\n\n" +
                    "Tiếp tục?",
                    "Xác nhận gửi thư mục", JOptionPane.YES_NO_OPTION);
                
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                
                // Send folder with progress
                sendFolderWithProgress(selectedFolder, fileCount, totalSize);
                
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi đọc thư mục: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Gửi thư mục với progress bar
     */
    private void sendFolderWithProgress(File folder, int totalFiles, long totalSize) {
        // Create progress dialog
        JDialog progressDialog = new JDialog(this, "Đang gửi thư mục", true);
        progressDialog.setSize(500, 180);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel contentPanel = new JPanel(new BorderLayout(5, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(PANEL_BG);
        
        JLabel folderLabel = new JLabel("📁 " + folder.getName() + " (" + totalFiles + " files)");
        folderLabel.setForeground(TEXT_COLOR);
        folderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(folderLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(new Color(52, 152, 219));
        progressBar.setBackground(new Color(40, 40, 60));
        progressBar.setPreferredSize(new Dimension(450, 30));
        contentPanel.add(progressBar, BorderLayout.CENTER);
        
        JLabel statusLabel = new JLabel("Đang quét thư mục...");
        statusLabel.setForeground(new Color(150, 150, 170));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        
        progressDialog.add(contentPanel);
        
        // Background thread to send folder
        new Thread(() -> {
            try {
                String transferId = FileTransferProtocol.generateTransferId(myId, otherId);
                
                // Scan all files in folder
                SwingUtilities.invokeLater(() -> statusLabel.setText("Đang quét files..."));
                List<com.friendzone.util.FolderTransferProtocol.FileEntry> fileEntries = 
                    com.friendzone.util.FolderTransferProtocol.scanFolder(folder);
                
                // Send FOLDER_START
                SwingUtilities.invokeLater(() -> statusLabel.setText("Bắt đầu gửi thư mục..."));
                java.util.Map<String, String> folderStartData = 
                    com.friendzone.util.FolderTransferProtocol.createFolderStartMessage(
                        transferId, folder.getName(), fileEntries.size(), totalSize);
                folderStartData.put("roomId", String.valueOf(roomId));
                socket.send("FOLDER_TRANSFER_START", folderStartData);
                
                Thread.sleep(100); // Small delay
                
                // Send each file
                int fileIndex = 0;
                long bytesSent = 0;
                
                for (com.friendzone.util.FolderTransferProtocol.FileEntry entry : fileEntries) {
                    fileIndex++;
                    final int currentIndex = fileIndex;
                    final String fileName = entry.relativePath;
                    
                    SwingUtilities.invokeLater(() -> 
                        statusLabel.setText("Đang gửi: " + fileName + " (" + currentIndex + "/" + totalFiles + ")"));
                    
                    // Send this file with FILE_TRANSFER protocol
                    String fileTransferId = transferId + "_file_" + fileIndex;
                    long fileSize = entry.size;
                    int totalChunks = FileTransferProtocol.calculateTotalChunks(fileSize);
                    
                    // Send FILE_START for this file
                    java.util.Map<String, String> fileStartData = new HashMap<>();
                    fileStartData.put("transferId", fileTransferId);
                    fileStartData.put("folderTransferId", transferId);
                    fileStartData.put("roomId", String.valueOf(roomId));
                    fileStartData.put("fileName", entry.relativePath);
                    fileStartData.put("fileSize", String.valueOf(fileSize));
                    fileStartData.put("totalChunks", String.valueOf(totalChunks));
                    socket.send("FILE_IN_FOLDER_START", fileStartData);
                    
                    // Send chunks for this file
                    for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                        byte[] chunkData = FileTransferProtocol.readChunk(entry.file, chunkIndex);
                        
                        java.util.Map<String, String> chunkMsg = new HashMap<>();
                        chunkMsg.put("transferId", fileTransferId);
                        chunkMsg.put("chunkIndex", String.valueOf(chunkIndex));
                        chunkMsg.put("chunkData", Base64.getEncoder().encodeToString(chunkData));
                        socket.send("FILE_IN_FOLDER_CHUNK", chunkMsg);
                        
                        Thread.sleep(5); // Small delay between chunks
                    }
                    
                    // Send FILE_COMPLETE for this file
                    java.util.Map<String, String> fileCompleteData = new HashMap<>();
                    fileCompleteData.put("transferId", fileTransferId);
                    fileCompleteData.put("checksum", FileTransferProtocol.calculateChecksum(entry.file));
                    socket.send("FILE_IN_FOLDER_COMPLETE", fileCompleteData);
                    
                    // Update overall progress
                    bytesSent += fileSize;
                    final int overallProgress = (int) ((bytesSent * 100) / totalSize);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(overallProgress);
                        progressBar.setString(overallProgress + "%");
                    });
                    
                    Thread.sleep(50); // Delay between files
                }
                
                // Send FOLDER_COMPLETE
                java.util.Map<String, String> folderCompleteData = 
                    com.friendzone.util.FolderTransferProtocol.createFolderCompleteMessage(
                        transferId, fileEntries.size(), totalSize);
                socket.send("FOLDER_TRANSFER_COMPLETE", folderCompleteData);
                
                // Show success
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("Hoàn thành!");
                    statusLabel.setText("Đã gửi " + totalFiles + " files");
                    
                    // Display in chat
                    appendMessage(myNickname, 
                        "📁 Đã gửi thư mục: " + folder.getName() + 
                        " (" + totalFiles + " files, " + 
                        FileTransferProtocol.formatFileSize(totalSize) + ")", 
                        true);
                });
                
                Thread.sleep(1000);
                SwingUtilities.invokeLater(() -> progressDialog.dispose());
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Lỗi khi gửi thư mục: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
        
        progressDialog.setVisible(true);
    }
    
    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh để gửi");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Ảnh (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > 5 * 1024 * 1024) { // Recommend sendFile for large images
                int recommend = JOptionPane.showConfirmDialog(this,
                    "Ảnh lớn (" + FileTransferProtocol.formatFileSize(selectedFile.length()) + ").\n" +
                    "Dùng 'Gửi File' để có progress bar?\n\n" +
                    "Vẫn gửi nhanh (không có progress)?",
                    "Gợi ý", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (recommend != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                // Đọc file và encode thành Base64
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("roomId", String.valueOf(roomId));
                data.put("fileName", selectedFile.getName());
                data.put("fileType", "IMAGE");
                data.put("imageData", base64Data); // Gửi dữ liệu ảnh Base64
                socket.send("SEND_IMAGE", data);
                
                // Hiển thị ảnh cho người gửi
                appendImageMessage("Bạn", selectedFile, true);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Không thể đọc file ảnh: " + e.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void setupVoiceButton(JButton btnVoice) {
        btnVoice.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startVoiceRecording(btnVoice);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                stopVoiceRecording(btnVoice);
            }
        });
    }
    
    private void startVoiceRecording(JButton btn) {
        voiceRecorder = new VoiceRecorder();
        if (voiceRecorder.startRecording()) {
            btn.setText("⏺️");
            btn.setBackground(Color.RED);
            btn.setToolTipText("Đang ghi âm... Thả ra để gửi");
        } else {
            JOptionPane.showMessageDialog(this, 
                "Không thể truy cập microphone!", 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            voiceRecorder = null;
        }
    }
    
    private void stopVoiceRecording(JButton btn) {
        if (voiceRecorder == null) return;
        
        btn.setText("🎤");
        btn.setBackground(new Color(231, 76, 60));
        btn.setToolTipText("Giữ để ghi âm");
        
        byte[] wavData = voiceRecorder.stopRecording();
        voiceRecorder = null;
        
        if (wavData == null || wavData.length < 1000) {
            JOptionPane.showMessageDialog(this, 
                "Ghi âm quá ngắn!", 
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get duration
        int duration = new VoiceRecorder().getDuration(wavData);
        
        try {
            String base64Data = Base64.getEncoder().encodeToString(wavData);
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("fileName", "voice_" + System.currentTimeMillis() + ".wav");
            data.put("duration", String.valueOf(duration));
            data.put("voiceData", base64Data);
            socket.send("SEND_VOICE", data);
            
            // Hiển thị voice message cho người gửi
            appendVoiceMessage("Bạn", duration, base64Data, true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Không thể gửi tin nhắn thoại: " + e.getMessage(), 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void fetchHistory() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", String.valueOf(roomId));
        socket.send("FETCH_HISTORY", data);
    }
    
    private void showStickerPicker() {
        JDialog picker = new JDialog(this, "Chọn Sticker", true);
        picker.setSize(450, 500);
        picker.setLocationRelativeTo(this);
        picker.getContentPane().setBackground(BG_COLOR);
        picker.setLayout(new BorderLayout());
        
        // Tabbed pane cho Emoji và Sticker ảnh
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_COLOR);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // ===== TAB 1: EMOJI =====
        JPanel emojiPanel = new JPanel(new GridLayout(6, 5, 5, 5));
        emojiPanel.setBackground(BG_COLOR);
        emojiPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] emojis = {
            "<3", ":)", ":D", ";)", ":P",
            "^^", ":*", "xD", "T_T", "-_-",
            ":O", ":(", ":'(", ">.<", "O.O",
            "(Y)", "(N)", "OK", "Hi", "Bye"
        };
        
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            btn.setBackground(new Color(60, 60, 80));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                inputField.setText(inputField.getText() + emoji);
                picker.dispose();
            });
            emojiPanel.add(btn);
        }
        
        JScrollPane emojiScroll = new JScrollPane(emojiPanel);
        emojiScroll.setBorder(null);
        emojiScroll.getViewport().setBackground(BG_COLOR);
        tabbedPane.addTab("😊 Emoji", emojiScroll);
        
        // ===== TAB 2: STICKER ẢNH (từ thư mục hinh) =====
        JPanel stickerPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        stickerPanel.setBackground(BG_COLOR);
        stickerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Load stickers từ thư mục hinh
        String stickerPath = "hinh";
        File stickerDir = new File(stickerPath);
        if (!stickerDir.exists()) {
            // Thử đường dẫn tuyệt đối
            stickerDir = new File("D:\\GAMEFRIENDZONE\\hinh");
        }
        
        if (stickerDir.exists() && stickerDir.isDirectory()) {
            File[] stickerFiles = stickerDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".webp") || 
                name.toLowerCase().endsWith(".png") || 
                name.toLowerCase().endsWith(".gif") ||
                name.toLowerCase().endsWith(".jpg"));
            
            if (stickerFiles != null) {
                for (File stickerFile : stickerFiles) {
                    try {
                        // Load và resize ảnh sticker
                        ImageIcon originalIcon = new ImageIcon(stickerFile.getAbsolutePath());
                        Image img = originalIcon.getImage();
                        Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        ImageIcon stickerIcon = new ImageIcon(scaledImg);
                        
                        JButton stickerBtn = new JButton(stickerIcon);
                        stickerBtn.setBackground(new Color(60, 60, 80));
                        stickerBtn.setFocusPainted(false);
                        stickerBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 2));
                        stickerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        stickerBtn.setToolTipText(stickerFile.getName());
                        stickerBtn.setPreferredSize(new Dimension(110, 110));
                        
                        // Hover effect
                        stickerBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                            public void mouseEntered(java.awt.event.MouseEvent evt) {
                                stickerBtn.setBorder(BorderFactory.createLineBorder(new Color(100, 255, 218), 3));
                            }
                            public void mouseExited(java.awt.event.MouseEvent evt) {
                                stickerBtn.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 2));
                            }
                        });
                        
                        final File finalFile = stickerFile;
                        stickerBtn.addActionListener(e -> {
                            sendSticker(finalFile);
                            picker.dispose();
                        });
                        
                        stickerPanel.add(stickerBtn);
                    } catch (Exception e) {
                        System.err.println("Không thể load sticker: " + stickerFile.getName());
                    }
                }
            }
        }
        
        // Nếu không có sticker, hiển thị thông báo
        if (stickerPanel.getComponentCount() == 0) {
            JLabel noSticker = new JLabel("Chưa có sticker nào!", SwingConstants.CENTER);
            noSticker.setForeground(Color.GRAY);
            noSticker.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            stickerPanel.add(noSticker);
        }
        
        JScrollPane stickerScroll = new JScrollPane(stickerPanel);
        stickerScroll.setBorder(null);
        stickerScroll.getViewport().setBackground(BG_COLOR);
        stickerScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Sticker", stickerScroll);
        
        picker.add(tabbedPane, BorderLayout.CENTER);
        picker.setVisible(true);
    }
    
    /**
     * Gửi sticker (ảnh từ file)
     */
    private void sendSticker(File stickerFile) {
        try {
            // Đọc file và encode thành Base64
            byte[] fileBytes = Files.readAllBytes(stickerFile.toPath());
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("fileName", stickerFile.getName());
            data.put("fileType", "STICKER");
            data.put("stickerData", base64Data);
            socket.send("SEND_STICKER", data);
            
            // Hiển thị sticker cho người gửi
            appendStickerMessage("Bạn", stickerFile, true);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Không thể gửi sticker: " + e.getMessage(), 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Hiển thị sticker từ File
     */
    public void appendStickerMessage(String sender, File stickerFile, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                ImageIcon originalIcon = new ImageIcon(stickerFile.getAbsolutePath());
                Image img = originalIcon.getImage();
                
                // Resize sticker (150x150)
                Image scaledImg = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaledImg);
                
                insertImageToChat(sender, icon, isMe);
            } catch (Exception e) {
                appendMessage(sender, "[Sticker]", isMe);
            }
        });
    }
    
    /**
     * Hiển thị sticker từ Base64
     */
    public void appendStickerFromBase64(String sender, String base64Data, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                ImageIcon originalIcon = new ImageIcon(imageBytes);
                Image img = originalIcon.getImage();
                
                // Resize sticker (150x150)
                Image scaledImg = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaledImg);
                
                // Dùng method mới để có thể click xem/lưu
                insertClickableImageToChat(sender, icon, originalIcon, base64Data, isMe, "sticker");
            } catch (Exception e) {
                appendMessage(sender, "[Sticker]", isMe);
            }
        });
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (!content.isEmpty()) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("roomId", String.valueOf(roomId));
            data.put("content", content);
            socket.send("SEND_DM", data);
            
            appendMessage("Bạn", content, true);
            inputField.setText("");
        }
    }
    
    /**
     * Thêm tin nhắn với style (bên phải nền xanh cho mình, bên trái nền xám cho người khác)
     */
    public void appendMessage(String sender, String content, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Tạo style cho tin nhắn
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
                chatDoc.insertString(len, sender + "\n", nameAttrs);
                chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
                
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
    public void appendMessage(String sender, String content) {
        boolean isMe = sender.equals("Bạn") || sender.equals("Hệ thống");
        appendMessage(sender, content, isMe);
    }
    
    /**
     * Hiển thị ảnh từ File
     */
    public void appendImageMessage(String sender, File imageFile, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                Image img = ImageIO.read(imageFile);
                if (img != null) {
                    // Resize ảnh nếu quá lớn (max 300px width)
                    int maxWidth = 300;
                    int width = img.getWidth(null);
                    int height = img.getHeight(null);
                    if (width > maxWidth) {
                        height = (int) ((double) height / width * maxWidth);
                        width = maxWidth;
                    }
                    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaledImg);
                    
                    insertImageToChat(sender, icon, isMe);
                }
            } catch (IOException e) {
                appendMessage(sender, "[Khong the hien thi anh]", isMe);
            }
        });
    }
    
    /**
     * Hiển thị ảnh từ Base64
     */
    public void appendImageFromBase64(String sender, String base64Data, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                ImageIcon originalIcon = new ImageIcon(imageBytes);
                Image img = originalIcon.getImage();
                
                // Resize ảnh nếu quá lớn (max 300px width)
                int maxWidth = 300;
                int width = img.getWidth(null);
                int height = img.getHeight(null);
                if (width > maxWidth) {
                    height = (int) ((double) height / width * maxWidth);
                    width = maxWidth;
                }
                Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaledImg);
                
                // Chèn ảnh có thể click để xem full và lưu
                insertClickableImageToChat(sender, icon, originalIcon, base64Data, isMe, "image");
            } catch (Exception e) {
                appendMessage(sender, "[Khong the hien thi anh]", isMe);
            }
        });
    }
    
    /**
     * Chèn ảnh đơn giản vào chat (không click được)
     */
    private void insertImageToChat(String sender, ImageIcon icon, boolean isMe) {
        try {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (isMe) {
                StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                StyleConstants.setBackground(attrs, MY_MSG_COLOR);
            } else {
                StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
            }
            StyleConstants.setForeground(attrs, Color.WHITE);
            StyleConstants.setFontFamily(attrs, "Segoe UI");
            StyleConstants.setFontSize(attrs, 13);
            
            // Tên người gửi
            SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
            StyleConstants.setBold(nameAttrs, true);
            StyleConstants.setFontSize(nameAttrs, 11);
            
            int len = chatDoc.getLength();
            chatDoc.insertString(len, "\n", attrs);
            chatDoc.setParagraphAttributes(len, 1, attrs, false);
            
            len = chatDoc.getLength();
            chatDoc.insertString(len, sender + "\n", nameAttrs);
            chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
            
            // Chèn ảnh
            len = chatDoc.getLength();
            Style style = chatPane.addStyle("ImageStyle", null);
            StyleConstants.setIcon(style, icon);
            chatDoc.insertString(len, " ", style);
            chatDoc.insertString(chatDoc.getLength(), "\n", attrs);
            
            // Cuộn xuống cuối
            chatPane.setCaretPosition(chatDoc.getLength());
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Chèn ảnh có thể click vào chat
     */
    private void insertClickableImageToChat(String sender, ImageIcon thumbIcon, ImageIcon fullIcon, 
                                            String base64Data, boolean isMe, String type) {
        try {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (isMe) {
                StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                StyleConstants.setBackground(attrs, MY_MSG_COLOR);
            } else {
                StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
            }
            StyleConstants.setForeground(attrs, Color.WHITE);
            StyleConstants.setFontFamily(attrs, "Segoe UI");
            StyleConstants.setFontSize(attrs, 13);
            
            // Tên người gửi
            SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
            StyleConstants.setBold(nameAttrs, true);
            StyleConstants.setFontSize(nameAttrs, 11);
            
            int len = chatDoc.getLength();
            chatDoc.insertString(len, "\n", attrs);
            chatDoc.setParagraphAttributes(len, 1, attrs, false);
            
            len = chatDoc.getLength();
            chatDoc.insertString(len, sender + "\n", nameAttrs);
            chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
            
            // Tạo JLabel chứa ảnh có thể click
            JLabel imgLabel = new JLabel(thumbIcon);
            imgLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            imgLabel.setToolTipText("Click để xem ảnh lớn | Chuột phải để lưu");
            
            // Click để xem ảnh lớn hoặc lưu
            imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                        // Click trái - xem ảnh full size
                        showFullImage(fullIcon, type);
                    } else if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                        // Click phải - menu lưu
                        showImageContextMenu(e, base64Data, type);
                    }
                }
            });
            
            // Chèn component vào chat
            len = chatDoc.getLength();
            Style style = chatPane.addStyle("ImageComponent", null);
            StyleConstants.setComponent(style, imgLabel);
            chatDoc.insertString(len, " ", style);
            chatDoc.insertString(chatDoc.getLength(), "\n", attrs);
            
            // Cuộn xuống cuối
            chatPane.setCaretPosition(chatDoc.getLength());
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Hiển thị ảnh full size trong dialog
     */
    private void showFullImage(ImageIcon fullIcon, String type) {
        JDialog dialog = new JDialog(this, type.equals("sticker") ? "Sticker" : "Xem ảnh", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 30, 30));
        
        // Resize nếu ảnh quá lớn so với màn hình
        Image img = fullIcon.getImage();
        int imgW = img.getWidth(null);
        int imgH = img.getHeight(null);
        
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = (int)(screenSize.width * 0.8);
        int maxH = (int)(screenSize.height * 0.8);
        
        if (imgW > maxW || imgH > maxH) {
            double ratio = Math.min((double)maxW / imgW, (double)maxH / imgH);
            imgW = (int)(imgW * ratio);
            imgH = (int)(imgH * ratio);
            img = img.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH);
        }
        
        JLabel imgLabel = new JLabel(new ImageIcon(img));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JScrollPane scroll = new JScrollPane(imgLabel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(30, 30, 30));
        dialog.add(scroll, BorderLayout.CENTER);
        
        // Nút đóng
        JButton btnClose = new JButton("Đóng");
        btnClose.setBackground(new Color(80, 80, 80));
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> dialog.dispose());
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(40, 40, 40));
        bottomPanel.add(btnClose);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setSize(Math.min(imgW + 50, maxW), Math.min(imgH + 100, maxH));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    /**
     * Hiển thị context menu để lưu ảnh
     */
    private void showImageContextMenu(java.awt.event.MouseEvent e, String base64Data, String type) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem saveItem = new JMenuItem("💾 Lưu " + (type.equals("sticker") ? "sticker" : "ảnh"));
        saveItem.addActionListener(ev -> saveImageFromBase64(base64Data, type));
        menu.add(saveItem);
        
        JMenuItem viewItem = new JMenuItem("Xem anh lon");
        viewItem.addActionListener(ev -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                ImageIcon icon = new ImageIcon(bytes);
                showFullImage(icon, type);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(DMWindow.this, "Không thể hiển thị ảnh!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        menu.add(viewItem);
        
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * Lưu ảnh từ Base64
     */
    private void saveImageFromBase64(String base64Data, String type) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu " + (type.equals("sticker") ? "sticker" : "ảnh"));
        
        String defaultName = type.equals("sticker") ? "sticker.png" : "image_" + System.currentTimeMillis() + ".png";
        chooser.setSelectedFile(new java.io.File(defaultName));
        
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Ảnh (PNG, JPG)", "png", "jpg", "jpeg"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                java.io.File file = chooser.getSelectedFile();
                
                // Thêm extension nếu chưa có
                String path = file.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".png") && !path.toLowerCase().endsWith(".jpg")) {
                    file = new java.io.File(path + ".png");
                }
                
                Files.write(file.toPath(), imageBytes);
                JOptionPane.showMessageDialog(this, 
                    "[OK] Da luu: " + file.getName(), "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "[Loi] Khong the luu: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Hiển thị file nhận được với khả năng click để lưu
     */
    public void appendFileMessage(String sender, String fileName, String fileDataBase64, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                if (isMe) {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setBackground(attrs, MY_MSG_COLOR);
                } else {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
                }
                StyleConstants.setForeground(attrs, Color.WHITE);
                StyleConstants.setFontFamily(attrs, "Segoe UI");
                StyleConstants.setFontSize(attrs, 13);
                
                // Tên người gửi
                SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
                StyleConstants.setBold(nameAttrs, true);
                StyleConstants.setFontSize(nameAttrs, 11);
                
                int len = chatDoc.getLength();
                chatDoc.insertString(len, "\n", attrs);
                chatDoc.setParagraphAttributes(len, 1, attrs, false);
                
                len = chatDoc.getLength();
                chatDoc.insertString(len, sender + "\n", nameAttrs);
                chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
                
                // Tạo panel chứa thông tin file có thể click
                JPanel filePanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 5));
                filePanel.setBackground(isMe ? MY_MSG_COLOR : OTHER_MSG_COLOR);
                filePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                
                JLabel iconLabel = new JLabel("[File]");
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                filePanel.add(iconLabel);
                
                JLabel nameLabel = new JLabel(fileName);
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                filePanel.add(nameLabel);
                
                // Nếu có data và không phải tin của mình, thêm nút lưu
                if (fileDataBase64 != null && !fileDataBase64.isEmpty()) {
                    JButton btnSave = new JButton("💾 Lưu");
                    btnSave.setBackground(new Color(76, 175, 80));
                    btnSave.setForeground(Color.WHITE);
                    btnSave.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    btnSave.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    btnSave.addActionListener(e -> saveFile(fileName, fileDataBase64));
                    filePanel.add(btnSave);
                    
                    // Cũng có thể click vào panel để lưu
                    filePanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    filePanel.setToolTipText("Click để lưu file");
                    filePanel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (e.getSource() == filePanel) {
                                saveFile(fileName, fileDataBase64);
                            }
                        }
                    });
                }
                
                // Chèn component vào chat
                len = chatDoc.getLength();
                Style style = chatPane.addStyle("FileComponent", null);
                StyleConstants.setComponent(style, filePanel);
                chatDoc.insertString(len, " ", style);
                chatDoc.insertString(chatDoc.getLength(), "\n", attrs);
                
                // Cuộn xuống cuối
                chatPane.setCaretPosition(chatDoc.getLength());
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Hiển thị voice message với nút play (chưa implement phát audio)
     */
    public void appendVoiceMessage(String sender, int duration, String voiceData, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Generate unique ID for this voice message
                String voiceId = "voice_" + System.currentTimeMillis() + "_" + sender.hashCode();
                
                // Store voice data for playback
                if (voiceData != null && !voiceData.isEmpty()) {
                    voiceDataMap.put(voiceId, voiceData);
                }
                
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                if (isMe) {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setBackground(attrs, MY_MSG_COLOR);
                } else {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
                }
                StyleConstants.setForeground(attrs, Color.WHITE);
                StyleConstants.setFontFamily(attrs, "Segoe UI");
                StyleConstants.setFontSize(attrs, 13);
                
                int len = chatDoc.getLength();
                chatDoc.insertString(len, "\n", attrs);
                chatDoc.setParagraphAttributes(len, 1, attrs, false);
                
                SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
                StyleConstants.setBold(nameAttrs, true);
                StyleConstants.setFontSize(nameAttrs, 11);
                
                len = chatDoc.getLength();
                chatDoc.insertString(len, sender + "\n", nameAttrs);
                chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
                
                // Voice message panel
                JPanel voicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
                voicePanel.setBackground(isMe ? MY_MSG_COLOR : OTHER_MSG_COLOR);
                voicePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 100, 120)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
                
                JLabel icon = new JLabel("🎙️");
                icon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
                voicePanel.add(icon);
                
                JLabel durLabel = new JLabel(duration + "s");
                durLabel.setForeground(Color.WHITE);
                durLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                voicePanel.add(durLabel);
                
                // Play button - now functional!
                JButton btnPlay = new JButton("▶️");
                btnPlay.setBackground(new Color(76, 175, 80));
                btnPlay.setForeground(Color.WHITE);
                btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnPlay.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                btnPlay.setToolTipText("Phát tin nhắn thoại");
                btnPlay.setEnabled(voiceDataMap.containsKey(voiceId));
                
                btnPlay.addActionListener(e -> playVoiceMessage(voiceId, btnPlay));
                voicePanel.add(btnPlay);
                
                len = chatDoc.getLength();
                Style style = chatPane.addStyle("VoiceComponent", null);
                StyleConstants.setComponent(style, voicePanel);
                chatDoc.insertString(len, " ", style);
                chatDoc.insertString(chatDoc.getLength(), "\n", attrs);
                
                chatPane.setCaretPosition(chatDoc.getLength());
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Play voice message from Base64 data
     */
    private void playVoiceMessage(String voiceId, JButton btnPlay) {
        String base64Data = voiceDataMap.get(voiceId);
        if (base64Data == null) {
            JOptionPane.showMessageDialog(this, 
                "Không tìm thấy dữ liệu âm thanh!", 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        new Thread(() -> {
            try {
                // Update button state
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setText("⏸️");
                    btnPlay.setBackground(new Color(231, 76, 60));
                    btnPlay.setEnabled(false);
                });
                
                // Decode Base64 to byte array
                byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                
                // Create temp input stream
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(audioBytes);
                javax.sound.sampled.AudioInputStream audioStream = 
                    javax.sound.sampled.AudioSystem.getAudioInputStream(bais);
                
                // Get audio format and line
                javax.sound.sampled.AudioFormat format = audioStream.getFormat();
                javax.sound.sampled.DataLine.Info info = 
                    new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, format);
                javax.sound.sampled.SourceDataLine line = 
                    (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
                
                line.open(format);
                line.start();
                
                // Play audio
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer, 0, buffer.length)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }
                
                // Cleanup
                line.drain();
                line.stop();
                line.close();
                audioStream.close();
                
                // Reset button
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setText("▶️");
                    btnPlay.setBackground(new Color(76, 175, 80));
                    btnPlay.setEnabled(true);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setText("▶️");
                    btnPlay.setBackground(new Color(76, 175, 80));
                    btnPlay.setEnabled(true);
                    JOptionPane.showMessageDialog(DMWindow.this, 
                        "Không thể phát âm thanh: " + e.getMessage(), 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Lưu file từ Base64
     */
    private void saveFile(String fileName, String base64Data) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file");
        chooser.setSelectedFile(new java.io.File(fileName));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] fileBytes = Base64.getDecoder().decode(base64Data);
                Files.write(chooser.getSelectedFile().toPath(), fileBytes);
                JOptionPane.showMessageDialog(this, 
                    "[OK] Da luu: " + chooser.getSelectedFile().getName(), 
                    "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "[Loi] Khong the luu: " + e.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
            }
        }
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
                String type = msg.get("type"); // Lấy loại tin nhắn
                
                boolean isMe = false;
                try {
                    isMe = Long.parseLong(senderId) == myId;
                } catch (NumberFormatException e) {
                    // ignore
                }
                
                // Xử lý theo loại tin nhắn
                if ("IMAGE".equals(type)) {
                    // Nếu là IMAGE, hiển thị text thông báo (vì không lưu Base64 trong DB)
                    appendMessage(senderName, content, isMe);
                } else if ("STICKER".equals(type)) {
                    // Nếu là STICKER, hiển thị text thông báo
                    appendMessage(senderName, content, isMe);
                } else if ("FILE".equals(type)) {
                    // Nếu là FILE, hiển thị text thông báo
                    appendMessage(senderName, content, isMe);
                } else {
                    // Text message bình thường
                    appendMessage(senderName, content, isMe);
                }
            }
        });
    }
    
    /**
     * Hiển thị folder message với nút download
     */
    public void appendFolderMessage(String sender, String folderName, int totalFiles, 
                                    long totalSize, String folderPath, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                if (isMe) {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setBackground(attrs, MY_MSG_COLOR);
                } else {
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setBackground(attrs, OTHER_MSG_COLOR);
                }
                StyleConstants.setForeground(attrs, Color.WHITE);
                StyleConstants.setFontFamily(attrs, "Segoe UI");
                StyleConstants.setFontSize(attrs, 13);
                
                // Tên người gửi
                SimpleAttributeSet nameAttrs = new SimpleAttributeSet(attrs);
                StyleConstants.setBold(nameAttrs, true);
                StyleConstants.setFontSize(nameAttrs, 11);
                
                int len = chatDoc.getLength();
                chatDoc.insertString(len, "\n", attrs);
                chatDoc.setParagraphAttributes(len, 1, attrs, false);
                
                len = chatDoc.getLength();
                chatDoc.insertString(len, sender + "\n", nameAttrs);
                chatDoc.setParagraphAttributes(len, sender.length() + 1, attrs, false);
                
                // Tạo panel chứa thông tin folder
                JPanel folderPanel = new JPanel();
                folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.Y_AXIS));
                folderPanel.setBackground(isMe ? MY_MSG_COLOR : OTHER_MSG_COLOR);
                folderPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
                
                // Icon và tên folder
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                headerPanel.setBackground(isMe ? MY_MSG_COLOR : OTHER_MSG_COLOR);
                
                JLabel iconLabel = new JLabel("📁");
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                headerPanel.add(iconLabel);
                
                JLabel nameLabel = new JLabel(folderName);
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
                headerPanel.add(nameLabel);
                
                folderPanel.add(headerPanel);
                
                // Thông tin chi tiết
                JLabel infoLabel = new JLabel(
                    String.format("%d files • %s", totalFiles, FileTransferProtocol.formatFileSize(totalSize))
                );
                infoLabel.setForeground(new Color(200, 200, 220));
                infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
                folderPanel.add(infoLabel);
                
                // Nút download (nếu không phải tin của mình và có folderPath)
                if (!isMe && folderPath != null && !folderPath.isEmpty()) {
                    JButton btnDownload = new JButton("⬇️ Tải về");
                    btnDownload.setBackground(new Color(52, 152, 219));
                    btnDownload.setForeground(Color.WHITE);
                    btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnDownload.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    btnDownload.setFocusPainted(false);
                    btnDownload.setMaximumSize(new Dimension(150, 30));
                    btnDownload.setAlignmentX(Component.LEFT_ALIGNMENT);
                    btnDownload.addActionListener(e -> downloadFolder(folderName, folderPath, totalFiles));
                    folderPanel.add(btnDownload);
                }
                
                // Chèn component vào chat
                len = chatDoc.getLength();
                Style style = chatPane.addStyle("FolderComponent", null);
                StyleConstants.setComponent(style, folderPanel);
                chatDoc.insertString(len, " ", style);
                chatDoc.insertString(chatDoc.getLength(), "\n", attrs);
                
                // Cuộn xuống cuối
                chatPane.setCaretPosition(chatDoc.getLength());
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Download folder - cho user chọn nơi lưu
     */
    private void downloadFolder(String folderName, String folderPath, int totalFiles) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn nơi lưu thư mục");
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveDir = new File(chooser.getSelectedFile(), folderName);
            
            if (saveDir.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                    "Thư mục đã tồn tại. Ghi đè?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                // Copy folder từ temp location
                File sourceFolder = new File(folderPath);
                copyFolder(sourceFolder, saveDir);
                
                JOptionPane.showMessageDialog(this,
                    "✓ Đã tải về: " + saveDir.getAbsolutePath(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải thư mục: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Utility method to copy folder
     */
    private void copyFolder(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }
            
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    copyFolder(new File(source, file), new File(dest, file));
                }
            }
        } else {
            Files.copy(source.toPath(), dest.toPath(), 
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    public long getRoomId() {
        return roomId;
    }
    
    public long getOtherId() {
        return otherId;
    }
}
