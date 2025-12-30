package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Game Vẽ Hình Đoán Chữ - Draw & Guess
 * Sử dụng UDP để stream dữ liệu vẽ realtime
 */
public class DrawGuessPanel extends JPanel {
    private ClientSocket socket;
    private MainFrame mainFrame;
    private long sessionId;
    private long myId;
    private long opponentId;
    private String opponentName;
    private boolean isDrawer; // true = người vẽ, false = người đoán
    
    // UI Components
    private DrawingCanvas canvas;
    private JLabel roleLabel;
    private JLabel wordLabel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JTextField guessField;
    private JButton guessButton;
    private JTextArea chatArea;
    private JPanel colorPanel;
    private JSlider brushSizeSlider;
    private JButton clearButton;
    private JButton eraserButton;
    
    // Game state
    private String currentWord = "";
    private int myScore = 0;
    private int opponentScore = 0;
    private int currentRound = 0;
    private int totalRounds = 6; // Mỗi người vẽ 3 lần
    private int timeLeft = 60;
    private Timer countdownTimer;
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;
    private boolean isEraser = false;
    
    // UDP for drawing
    private DatagramSocket udpSocket;
    private String targetIp;
    private int targetPort;
    private int listenPort;
    private Thread udpReceiverThread;
    private volatile boolean running = false;
    
    // Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color ACCENT_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color WARNING_COLOR = new Color(231, 76, 60); // Red
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    
    // Danh sách từ để vẽ
    private static final String[] WORD_LIST = {
        "con mèo", "con chó", "ngôi nhà", "cái cây", "mặt trời",
        "con cá", "bông hoa", "chiếc xe", "máy bay", "con bướm",
        "quả táo", "trái tim", "ngôi sao", "cầu vồng", "đám mây",
        "con gà", "con voi", "cái bàn", "cái ghế", "quyển sách",
        "điện thoại", "máy tính", "đồng hồ", "cái ô", "đôi giày",
        "cái mũ", "kính mắt", "bánh pizza", "kem", "hamburger",
        "con rắn", "con thỏ", "con khỉ", "con sư tử", "con cá voi",
        "guitar", "piano", "bóng đá", "bóng rổ", "xe đạp"
    };
    
    public DrawGuessPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Center - Canvas và Chat
        JPanel centerPanel = new JPanel(new BorderLayout(10, 0));
        centerPanel.setOpaque(false);
        
        // Canvas
        canvas = new DrawingCanvas();
        canvas.setPreferredSize(new Dimension(600, 450));
        canvas.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        centerPanel.add(canvas, BorderLayout.CENTER);
        
        // Right panel - Chat và Guess
        JPanel rightPanel = createRightPanel();
        centerPanel.add(rightPanel, BorderLayout.EAST);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom - Tools
        JPanel toolPanel = createToolPanel();
        add(toolPanel, BorderLayout.SOUTH);
        
        // Timer
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Gio: " + timeLeft + "s");
            if (timeLeft <= 10) {
                timerLabel.setForeground(WARNING_COLOR);
            }
            if (timeLeft <= 0) {
                countdownTimer.stop();
                onTimeUp();
            }
        });
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("VE HINH DOAN CHU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel infoPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        infoPanel.setOpaque(false);
        
        roleLabel = new JLabel("Vai tro: ---", SwingConstants.CENTER);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleLabel.setForeground(ACCENT_COLOR);
        
        wordLabel = new JLabel("Tu: ???", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        wordLabel.setForeground(Color.YELLOW);
        
        timerLabel = new JLabel("Gio: 60s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timerLabel.setForeground(Color.WHITE);
        
        scoreLabel = new JLabel("Diem: 0 - 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreLabel.setForeground(Color.WHITE);
        
        infoPanel.add(roleLabel);
        infoPanel.add(wordLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_BG);
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chatArea.setBackground(new Color(40, 40, 60));
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Doan tu",
            0, 0, new Font("Segoe UI", Font.PLAIN, 12), TEXT_COLOR));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        // Guess input
        JPanel guessPanel = new JPanel(new BorderLayout(5, 0));
        guessPanel.setOpaque(false);
        
        guessField = new JTextField();
        guessField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        guessField.setBackground(new Color(40, 40, 60));
        guessField.setForeground(Color.WHITE);
        guessField.setCaretColor(Color.WHITE);
        guessField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        guessField.setEnabled(false);
        guessField.addActionListener(e -> submitGuess());
        
        guessButton = new JButton("Đoán");
        styleButton(guessButton, ACCENT_COLOR);
        guessButton.setForeground(Color.BLACK);
        guessButton.setEnabled(false);
        guessButton.addActionListener(e -> submitGuess());
        
        guessPanel.add(guessField, BorderLayout.CENTER);
        guessPanel.add(guessButton, BorderLayout.EAST);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(guessPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        // Left side - Drawing tools
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolsPanel.setOpaque(false);
        
        // Colors
        colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        colorPanel.setOpaque(false);
        
        Color[] colors = {
            Color.BLACK, Color.WHITE, Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK,
            new Color(139, 69, 19), Color.GRAY
        };
        
        for (Color c : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setPreferredSize(new Dimension(25, 25));
            colorBtn.setBackground(c);
            colorBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
            colorBtn.addActionListener(e -> {
                currentColor = c;
                isEraser = false;
                canvas.setCursor(Cursor.getDefaultCursor());
            });
            colorPanel.add(colorBtn);
        }
        
        // Brush size
        JLabel sizeLabel = new JLabel("Cọ:");
        sizeLabel.setForeground(Color.WHITE);
        
        brushSizeSlider = new JSlider(1, 20, 5);
        brushSizeSlider.setPreferredSize(new Dimension(100, 25));
        brushSizeSlider.setOpaque(false);
        brushSizeSlider.addChangeListener(e -> brushSize = brushSizeSlider.getValue());
        
        // Eraser
        eraserButton = new JButton("Tay");
        eraserButton.setBackground(Color.WHITE);
        eraserButton.setFocusPainted(false);
        eraserButton.addActionListener(e -> {
            isEraser = true;
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        });
        
        // Clear
        clearButton = new JButton("Xoa het");
        styleButton(clearButton, WARNING_COLOR);
        clearButton.addActionListener(e -> {
            if (isDrawer) {
                canvas.clear();
                sendClearCommand();
            }
        });
        
        toolsPanel.add(colorPanel);
        toolsPanel.add(sizeLabel);
        toolsPanel.add(brushSizeSlider);
        toolsPanel.add(eraserButton);
        toolsPanel.add(clearButton);
        
        // Right side - Quit button (always visible)
        JPanel quitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        quitPanel.setOpaque(false);
        
        JButton quitButton = new JButton("THOAT GAME");
        styleButton(quitButton, new Color(192, 57, 43));
        quitButton.setPreferredSize(new Dimension(150, 35));
        quitButton.addActionListener(e -> quitGame());
        
        quitPanel.add(quitButton);
        
        panel.add(toolsPanel, BorderLayout.CENTER);
        panel.add(quitPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    public void setSocket(ClientSocket socket) {
        this.socket = socket;
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Bắt đầu game
     */
    public void startGame(long sessionId, long myId, long opponentId, String opponentName,
                          boolean isFirst, String targetIp, int targetPort, int listenPort) {
        this.sessionId = sessionId;
        this.myId = myId;
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        this.isDrawer = isFirst; // Người đi trước là người vẽ
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.listenPort = listenPort;
        
        this.currentRound = 1;
        this.myScore = 0;
        this.opponentScore = 0;
        
        canvas.clear();
        chatArea.setText("");
        
        // Start UDP
        startUdp();
        
        // Setup round
        setupRound();
    }
    
    private void startUdp() {
        try {
            udpSocket = new DatagramSocket(listenPort);
            running = true;
            
            udpReceiverThread = new Thread(this::receiveUdpData);
            udpReceiverThread.setDaemon(true);
            udpReceiverThread.start();
            
            System.out.println("DrawGuess UDP started on port " + listenPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopUdp() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
    
    private void setupRound() {
        timeLeft = 60;
        timerLabel.setText("Gio: 60s");
        timerLabel.setForeground(Color.WHITE);
        canvas.clear();
        
        if (isDrawer) {
            // Chọn từ ngẫu nhiên
            currentWord = WORD_LIST[(int)(Math.random() * WORD_LIST.length)];
            roleLabel.setText("[VE] Ban ve!");
            roleLabel.setForeground(ACCENT_COLOR);
            wordLabel.setText("Từ: " + currentWord.toUpperCase());
            
            // Enable drawing tools
            setDrawingEnabled(true);
            guessField.setEnabled(false);
            guessButton.setEnabled(false);
            
            appendChat("[Hệ thống] Vẽ từ: " + currentWord);
            
            // Gửi từ cần vẽ và hint cho server
            sendWordAndHint(currentWord);
        } else {
            currentWord = "";
            roleLabel.setText("[DOAN] Ban doan!");
            roleLabel.setForeground(Color.YELLOW);
            wordLabel.setText("Từ: _ _ _ _ _");
            
            // Disable drawing
            setDrawingEnabled(false);
            guessField.setEnabled(true);
            guessButton.setEnabled(true);
            guessField.requestFocus();
            
            appendChat("[Hệ thống] Đoán từ mà đối phương đang vẽ!");
        }
        
        scoreLabel.setText("Điểm: " + myScore + " - " + opponentScore);
        countdownTimer.start();
    }
    
    private void setDrawingEnabled(boolean enabled) {
        canvas.setDrawingEnabled(enabled);
        colorPanel.setVisible(enabled);
        brushSizeSlider.setEnabled(enabled);
        eraserButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }
    
    /**
     * Gửi dữ liệu vẽ qua UDP
     */
    private void sendDrawData(int x1, int y1, int x2, int y2, Color color, int size, boolean eraser) {
        if (!isDrawer || udpSocket == null) return;
        
        try {
            // Format: DRAW|x1|y1|x2|y2|r|g|b|size|eraser
            String data = String.format("DRAW|%d|%d|%d|%d|%d|%d|%d|%d|%b",
                x1, y1, x2, y2, color.getRed(), color.getGreen(), color.getBlue(), size, eraser);
            
            byte[] bytes = data.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                InetAddress.getByName(targetIp), targetPort);
            udpSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendClearCommand() {
        if (!isDrawer || udpSocket == null) return;
        
        try {
            String data = "CLEAR";
            byte[] bytes = data.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                InetAddress.getByName(targetIp), targetPort);
            udpSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendHint(int length) {
        // Deprecated - use sendWordAndHint instead
    }
    
    private void sendWordAndHint(String word) {
        // Gửi từ và hint qua TCP
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("word", word.toLowerCase());
        data.put("hint", String.valueOf(word.length()));
        socket.send("DRAW_START_ROUND", data);
    }
    
    /**
     * Nhận dữ liệu UDP
     */
    private void receiveUdpData() {
        byte[] buffer = new byte[1024];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                
                String data = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                processUdpData(data);
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }
    }
    
    private void processUdpData(String data) {
        SwingUtilities.invokeLater(() -> {
            if (data.startsWith("DRAW|")) {
                String[] parts = data.split("\\|");
                if (parts.length >= 10) {
                    int x1 = Integer.parseInt(parts[1]);
                    int y1 = Integer.parseInt(parts[2]);
                    int x2 = Integer.parseInt(parts[3]);
                    int y2 = Integer.parseInt(parts[4]);
                    int r = Integer.parseInt(parts[5]);
                    int g = Integer.parseInt(parts[6]);
                    int b = Integer.parseInt(parts[7]);
                    int size = Integer.parseInt(parts[8]);
                    boolean eraser = Boolean.parseBoolean(parts[9]);
                    
                    Color color = eraser ? Color.WHITE : new Color(r, g, b);
                    canvas.drawLineExternal(x1, y1, x2, y2, color, size);
                }
            } else if (data.equals("CLEAR")) {
                canvas.clear();
            }
        });
    }
    
    /**
     * Nhận hint từ người vẽ
     */
    public void onHintReceived(int length) {
        if (!isDrawer) {
            StringBuilder hint = new StringBuilder();
            for (int i = 0; i < length; i++) {
                hint.append("_ ");
            }
            wordLabel.setText("Từ: " + hint.toString().trim() + " (" + length + " chữ)");
        }
    }
    
    /**
     * Submit guess
     */
    private void submitGuess() {
        if (isDrawer) return;
        
        String guess = guessField.getText().trim().toLowerCase();
        if (guess.isEmpty()) return;
        
        guessField.setText("");
        appendChat("Bạn: " + guess);
        
        // Gửi đáp án qua TCP
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("guess", guess);
        socket.send("DRAW_GUESS", data);
    }
    
    /**
     * Nhận kết quả đoán
     */
    public void onGuessResult(String guess, boolean correct, boolean isMe) {
        String sender = isMe ? "Bạn" : opponentName;
        
        if (correct) {
            appendChat("[DUNG] " + sender + " doan dung: " + guess);
            countdownTimer.stop();
            
            if (isMe) {
                // Người đoán đúng được điểm
                myScore += 10;
                opponentScore += 5; // Người vẽ cũng được điểm
            } else {
                opponentScore += 10;
                myScore += 5;
            }
            scoreLabel.setText("Điểm: " + myScore + " - " + opponentScore);
            
            // Chuyển round sau 2 giây
            Timer nextRound = new Timer(2000, e -> nextRound());
            nextRound.setRepeats(false);
            nextRound.start();
        } else {
            appendChat("[SAI] " + sender + ": " + guess);
        }
    }
    
    /**
     * Nhận tin người khác đoán (cho người vẽ thấy)
     */
    public void onOpponentGuess(String guess) {
        appendChat(opponentName + ": " + guess);
    }
    
    private void onTimeUp() {
        if (isDrawer) {
            appendChat("[Hệ thống] Hết giờ! Từ cần đoán là: " + currentWord);
        } else {
            appendChat("[Hệ thống] Hết giờ!");
        }
        
        Timer nextRound = new Timer(2000, e -> nextRound());
        nextRound.setRepeats(false);
        nextRound.start();
    }
    
    private void nextRound() {
        currentRound++;
        
        if (currentRound > totalRounds) {
            endGame();
        } else {
            // Đổi vai
            isDrawer = !isDrawer;
            setupRound();
        }
    }
    
    private void endGame() {
        countdownTimer.stop();
        stopUdp();
        
        String result;
        if (myScore > opponentScore) {
            result = "CHIEN THANG!";
        } else if (myScore < opponentScore) {
            result = "THUA CUOC!";
        } else {
            result = "HOA!";
        }
        
        JOptionPane.showMessageDialog(this,
            result + "\n\nĐiểm số: " + myScore + " - " + opponentScore,
            "Kết thúc game", JOptionPane.INFORMATION_MESSAGE);
        
        // Gửi kết quả về server
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("myScore", String.valueOf(myScore));
        socket.send("DRAW_GAME_END", data);
        
        if (mainFrame != null) {
            mainFrame.showCard("LOBBY");
        }
    }
    
    private void quitGame() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn thoát?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            countdownTimer.stop();
            stopUdp();
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            socket.send("DRAW_QUIT", data);
            
            if (mainFrame != null) {
                mainFrame.showCard("LOBBY");
            }
        }
    }
    
    private void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * Canvas để vẽ
     */
    private class DrawingCanvas extends JPanel {
        private BufferedImage image;
        private Graphics2D g2d;
        private int prevX, prevY;
        private boolean drawingEnabled = false;
        
        public DrawingCanvas() {
            setBackground(Color.WHITE);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!drawingEnabled) return;
                    prevX = e.getX();
                    prevY = e.getY();
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!drawingEnabled) return;
                    
                    int x = e.getX();
                    int y = e.getY();
                    
                    Color drawColor = isEraser ? Color.WHITE : currentColor;
                    
                    if (g2d != null) {
                        g2d.setColor(drawColor);
                        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2d.drawLine(prevX, prevY, x, y);
                        repaint();
                    }
                    
                    // Gửi qua UDP
                    sendDrawData(prevX, prevY, x, y, currentColor, brushSize, isEraser);
                    
                    prevX = x;
                    prevY = y;
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (image == null) {
                image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                g2d = image.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                clear();
            }
            
            g.drawImage(image, 0, 0, null);
        }
        
        public void clear() {
            if (g2d != null) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                repaint();
            }
        }
        
        public void drawLineExternal(int x1, int y1, int x2, int y2, Color color, int size) {
            if (g2d != null) {
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x1, y1, x2, y2);
                repaint();
            }
        }
        
        public void setDrawingEnabled(boolean enabled) {
            this.drawingEnabled = enabled;
            setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
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
