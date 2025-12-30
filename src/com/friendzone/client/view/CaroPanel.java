package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.friendzone.client.controller.ClientSocket;

public class CaroPanel extends JPanel {
    private static final int SIZE = 15;
    private static final int WIN_COUNT = 5; // Số quân liên tiếp để thắng
    private static final int WARN_COUNT = 4; // Số quân liên tiếp để cảnh báo
    private JButton[][] buttons;
    private String[][] board; // Lưu trạng thái bàn cờ
    private ClientSocket socket;
    private long sessionId;
    private long myId;
    private long opponentId;
    private boolean isMyTurn;
    private String mySymbol; // "X" hoặc "O"
    private JLabel statusLabel;
    private JButton leaveButton;
    private boolean gameEnded = false;
    private MainFrame mainFrame;
    
    // Blink warning for 4-in-a-row
    private Timer blinkTimer;
    private java.util.List<int[]> warningCells = new java.util.ArrayList<>();
    private boolean blinkOn = false;
    private static final Color BLINK_COLOR = new Color(255, 0, 0); // Đỏ
    private static final Color NORMAL_BG = new Color(236, 240, 241);
    
    // Modern Theme Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color STATUS_BG = new Color(40, 40, 60);

    public CaroPanel(ClientSocket socket) {
        this.socket = socket;
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        
        // Status label - dùng Segoe UI cho tiếng Việt
        statusLabel = new JLabel("Đang chờ trận đấu...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(STATUS_BG);
        statusLabel.setPreferredSize(new Dimension(0, 50));
        add(statusLabel, BorderLayout.NORTH);
        
        // Game board
        JPanel gridPanel = new JPanel(new GridLayout(SIZE, SIZE, 1, 1));
        gridPanel.setBackground(PANEL_BG);
        buttons = new JButton[SIZE][SIZE];
        board = new String[SIZE][SIZE];
        
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                JButton btn = new JButton("");
                btn.setBackground(new Color(236, 240, 241));
                btn.setFocusPainted(false);
                btn.setFont(new Font("Arial", Font.BOLD, 20));
                btn.setPreferredSize(new Dimension(35, 35));
                final int x = i;
                final int y = j;
                btn.addActionListener(e -> onCellClick(x, y));
                buttons[i][j] = btn;
                board[i][j] = "";
                gridPanel.add(btn);
            }
        }
        add(gridPanel, BorderLayout.CENTER);
        
        // Bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(BG_COLOR);
        
        leaveButton = new JButton("Rời trận");
        leaveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leaveButton.setBackground(new Color(231, 76, 60));
        leaveButton.setForeground(Color.WHITE);
        leaveButton.setFocusPainted(false);
        leaveButton.addActionListener(e -> leaveGame());
        bottomPanel.add(leaveButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void setMainFrame(MainFrame frame) {
        this.mainFrame = frame;
    }
    
    public void startGame(long sessionId, long myId, long opponentId, boolean isFirst) {
        this.sessionId = sessionId;
        this.myId = myId;
        this.opponentId = opponentId;
        this.isMyTurn = isFirst;
        this.mySymbol = isFirst ? "X" : "O";
        this.gameEnded = false;
        
        resetBoard();
        updateStatus();
    }
    
    // Overload cho compatibility
    public void startGame(long sessionId, long myId, boolean isFirst) {
        startGame(sessionId, myId, 0, isFirst);
    }
    
    private void resetBoard() {
        stopBlinkTimer(); // Dừng nhấp nháy khi reset
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackground(new Color(236, 240, 241));
                board[i][j] = "";
            }
        }
    }
    
    private void updateStatus() {
        SwingUtilities.invokeLater(() -> {
            if (gameEnded) return;
            
            if (isMyTurn) {
                statusLabel.setText("Lượt của bạn (" + mySymbol + ")");
                statusLabel.setBackground(new Color(46, 204, 113)); // Green
            } else {
                statusLabel.setText("Lượt đối thủ...");
                statusLabel.setBackground(new Color(231, 76, 60)); // Red
            }
        });
    }
    
    /**
     * Bắt đầu blink timer để nhấp nháy các ô cảnh báo
     */
    private void startBlinkTimer() {
        if (blinkTimer != null && blinkTimer.isRunning()) {
            return; // Đã đang chạy
        }
        
        blinkTimer = new Timer(400, e -> {
            blinkOn = !blinkOn;
            SwingUtilities.invokeLater(() -> {
                for (int[] cell : warningCells) {
                    int x = cell[0];
                    int y = cell[1];
                    if (blinkOn) {
                        buttons[x][y].setBackground(BLINK_COLOR);
                    } else {
                        // Trả về màu nền nhưng giữ text
                        buttons[x][y].setBackground(NORMAL_BG);
                    }
                }
            });
        });
        blinkTimer.start();
    }
    
    /**
     * Dừng blink timer và reset màu các ô
     */
    private void stopBlinkTimer() {
        if (blinkTimer != null) {
            blinkTimer.stop();
            blinkTimer = null;
        }
        
        // Reset màu tất cả các ô đang cảnh báo
        for (int[] cell : warningCells) {
            int x = cell[0];
            int y = cell[1];
            buttons[x][y].setBackground(NORMAL_BG);
        }
        warningCells.clear();
        blinkOn = false;
    }
    
    /**
     * Kiểm tra và đánh dấu các ô có 4 quân liên tiếp (chỉ cảnh báo nếu còn đầu mở)
     */
    private void checkAndWarnFourInRow() {
        // Dừng blink cũ
        stopBlinkTimer();
        
        java.util.Set<String> foundCells = new java.util.HashSet<>();
        
        // Duyệt tất cả các ô đã có quân
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                String symbol = board[i][j];
                if (symbol.isEmpty()) continue;
                
                // Kiểm tra 4 hướng
                int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
                for (int[] dir : directions) {
                    int count = countLine(i, j, dir[0], dir[1], symbol);
                    
                    // Nếu có đúng 4 quân (không phải 5+ vì đó là thắng)
                    if (count == WARN_COUNT) {
                        // Kiểm tra xem có bị chặn 2 đầu không
                        if (!isBlockedBothEnds(i, j, dir[0], dir[1], symbol)) {
                            // Chỉ cảnh báo nếu còn ít nhất 1 đầu mở
                            collectLineCells(i, j, dir[0], dir[1], symbol, foundCells);
                        }
                    }
                }
            }
        }
        
        // Chuyển từ Set sang List
        for (String key : foundCells) {
            String[] parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            warningCells.add(new int[]{x, y});
        }
        
        // Nếu có ô cần cảnh báo, bắt đầu blink
        if (!warningCells.isEmpty()) {
            startBlinkTimer();
        }
    }
    
    /**
     * Kiểm tra xem dòng có bị chặn cả 2 đầu không
     * @return true nếu bị chặn cả 2 đầu (không thể thắng được)
     */
    private boolean isBlockedBothEnds(int x, int y, int dx, int dy, String symbol) {
        // Tìm 2 đầu mút của dòng
        int startX = x, startY = y;
        int endX = x, endY = y;
        
        // Tìm điểm đầu (đi ngược hướng)
        while (startX - dx >= 0 && startX - dx < SIZE && 
               startY - dy >= 0 && startY - dy < SIZE && 
               board[startX - dx][startY - dy].equals(symbol)) {
            startX -= dx;
            startY -= dy;
        }
        
        // Tìm điểm cuối (đi theo hướng)
        while (endX + dx >= 0 && endX + dx < SIZE && 
               endY + dy >= 0 && endY + dy < SIZE && 
               board[endX + dx][endY + dy].equals(symbol)) {
            endX += dx;
            endY += dy;
        }
        
        // Kiểm tra ô trước điểm đầu
        int beforeX = startX - dx;
        int beforeY = startY - dy;
        boolean blockedStart = false;
        if (beforeX < 0 || beforeX >= SIZE || beforeY < 0 || beforeY >= SIZE) {
            blockedStart = true; // Ra ngoài bàn cờ
        } else if (!board[beforeX][beforeY].isEmpty()) {
            blockedStart = true; // Có quân đối thủ chặn
        }
        
        // Kiểm tra ô sau điểm cuối
        int afterX = endX + dx;
        int afterY = endY + dy;
        boolean blockedEnd = false;
        if (afterX < 0 || afterX >= SIZE || afterY < 0 || afterY >= SIZE) {
            blockedEnd = true; // Ra ngoài bàn cờ
        } else if (!board[afterX][afterY].isEmpty()) {
            blockedEnd = true; // Có quân đối thủ chặn
        }
        
        // Trả về true nếu bị chặn CẢ 2 đầu
        return blockedStart && blockedEnd;
    }
    
    /**
     * Thu thập tất cả các ô trong một dòng có cùng symbol
     */
    private void collectLineCells(int x, int y, int dx, int dy, String symbol, java.util.Set<String> cells) {
        // Thêm ô hiện tại
        cells.add(x + "," + y);
        
        // Thu thập về một hướng
        int nx = x + dx, ny = y + dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            cells.add(nx + "," + ny);
            nx += dx;
            ny += dy;
        }
        
        // Thu thập về hướng ngược lại
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            cells.add(nx + "," + ny);
            nx -= dx;
            ny -= dy;
        }
    }

    private void onCellClick(int x, int y) {
        if (gameEnded) return;
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(this, "Chưa đến lượt của bạn!");
            return;
        }
        if (!board[x][y].isEmpty()) return;
        
        // QUAN TRỌNG: Set isMyTurn = false NGAY để tránh race condition
        isMyTurn = false;
        updateStatus();
        
        // Đánh dấu ô
        board[x][y] = mySymbol;
        buttons[x][y].setText(mySymbol);
        buttons[x][y].setForeground(new Color(52, 152, 219)); // Blue
        buttons[x][y].setEnabled(false);
        
        // Gửi nước đi
        sendMove(x, y);
        
        // Kiểm tra thắng
        if (checkWin(x, y, mySymbol)) {
            gameEnded = true;
            stopBlinkTimer(); // Dừng nhấp nháy khi game kết thúc
            highlightWinningLine(x, y, mySymbol);
            statusLabel.setText("🎉 BẠN THẮNG! 🎉");
            statusLabel.setBackground(new Color(241, 196, 15)); // Gold
            
            // Gửi kết quả
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            data.put("winnerId", String.valueOf(myId));
            data.put("opponentId", String.valueOf(opponentId));
            data.put("gameType", "CARO");
            socket.send("GAME_END", data);
            
            // Hỏi chơi tiếp
            askPlayAgain(true);
            return;
        }
        
        // Kiểm tra hòa
        if (isBoardFull()) {
            gameEnded = true;
            stopBlinkTimer(); // Dừng nhấp nháy khi game kết thúc
            statusLabel.setText("Hòa!");
            statusLabel.setBackground(new Color(149, 165, 166));
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            data.put("winnerId", "0"); // 0 = hòa
            data.put("opponentId", String.valueOf(opponentId));
            data.put("gameType", "CARO");
            socket.send("GAME_END", data);
            
            // Hỏi chơi tiếp
            askPlayAgain(false);
            return;
        }
        
        // Kiểm tra cảnh báo 4 quân liên tiếp
        checkAndWarnFourInRow();
        
        // isMyTurn đã set = false ở đầu hàm rồi, không cần set lại
    }
    
    public void onOpponentMove(int x, int y) {
        SwingUtilities.invokeLater(() -> {
            if (gameEnded) return;
            
            String opponentSymbol = mySymbol.equals("X") ? "O" : "X";
            board[x][y] = opponentSymbol;
            buttons[x][y].setText(opponentSymbol);
            buttons[x][y].setForeground(new Color(231, 76, 60)); // Red
            buttons[x][y].setEnabled(false);
            
            // Kiểm tra đối thủ thắng
            if (checkWin(x, y, opponentSymbol)) {
                gameEnded = true;
                stopBlinkTimer(); // Dừng nhấp nháy khi game kết thúc
                highlightWinningLine(x, y, opponentSymbol);
                statusLabel.setText("😢 BẠN THUA! 😢");
                statusLabel.setBackground(new Color(231, 76, 60));
                // Hỏi chơi tiếp
                askPlayAgain(false);
                return;
            }
            
            // Kiểm tra hòa
            if (isBoardFull()) {
                gameEnded = true;
                stopBlinkTimer(); // Dừng nhấp nháy khi game kết thúc
                statusLabel.setText("Hòa!");
                statusLabel.setBackground(new Color(149, 165, 166));
                // Hỏi chơi tiếp
                askPlayAgain(false);
                return;
            }
            
            // Kiểm tra cảnh báo 4 quân liên tiếp
            checkAndWarnFourInRow();
            
            isMyTurn = true;
            updateStatus();
        });
    }
    
    private boolean checkWin(int x, int y, String symbol) {
        // Kiểm tra 4 hướng: ngang, dọc, chéo chính, chéo phụ
        return countLine(x, y, 0, 1, symbol) >= WIN_COUNT ||  // Ngang
               countLine(x, y, 1, 0, symbol) >= WIN_COUNT ||  // Dọc
               countLine(x, y, 1, 1, symbol) >= WIN_COUNT ||  // Chéo \
               countLine(x, y, 1, -1, symbol) >= WIN_COUNT;   // Chéo /
    }
    
    private int countLine(int x, int y, int dx, int dy, String symbol) {
        int count = 1;
        
        // Đếm về một hướng
        int nx = x + dx, ny = y + dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            count++;
            nx += dx;
            ny += dy;
        }
        
        // Đếm về hướng ngược lại
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        
        return count;
    }
    
    private void highlightWinningLine(int x, int y, String symbol) {
        // Tìm và highlight dòng thắng
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            if (countLine(x, y, dir[0], dir[1], symbol) >= WIN_COUNT) {
                // Highlight dòng này
                highlightDirection(x, y, dir[0], dir[1], symbol);
                break;
            }
        }
    }
    
    private void highlightDirection(int x, int y, int dx, int dy, String symbol) {
        Color winColor = new Color(241, 196, 15); // Gold
        buttons[x][y].setBackground(winColor);
        
        // Highlight về một hướng
        int nx = x + dx, ny = y + dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            buttons[nx][ny].setBackground(winColor);
            nx += dx;
            ny += dy;
        }
        
        // Highlight về hướng ngược lại
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny].equals(symbol)) {
            buttons[nx][ny].setBackground(winColor);
            nx -= dx;
            ny -= dy;
        }
    }
    
    private boolean isBoardFull() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j].isEmpty()) return false;
            }
        }
        return true;
    }
    
    private void sendMove(int x, int y) {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sessionId", String.valueOf(sessionId));
        data.put("x", String.valueOf(x));
        data.put("y", String.valueOf(y));
        data.put("opponentId", String.valueOf(opponentId));
        socket.send("MOVE_CARO", data);
    }
    
    private void leaveGame() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn rời trận? Bạn sẽ thua nếu rời đi!",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            gameEnded = true;
            stopBlinkTimer(); // Dừng nhấp nháy khi rời trận
            
            // Gửi thông báo thua
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sessionId", String.valueOf(sessionId));
            data.put("winnerId", String.valueOf(opponentId)); // Đối thủ thắng
            data.put("opponentId", String.valueOf(opponentId));
            data.put("reason", "LEAVE");
            data.put("gameType", "CARO");
            socket.send("GAME_END", data);
            
            // Quay về lobby
            if (mainFrame != null) {
                mainFrame.showCard("LOBBY");
            }
        }
    }
    
    public void onGameEnd(String result, long winnerId) {
        gameEnded = true;
        stopBlinkTimer(); // Dừng nhấp nháy khi game kết thúc
        SwingUtilities.invokeLater(() -> {
            if (winnerId == myId) {
                statusLabel.setText("🎉 BẠN THẮNG! 🎉");
                statusLabel.setBackground(new Color(241, 196, 15));
            } else if (winnerId == 0) {
                statusLabel.setText("Hòa!");
                statusLabel.setBackground(new Color(149, 165, 166));
            } else {
                statusLabel.setText("😢 BẠN THUA! 😢");
                statusLabel.setBackground(new Color(231, 76, 60));
            }
        });
    }
    
    /**
     * Hỏi người chơi có muốn chơi lại không
     */
    private void askPlayAgain(boolean isWinner) {
        String message = isWinner ? 
            "🎉 Chúc mừng bạn đã thắng!\nBạn có muốn chơi lại với đối thủ này không?" :
            "Trận đấu đã kết thúc.\nBạn có muốn chơi lại với đối thủ này không?";
            
        int choice = JOptionPane.showConfirmDialog(this,
            message,
            "Chơi lại?",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
            
        if (choice == JOptionPane.YES_OPTION) {
            // Gửi yêu cầu chơi lại đến server
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("opponentId", String.valueOf(opponentId));
            data.put("gameType", "CARO");
            socket.send("REMATCH_REQUEST", data);
            
            statusLabel.setText("Đang chờ đối thủ đồng ý...");
            statusLabel.setBackground(new Color(52, 152, 219)); // Blue - waiting
        } else {
            // Quay về lobby
            if (mainFrame != null) {
                mainFrame.showCard("LOBBY");
            }
        }
    }
    
    /**
     * Xử lý khi nhận được yêu cầu chơi lại từ đối thủ
     */
    public void onRematchRequest(long fromUserId) {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Đối thủ muốn chơi lại!\nBạn có đồng ý không?",
                "Yêu cầu chơi lại",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
                
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("opponentId", String.valueOf(fromUserId));
            data.put("gameType", "CARO");
            
            if (choice == JOptionPane.YES_OPTION) {
                data.put("accepted", "true");
                socket.send("REMATCH_RESPONSE", data);
            } else {
                data.put("accepted", "false");
                socket.send("REMATCH_RESPONSE", data);
                // Quay về lobby
                if (mainFrame != null) {
                    mainFrame.showCard("LOBBY");
                }
            }
        });
    }
    
    /**
     * Xử lý khi đối thủ phản hồi yêu cầu chơi lại
     */
    public void onRematchResponse(boolean accepted, long newSessionId) {
        SwingUtilities.invokeLater(() -> {
            if (accepted) {
                // Bắt đầu ván mới
                JOptionPane.showMessageDialog(this,
                    "Đối thủ đồng ý!\nBắt đầu ván mới...",
                    "Chơi lại",
                    JOptionPane.INFORMATION_MESSAGE);
                    
                // Đổi lượt (người thua ván trước được đi trước)
                boolean wasFirst = mySymbol.equals("X");
                startGame(newSessionId, myId, opponentId, !wasFirst);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Đối thủ từ chối chơi lại.",
                    "Từ chối",
                    JOptionPane.INFORMATION_MESSAGE);
                    
                // Quay về lobby
                if (mainFrame != null) {
                    mainFrame.showCard("LOBBY");
                }
            }
        });
    }
}
