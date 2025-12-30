package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.friendzone.client.controller.ClientSocket;

/**
 * Game Nối Từ - 2 người chơi thay phiên nhập từ
 * Từ tiếp theo phải bắt đầu bằng chữ cái cuối của từ trước
 */
public class WordChainPanel extends JPanel {
    private ClientSocket socket;
    private MainFrame mainFrame;
    private long sessionId;
    private long myId;
    private long opponentId;
    private boolean isMyTurn;
    
    private JLabel statusLabel;
    private JLabel currentWordLabel;
    private JLabel timerLabel;
    private JTextField inputField;
    private JButton submitButton;
    private JTextArea historyArea;
    private JLabel myScoreLabel;
    private JLabel opponentScoreLabel;
    
    private String lastWord = "";
    private Set<String> usedWords = new HashSet<>();
    private int myScore = 0;
    private int opponentScore = 0;
    private Timer countdownTimer;
    private int timeLeft = 15; // 15 giây mỗi lượt
    
    // Vietnamese dictionary for word validation
    private static Set<String> vietnameseDictionary = new HashSet<>();
    private static boolean dictionaryLoaded = false;
    
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color ACCENT_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color WARNING_COLOR = new Color(231, 76, 60); // Red
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    
    public WordChainPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Load dictionary if not already loaded
        loadDictionary();
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🔤 NỐI TỪ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Score panel
        JPanel scorePanel = new JPanel(new GridLayout(1, 3, 10, 0));
        scorePanel.setOpaque(false);
        
        myScoreLabel = new JLabel("Bạn: 0", SwingConstants.CENTER);
        myScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        myScoreLabel.setForeground(ACCENT_COLOR);
        
        timerLabel = new JLabel("Gio: 15", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        timerLabel.setForeground(Color.YELLOW);
        
        opponentScoreLabel = new JLabel("Đối thủ: 0", SwingConstants.CENTER);
        opponentScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        opponentScoreLabel.setForeground(WARNING_COLOR);
        
        scorePanel.add(myScoreLabel);
        scorePanel.add(timerLabel);
        scorePanel.add(opponentScoreLabel);
        headerPanel.add(scorePanel, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Center - Current word and input
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        
        // Current word display
        JPanel wordPanel = new JPanel(new BorderLayout());
        wordPanel.setBackground(PANEL_BG);
        wordPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        statusLabel = new JLabel("Đang chờ bắt đầu...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        
        currentWordLabel = new JLabel("---", SwingConstants.CENTER);
        currentWordLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        currentWordLabel.setForeground(TEXT_COLOR);
        
        wordPanel.add(statusLabel, BorderLayout.NORTH);
        wordPanel.add(currentWordLabel, BorderLayout.CENTER);
        
        centerPanel.add(wordPanel, BorderLayout.NORTH);
        
        // History
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        historyArea.setBackground(new Color(40, 40, 60));
        historyArea.setForeground(TEXT_COLOR);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Lịch sử từ",
            0, 0, new Font("Segoe UI", Font.PLAIN, 12), TEXT_COLOR));
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom - Input
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        inputField.setBackground(new Color(40, 40, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        inputField.setEnabled(false);
        inputField.addActionListener(e -> submitWord());
        
        submitButton = new JButton("Gửi");
        styleButton(submitButton, ACCENT_COLOR);
        submitButton.setForeground(Color.BLACK); // Neon needs black text
        submitButton.setEnabled(false);
        submitButton.addActionListener(e -> submitWord());
        
        JButton quitButton = new JButton("Thoát");
        styleButton(quitButton, WARNING_COLOR);
        quitButton.addActionListener(e -> quitGame());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(submitButton);
        buttonPanel.add(quitButton);
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(inputPanel, BorderLayout.SOUTH);
        
        // Timer
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Gio: " + timeLeft);
            if (timeLeft <= 5) {
                timerLabel.setForeground(WARNING_COLOR);
            }
            if (timeLeft <= 0) {
                countdownTimer.stop();
                if (isMyTurn) {
                    // Hết giờ - thua lượt này
                    sendTimeout();
                }
            }
        });
    }
    
    public void setSocket(ClientSocket socket) {
        this.socket = socket;
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Load Vietnamese dictionary from file (runs once)
     */
    private static void loadDictionary() {
        if (dictionaryLoaded) return;
        
        try {
            java.io.File dictFile = new java.io.File("Viet74K_2words.txt");
            if (!dictFile.exists()) {
                System.err.println("Warning: Dictionary file Viet74K_2words.txt not found!");
                dictionaryLoaded = true;
                return;
            }
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(dictFile, java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty()) {
                    vietnameseDictionary.add(line);
                }
            }
            reader.close();
            
            System.out.println("Loaded " + vietnameseDictionary.size() + " Vietnamese words");
            dictionaryLoaded = true;
        } catch (Exception e) {
            System.err.println("Error loading dictionary: " + e.getMessage());
            dictionaryLoaded = true;
        }
    }
    
    /**
     * Check if a word exists in Vietnamese dictionary
     */
    private boolean isValidVietnameseWord(String word) {
        if (!dictionaryLoaded || vietnameseDictionary.isEmpty()) {
            // If dictionary not loaded, accept all words (fallback)
            return true;
        }
        return vietnameseDictionary.contains(word.toLowerCase());
    }
    
    public void startGame(long sessionId, long myId, long opponentId, boolean isFirst) {
        this.sessionId = sessionId;
        this.myId = myId;
        this.opponentId = opponentId;
        this.isMyTurn = isFirst;
        this.lastWord = "";
        this.usedWords.clear();
        this.myScore = 0;
        this.opponentScore = 0;
        
        historyArea.setText("");
        myScoreLabel.setText("Bạn: 0");
        opponentScoreLabel.setText("Đối thủ: 0");
        
        if (isFirst) {
            // Người đi trước nhập từ đầu tiên (bất kỳ - nhưng phải đúng 2 từ có nghĩa)
            setMyTurn(true, "Bạn đi trước! Nhập đúng 2 từ có nghĩa:");
            currentWordLabel.setText("Bắt đầu!");
        } else {
            setMyTurn(false, "Đối thủ đang nhập 2 từ đầu tiên...");
            currentWordLabel.setText("Chờ đối thủ...");
        }
    }
    
    private void setMyTurn(boolean myTurn, String status) {
        this.isMyTurn = myTurn;
        statusLabel.setText(status);
        inputField.setEnabled(myTurn);
        submitButton.setEnabled(myTurn);
        
        if (myTurn) {
            inputField.requestFocus();
            startTimer();
        } else {
            countdownTimer.stop();
            timerLabel.setText("Gio: --");
            timerLabel.setForeground(Color.YELLOW);
        }
    }
    
    private void startTimer() {
        timeLeft = 15;
        timerLabel.setText("Gio: 15");
        timerLabel.setForeground(Color.YELLOW);
        countdownTimer.start();
    }
    
    private void submitWord() {
        if (!isMyTurn) return;
        
        String word = inputField.getText().trim().toLowerCase();
        if (word.isEmpty()) return;
        
        // Validate
        if (usedWords.contains(word)) {
            JOptionPane.showMessageDialog(this, "Từ này đã được sử dụng!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Kiểm tra từ có nghĩa (bắt buộc đúng 2 từ)
        String[] words = word.split("\\s+");
        if (words.length != 2) {
            JOptionPane.showMessageDialog(this, 
                "Phải nhập đúng 2 từ có nghĩa!\nVí dụ: 'táo bón', 'bón phân', 'phân biệt'", 
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Kiểm tra cụm từ có trong từ điển tiếng Việt (đã lọc sẵn cụm 2 từ)
        if (!isValidVietnameseWord(word)) {
            JOptionPane.showMessageDialog(this, 
                "Cụm từ '" + word + "' không có trong từ điển!\nVui lòng nhập cụm từ có nghĩa.\nVí dụ: 'táo bón', 'bón phân', 'phân biệt'", 
                "Cụm từ không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!lastWord.isEmpty()) {
            // Lấy từ cuối cùng của câu trước
            String[] lastWords = lastWord.split("\\s+");
            String lastWordEnd = lastWords[lastWords.length - 1];
            
            // Từ mới phải bắt đầu bằng từ cuối của câu trước
            String firstWordNew = words[0];
            if (!firstWordNew.equals(lastWordEnd)) {
                JOptionPane.showMessageDialog(this, 
                    "Từ phải bắt đầu bằng '" + lastWordEnd + "'!\nVí dụ: '" + lastWordEnd + " ...'", 
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        // Valid word - send to server
        countdownTimer.stop();
        
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("word", word);
        socket.send("WORD_CHAIN_MOVE", data);
        
        inputField.setText("");
    }
    
    private void sendTimeout() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("timeout", "true");
        socket.send("WORD_CHAIN_TIMEOUT", data);
    }
    
    public void onWordReceived(String word, long senderId, boolean valid) {
        boolean isMe = (senderId == myId);
        String sender = isMe ? "Bạn" : "Đối thủ";
        
        if (valid) {
            usedWords.add(word.toLowerCase());
            lastWord = word.toLowerCase();
            
            historyArea.append(sender + ": " + word + "\n");
            historyArea.setCaretPosition(historyArea.getDocument().getLength());
            
            currentWordLabel.setText(word.toUpperCase());
            
            if (isMe) {
                myScore++;
                myScoreLabel.setText("Bạn: " + myScore);
                setMyTurn(false, "Đối thủ đang nghĩ...");
            } else {
                opponentScore++;
                opponentScoreLabel.setText("Đối thủ: " + opponentScore);
                // Lấy từ cuối cùng của câu
                String[] lastWords = word.split("\\s+");
                String lastWordEnd = lastWords[lastWords.length - 1];
                setMyTurn(true, "Lượt bạn! Nhập 2 từ bắt đầu bằng '" + lastWordEnd + "':");
            }
        }
    }
    
    public void onGameEnd(long winnerId, String reason) {
        countdownTimer.stop();
        inputField.setEnabled(false);
        submitButton.setEnabled(false);
        
        String message;
        if (winnerId == myId) {
            message = "CHIEN THANG!\n" + reason + "\nDiem: " + myScore + " - " + opponentScore;
        } else if (winnerId == opponentId) {
            message = "THUA CUOC!\n" + reason + "\nDiem: " + myScore + " - " + opponentScore;
        } else {
            message = "HOA!\n" + reason;
        }
        
        JOptionPane.showMessageDialog(this, message, "Kết thúc", JOptionPane.INFORMATION_MESSAGE);
        
        if (mainFrame != null) {
            mainFrame.showCard("LOBBY");
        }
    }
    
    private void quitGame() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc muốn thoát? Bạn sẽ thua!", 
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            countdownTimer.stop();
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            socket.send("WORD_CHAIN_QUIT", data);
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
