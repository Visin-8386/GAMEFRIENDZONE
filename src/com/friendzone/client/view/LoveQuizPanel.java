package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.friendzone.client.controller.ClientSocket;

/**
 * Game Quiz Tình Yêu - 2 người trả lời câu hỏi
 * Điểm khi cả 2 chọn giống nhau
 */
public class LoveQuizPanel extends JPanel {
    private ClientSocket socket;
    private MainFrame mainFrame;
    private long sessionId;
    private long myId;
    private long opponentId;
    
    private JLabel questionNumberLabel;
    private JLabel questionLabel;
    private JButton[] answerButtons = new JButton[4];
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JProgressBar progressBar;
    
    private int currentQuestion = 0;
    private int totalQuestions = 10;
    private int score = 0;
    private int myAnswer = -1;
    private boolean hasAnswered = false;
    private Timer countdownTimer;
    private int timeLeft = 20;
    
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color ACCENT_COLOR = new Color(155, 89, 182); // Purple
    private static final Color CORRECT_COLOR = new Color(46, 204, 113); // Green
    private static final Color WRONG_COLOR = new Color(231, 76, 60); // Red
    private static final Color SELECTED_COLOR = new Color(52, 152, 219); // Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    
    public LoveQuizPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("QUIZ TINH YEU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(255, 182, 193)); // Pink
        
        // Progress and score
        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        infoPanel.setOpaque(false);
        
        questionNumberLabel = new JLabel("Câu 1/10", SwingConstants.LEFT);
        questionNumberLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        questionNumberLabel.setForeground(TEXT_COLOR);
        
        timerLabel = new JLabel("Gio: 20", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        timerLabel.setForeground(Color.YELLOW);
        
        scoreLabel = new JLabel("Diem: 0", SwingConstants.RIGHT);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreLabel.setForeground(CORRECT_COLOR);
        
        infoPanel.add(questionNumberLabel);
        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);
        
        progressBar = new JProgressBar(0, totalQuestions);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(new Color(40, 40, 60));
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.CENTER);
        headerPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Center - Question and answers
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setOpaque(false);
        
        // Question
        JPanel questionPanel = new JPanel(new BorderLayout());
        questionPanel.setBackground(PANEL_BG);
        questionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 3),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        
        questionLabel = new JLabel("<html><center>Đang chờ câu hỏi...</center></html>", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        questionLabel.setForeground(TEXT_COLOR);
        questionPanel.add(questionLabel, BorderLayout.CENTER);
        
        centerPanel.add(questionPanel, BorderLayout.NORTH);
        
        // Answers - 2x2 grid
        JPanel answersPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        answersPanel.setOpaque(false);
        
        String[] labels = {"A", "B", "C", "D"};
        Color[] colors = {
            new Color(231, 76, 60),   // Đỏ
            new Color(52, 152, 219),  // Xanh dương
            new Color(46, 204, 113),  // Xanh lá
            new Color(241, 196, 15)   // Vàng
        };
        
        for (int i = 0; i < 4; i++) {
            final int index = i;
            answerButtons[i] = new JButton(labels[i] + ". ---");
            answerButtons[i].setFont(new Font("Segoe UI", Font.BOLD, 16));
            answerButtons[i].setBackground(colors[i]);
            answerButtons[i].setForeground(Color.WHITE);
            answerButtons[i].setFocusPainted(false);
            answerButtons[i].setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
            answerButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            answerButtons[i].setEnabled(false);
            answerButtons[i].addActionListener(e -> selectAnswer(index));
            answersPanel.add(answerButtons[i]);
        }
        
        centerPanel.add(answersPanel, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom - Status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        statusLabel = new JLabel("Đang chờ đối thủ...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        
        JButton quitButton = new JButton("Thoát");
        styleButton(quitButton, WRONG_COLOR);
        quitButton.addActionListener(e -> quitGame());
        
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(quitButton, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Timer
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Gio: " + timeLeft);
            if (timeLeft <= 5) {
                timerLabel.setForeground(WRONG_COLOR);
            }
            if (timeLeft <= 0) {
                countdownTimer.stop();
                if (!hasAnswered) {
                    // Auto submit -1 (không chọn)
                    submitAnswer(-1);
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
    
    public void startGame(long sessionId, long myId, long opponentId) {
        this.sessionId = sessionId;
        this.myId = myId;
        this.opponentId = opponentId;
        this.currentQuestion = 0;
        this.score = 0;
        this.myAnswer = -1;
        this.hasAnswered = false;
        
        scoreLabel.setText("Diem: 0");
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Đang tải câu hỏi...");
        
        // Request first question
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        socket.send("QUIZ_READY", data);
    }
    
    public void showQuestion(int questionNum, String question, String[] answers) {
        this.currentQuestion = questionNum;
        this.myAnswer = -1;
        this.hasAnswered = false;
        
        questionNumberLabel.setText("Câu " + questionNum + "/" + totalQuestions);
        questionLabel.setText("<html><center>" + question + "</center></html>");
        
        String[] labels = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(labels[i] + ". " + answers[i]);
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        }
        
        statusLabel.setText("Chọn đáp án của bạn!");
        startTimer();
    }
    
    private void startTimer() {
        timeLeft = 20;
        timerLabel.setText("Gio: 20");
        timerLabel.setForeground(Color.YELLOW);
        countdownTimer.start();
    }
    
    private void selectAnswer(int index) {
        if (hasAnswered) return;
        
        myAnswer = index;
        hasAnswered = true;
        countdownTimer.stop();
        
        // Highlight selected
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setEnabled(false);
            if (i == index) {
                answerButtons[i].setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
            }
        }
        
        statusLabel.setText("Đang chờ đối thủ trả lời...");
        submitAnswer(index);
    }
    
    private void submitAnswer(int answer) {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("questionNum", String.valueOf(currentQuestion));
        data.put("answer", String.valueOf(answer));
        socket.send("QUIZ_ANSWER", data);
    }
    
    public void showResult(int myAns, int opponentAns, boolean matched) {
        countdownTimer.stop();
        
        // Disable all buttons
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setEnabled(false);
        }
        
        // Highlight answers
        if (myAns >= 0 && myAns < 4) {
            answerButtons[myAns].setBorder(BorderFactory.createLineBorder(SELECTED_COLOR, 4));
        }
        if (opponentAns >= 0 && opponentAns < 4 && opponentAns != myAns) {
            answerButtons[opponentAns].setBorder(BorderFactory.createLineBorder(WRONG_COLOR, 4));
        }
        
        if (matched) {
            score++;
            scoreLabel.setText("Diem: " + score);
            statusLabel.setText("<3 Ca 2 chon giong nhau! +1 diem");
            statusLabel.setForeground(CORRECT_COLOR);
        } else {
            if (myAns < 0) {
                statusLabel.setText("Het gio! Ban khong tra loi kip!");
            } else if (opponentAns < 0) {
                statusLabel.setText("Het gio! Doi thu khong tra loi kip!");
            } else {
                statusLabel.setText("Khong trung dap an...");
            }
            statusLabel.setForeground(WRONG_COLOR);
        }
        
        progressBar.setValue(currentQuestion);
        progressBar.setString((currentQuestion * 100 / totalQuestions) + "%");
        
        // Wait 2 seconds then request next question
        Timer nextTimer = new Timer(2500, e -> {
            statusLabel.setForeground(Color.LIGHT_GRAY);
            if (currentQuestion < totalQuestions) {
                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("sessionId", String.valueOf(sessionId));
                socket.send("QUIZ_NEXT", data);
            }
        });
        nextTimer.setRepeats(false);
        nextTimer.start();
    }
    
    public void onGameEnd(int finalScore, int maxScore) {
        countdownTimer.stop();
        
        for (JButton btn : answerButtons) {
            btn.setEnabled(false);
        }
        
        int percentage = (finalScore * 100) / maxScore;
        String message;
        String emoji;
        
        if (percentage >= 80) {
            emoji = "<3<3<3";
            message = "TUYET VOI! Hai ban rat hop nhau!";
        } else if (percentage >= 60) {
            emoji = "<3<3";
            message = "Kha tot! Hai ban kha hieu nhau!";
        } else if (percentage >= 40) {
            emoji = "<3";
            message = "Tam on! Can tim hieu them nhe!";
        } else {
            emoji = "</3";
            message = "Hmm... Hai ban can noi chuyen nhieu hon!";
        }
        
        JOptionPane.showMessageDialog(this, 
            emoji + "\n\n" +
            "Kết quả: " + finalScore + "/" + maxScore + " (" + percentage + "%)\n\n" +
            message,
            "Kết thúc Quiz", JOptionPane.INFORMATION_MESSAGE);
        
        if (mainFrame != null) {
            mainFrame.showCard("LOBBY");
        }
    }
    
    private void quitGame() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc muốn thoát?", 
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            countdownTimer.stop();
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            socket.send("QUIZ_QUIT", data);
            
            if (mainFrame != null) {
                mainFrame.showCard("LOBBY");
            }
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
