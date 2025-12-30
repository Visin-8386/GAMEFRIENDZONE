package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class RegisterPanel extends JPanel {
    private JTextField userField;
    private JTextField nickField;
    private JPasswordField passField;
    private JPasswordField confirmField;
    private JComboBox<String> genderBox;
    private JButton registerButton;
    private JButton backButton;
    private ClientSocket socket;
    private MainFrame mainFrame;

    // Theme Colors
    // Modern Theme Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color BTN_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color INPUT_BG = new Color(40, 40, 60);
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);

    public RegisterPanel(ClientSocket socket, MainFrame mainFrame) {
        this.socket = socket;
        this.mainFrame = mainFrame;
        
        setLayout(new GridBagLayout());
        setBackground(BG_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("Tạo tài khoản");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);
        
        // Reset width
        gbc.gridwidth = 1;
        
        // Username
        addLabel("Tên đăng nhập:", 1, gbc);
        userField = createTextField();
        gbc.gridx = 1; 
        add(userField, gbc);
        
        // Nickname
        addLabel("Biệt danh:", 2, gbc);
        nickField = createTextField();
        gbc.gridx = 1;
        add(nickField, gbc);
        
        // Password
        addLabel("Mật khẩu:", 3, gbc);
        passField = createPasswordField();
        gbc.gridx = 1;
        add(passField, gbc);
        
        // Confirm Password
        addLabel("Nhập lại mật khẩu:", 4, gbc);
        confirmField = createPasswordField();
        gbc.gridx = 1;
        add(confirmField, gbc);
        
        // Gender
        addLabel("Giới tính:", 5, gbc);
        genderBox = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        genderBox.setFont(MAIN_FONT);
        gbc.gridx = 1;
        add(genderBox, gbc);
        
        // Register Button
        registerButton = new JButton("ĐĂNG KÝ");
        styleButton(registerButton, BTN_COLOR);
        registerButton.addActionListener(e -> onRegister());
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        add(registerButton, gbc);
        
        // Back Button
        backButton = new JButton("Quay lại đăng nhập");
        styleButton(backButton, new Color(127, 140, 141));
        backButton.addActionListener(e -> mainFrame.showCard("LOGIN"));
        gbc.gridy = 7;
        gbc.insets = new Insets(5, 10, 10, 10);
        add(backButton, gbc);
    }
    
    private void addLabel(String text, int y, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setFont(MAIN_FONT);
        label.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = y;
        add(label, gbc);
    }
    
    private JTextField createTextField() {
        JTextField tf = new JTextField(15);
        tf.setFont(MAIN_FONT);
        tf.setBackground(INPUT_BG);
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(100, 100, 120)), 
            javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return tf;
    }
    
    private JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField(15);
        pf.setFont(MAIN_FONT);
        pf.setBackground(INPUT_BG);
        pf.setForeground(Color.WHITE);
        pf.setCaretColor(Color.WHITE);
        pf.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(100, 100, 120)), 
            javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return pf;
    }
    
    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        // If neon (BTN_COLOR), use black text, otherwise white
        if (bg.equals(BTN_COLOR)) {
            btn.setForeground(Color.BLACK);
        } else {
            btn.setForeground(Color.WHITE);
        }
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

    private void onRegister() {
        String user = userField.getText();
        String nick = nickField.getText();
        String pass = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());
        
        if (user.isEmpty() || nick.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin");
            return;
        }
        
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không khớp");
            return;
        }
        
        String gender = (String) genderBox.getSelectedItem();
        // Chuyển đổi giới tính sang tiếng Anh
        if ("Nam".equals(gender)) gender = "MALE";
        else if ("Nữ".equals(gender)) gender = "FEMALE";
        else if ("Khác".equals(gender)) gender = "OTHER";
        
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("username", user);
        data.put("password", pass);
        data.put("nickname", nick);
        data.put("gender", gender);
        socket.send("REGISTER", data);
    }
}
