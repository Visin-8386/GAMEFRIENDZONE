package com.friendzone.client.view;

import com.friendzone.client.controller.ClientSocket;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginPanel extends JPanel {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JButton registerButton;
    private ClientSocket socket;
    private MainFrame mainFrame;

    // Theme Colors
    // Modern Theme Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color BTN_COLOR = new Color(100, 255, 218); // Cyan/Teal Neon
    private static final Color INPUT_BG = new Color(40, 40, 60);
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 32);

    public LoginPanel(ClientSocket socket, MainFrame mainFrame) {
        this.socket = socket;
        this.mainFrame = mainFrame;
        
        setLayout(new GridBagLayout());
        setBackground(BG_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("FRIENDZONE");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(231, 76, 60)); // Red accent
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 30, 10);
        add(titleLabel, gbc);
        
        // Reset insets
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;
        
        // Username
        JLabel userLabel = new JLabel("Tên đăng nhập:");
        userLabel.setFont(MAIN_FONT);
        userLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = 1;
        add(userLabel, gbc);
        
        userField = new JTextField(15);
        userField.setFont(MAIN_FONT);
        userField.setBackground(INPUT_BG);
        userField.setForeground(Color.WHITE);
        userField.setCaretColor(Color.WHITE);
        userField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(100, 100, 120)), 
            javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1; 
        add(userField, gbc);
        
        // Password
        JLabel passLabel = new JLabel("Mật khẩu:");
        passLabel.setFont(MAIN_FONT);
        passLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = 2;
        add(passLabel, gbc);
        
        passField = new JPasswordField(15);
        passField.setFont(MAIN_FONT);
        passField.setBackground(INPUT_BG);
        passField.setForeground(Color.WHITE);
        passField.setCaretColor(Color.WHITE);
        passField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(100, 100, 120)), 
            javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1; 
        add(passField, gbc);
        
        // Login Button
        loginButton = new JButton("ĐĂNG NHẬP");
        styleButton(loginButton, BTN_COLOR, Color.BLACK); // Black text on Neon button
        loginButton.addActionListener(this::onLoginClick);
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        add(loginButton, gbc);
        
        // Register Button
        registerButton = new JButton("Tạo tài khoản mới");
        styleButton(registerButton, new Color(60, 60, 80), Color.WHITE); // Dark Grey
        registerButton.addActionListener(e -> mainFrame.showCard("REGISTER"));
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 10, 10, 10);
        add(registerButton, gbc);
    }
    
    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Modern hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
    }

    private void onLoginClick(ActionEvent e) {
        String user = userField.getText();
        String pass = new String(passField.getPassword());
        
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }
        
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("username", user);
        data.put("password", pass);
        socket.send("LOGIN", data);
    }
}
