package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class CatchHeartPanel extends JPanel {
    private int basketX = 300;
    private int basketY = 520;
    private int basketWidth = 80;
    private int basketHeight = 30;
    
    private List<Heart> hearts = new ArrayList<>();
    private Random random = new Random();
    
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    
    private Timer gameTimer;
    private Timer spawnTimer;
    
    private ClientSocket socket;
    private long sessionId;
    private long myId;
    private long opponentId;
    private MainFrame mainFrame;
    
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JLabel levelLabel;
    private JButton startButton;
    private JButton pauseButton;
    
    // Modern Theme Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color BASKET_COLOR = new Color(255, 215, 0); // Gold
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color BTN_COLOR = new Color(46, 204, 113); // Green for Start
    private static final Color BTN_WARN = new Color(231, 76, 60); // Red for Exit

    public CatchHeartPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setFocusable(true);
        setPreferredSize(new Dimension(600, 600));
        
        // Top panel - Thông tin
        JPanel topPanel = new JPanel();
        topPanel.setBackground(PANEL_BG);
        topPanel.setPreferredSize(new Dimension(600, 50));
        
        scoreLabel = new JLabel("Điểm: 0");
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        scoreLabel.setForeground(TEXT_COLOR);
        
        livesLabel = new JLabel("<3 <3 <3");
        livesLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        livesLabel.setForeground(new Color(231, 76, 60)); // Red
        
        levelLabel = new JLabel("Cấp độ: 1");
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        levelLabel.setForeground(new Color(241, 196, 15));
        
        topPanel.add(scoreLabel);
        topPanel.add(new JLabel("    "));
        topPanel.add(livesLabel);
        topPanel.add(new JLabel("    "));
        topPanel.add(levelLabel);
        add(topPanel, BorderLayout.NORTH);
        
        // Game panel
        JPanel gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        
        // Bottom panel - Nút điều khiển
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(PANEL_BG);
        
        startButton = new JButton("Bắt đầu");
        styleButton(startButton, BTN_COLOR);
        startButton.addActionListener(e -> startGame());
        
        pauseButton = new JButton("Tạm dừng");
        styleButton(pauseButton, new Color(241, 196, 15));
        pauseButton.setForeground(Color.BLACK); // Yellow btn needs black text
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(e -> togglePause());
        
        JButton exitButton = new JButton("Thoát");
        styleButton(exitButton, BTN_WARN);
        exitButton.addActionListener(e -> exitGame());
        
        bottomPanel.add(startButton);
        bottomPanel.add(pauseButton);
        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Keyboard control
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameRunning || gamePaused) return;
                
                int speed = 20;
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    basketX = Math.max(0, basketX - speed);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    basketX = Math.min(520, basketX + speed);
                }
                repaint();
            }
        });
    }
    
    public void setMainFrame(MainFrame frame) {
        this.mainFrame = frame;
    }
    
    public void setSocket(ClientSocket socket) {
        this.socket = socket;
    }
    
    public void startGame() {
        score = 0;
        lives = 3;
        level = 1;
        hearts.clear();
        basketX = 260;
        gameRunning = true;
        gamePaused = false;
        
        updateGameUI();
        
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        
        // Game loop - 60 FPS
        gameTimer = new Timer(16, e -> {
            if (!gamePaused) {
                updateGame();
                repaint();
            }
        });
        gameTimer.start();
        
        // Spawn hearts
        spawnTimer = new Timer(1000, e -> {
            if (!gamePaused && gameRunning) {
                spawnHeart();
            }
        });
        spawnTimer.start();
        
        requestFocusInWindow();
    }
    
    private void spawnHeart() {
        int x = random.nextInt(560);
        int type = random.nextInt(10);
        
        Heart heart;
        if (type < 6) {
            // Trái tim đỏ thường - 60%
            heart = new Heart(x, 0, HeartType.NORMAL);
        } else if (type < 8) {
            // Trái tim vàng bonus - 20%
            heart = new Heart(x, 0, HeartType.GOLD);
        } else if (type < 9) {
            // Trái tim đen (mất mạng) - 10%
            heart = new Heart(x, 0, HeartType.BLACK);
        } else {
            // Trái tim xanh (thêm mạng) - 10%
            heart = new Heart(x, 0, HeartType.BLUE);
        }
        
        hearts.add(heart);
    }
    
    private void updateGame() {
        Iterator<Heart> it = hearts.iterator();
        
        while (it.hasNext()) {
            Heart heart = it.next();
            heart.y += heart.speed + (level - 1);
            
            // Kiểm tra bắt được
            if (heart.y + 20 >= basketY && heart.y <= basketY + basketHeight) {
                if (heart.x + 20 >= basketX && heart.x <= basketX + basketWidth) {
                    // Bắt được!
                    handleCatch(heart);
                    it.remove();
                    continue;
                }
            }
            
            // Trái tim rơi xuống đất
            if (heart.y > 560) {
                if (heart.type == HeartType.NORMAL || heart.type == HeartType.GOLD) {
                    lives--;
                }
                it.remove();
            }
        }
        
        // Level up mỗi 100 điểm
        int newLevel = (score / 100) + 1;
        if (newLevel > level) {
            level = newLevel;
        }
        
        updateGameUI();
        
        // Game over
        if (lives <= 0) {
            endGame();
        }
    }
    
    private void handleCatch(Heart heart) {
        switch (heart.type) {
            case NORMAL:
                score += 10;
                break;
            case GOLD:
                score += 50;
                break;
            case BLACK:
                lives--;
                break;
            case BLUE:
                lives = Math.min(lives + 1, 5);
                break;
        }
    }
    
    private void updateGameUI() {
        SwingUtilities.invokeLater(() -> {
            scoreLabel.setText("Điểm: " + score);
            levelLabel.setText("Cấp độ: " + level);
            
            StringBuilder heartsStr = new StringBuilder();
            for (int i = 0; i < lives; i++) {
                heartsStr.append("<3 ");
            }
            livesLabel.setText(heartsStr.toString().trim());
        });
    }
    
    private void togglePause() {
        gamePaused = !gamePaused;
        pauseButton.setText(gamePaused ? "Tiếp tục" : "Tạm dừng");
        if (!gamePaused) {
            requestFocusInWindow();
        }
    }
    
    private void endGame() {
        gameRunning = false;
        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        
        // Gửi điểm lên server nếu có kết nối
        if (socket != null) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("gameCode", "CATCH_HEART");
            data.put("score", String.valueOf(score));
            socket.send("GAME_SCORE", data);
        }
        
        // Hỏi chơi lại
        int choice = JOptionPane.showConfirmDialog(this, 
            "Game Over!\n\n🏆 Điểm của bạn: " + score + "\n⭐ Cấp độ đạt được: " + level + 
            "\n\nBạn có muốn chơi lại không?",
            "Kết thúc game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Chơi lại ngay
            startGame();
        } else {
            // Quay về lobby
            if (mainFrame != null) {
                mainFrame.showCard("LOBBY");
            }
        }
    }
    
    private void exitGame() {
        if (gameRunning) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn thoát? Điểm sẽ không được lưu!",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        
        if (mainFrame != null) {
            mainFrame.showCard("LOBBY");
        }
    }
    
    // Inner class cho Game Panel
    private class GamePanel extends JPanel {
        public GamePanel() {
            setBackground(BG_COLOR);
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (gameRunning && !gamePaused) {
                        basketX = Math.max(0, Math.min(520, e.getX() - basketWidth / 2));
                        repaint();
                    }
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ các trái tim
            for (Heart heart : hearts) {
                drawHeart(g2d, heart);
            }
            
            // Vẽ rổ
            g2d.setColor(BASKET_COLOR);
            g2d.fillRoundRect(basketX, basketY, basketWidth, basketHeight, 10, 10);
            g2d.setColor(new Color(218, 165, 32)); // Dark Gold
            g2d.drawRoundRect(basketX, basketY, basketWidth, basketHeight, 10, 10);
            
            // Hiển thị hướng dẫn khi chưa chơi
            if (!gameRunning) {
                g2d.setColor(TEXT_COLOR);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                String msg = "Nhấn 'Bắt đầu' để chơi!";
                int msgWidth = g2d.getFontMetrics().stringWidth(msg);
                g2d.drawString(msg, (getWidth() - msgWidth) / 2, 250);
                
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2d.drawString("Di chuyển chuột hoặc phím ← → để bắt trái tim", 120, 290);
                g2d.drawString("Do: +10 diem  |  Vang: +50 diem", 150, 320);
                g2d.drawString("🖤 Đen: -1 mạng  |  💙 Xanh: +1 mạng", 150, 350);
            }
            
            // Hiển thị tạm dừng
            if (gamePaused) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(TEXT_COLOR);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 32));
                String pause = "TẠM DỪNG";
                int pauseWidth = g2d.getFontMetrics().stringWidth(pause);
                g2d.drawString(pause, (getWidth() - pauseWidth) / 2, getHeight() / 2);
            }
        }
        
        private void drawHeart(Graphics2D g2d, Heart heart) {
            Color color;
            switch (heart.type) {
                case GOLD:
                    color = new Color(255, 215, 0); // Vàng
                    break;
                case BLACK:
                    color = Color.DARK_GRAY; // Đen
                    break;
                case BLUE:
                    color = new Color(52, 152, 219); // Xanh
                    break;
                default:
                    color = new Color(231, 76, 60); // Đỏ
            }
            
            g2d.setColor(color);
            // Vẽ hình trái tim đơn giản
            int x = heart.x;
            int y = heart.y;
            int size = 20;
            
            g2d.fillOval(x, y, size/2 + 2, size/2 + 2);
            g2d.fillOval(x + size/2 - 2, y, size/2 + 2, size/2 + 2);
            int[] xPoints = {x, x + size/2, x + size};
            int[] yPoints = {y + size/4, y + size, y + size/4};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
    }
    
    // Inner class Heart
    private class Heart {
        int x, y;
        int speed;
        HeartType type;
        
        Heart(int x, int y, HeartType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.speed = 3 + random.nextInt(3);
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
    
    private enum HeartType {
        NORMAL, GOLD, BLACK, BLUE
    }
}
