package com.friendzone.client.view;

import com.friendzone.model.User;
import javax.swing.*;
import java.awt.*;

/**
 * Custom cell renderer to display user avatars in JList
 */
public class UserListCellRenderer extends DefaultListCellRenderer {
    
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color SELECTED_BG = new Color(33, 150, 243);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, 
            int index, boolean isSelected, boolean cellHasFocus) {
        
        JLabel label = (JLabel) super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof String) {
            String text = (String) value;
            label.setText(text);
            
            // Set avatar icon (default circle icon for now)
            ImageIcon icon = createDefaultAvatar();
            label.setIcon(icon);
            label.setIconTextGap(10);
        }
        
        // Styling
        label.setOpaque(true);
        label.setBackground(isSelected ? SELECTED_BG : BG_COLOR);
        label.setForeground(isSelected ? Color.WHITE : TEXT_COLOR);
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        return label;
    }
    
    /**
     * Creates a simple circular default avatar
     */
    private ImageIcon createDefaultAvatar() {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw circle
        g2d.setColor(new Color(100, 150, 200));
        g2d.fillOval(0, 0, size, size);
        
        // Draw border
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(1, 1, size-2, size-2);
        
        g2d.dispose();
        return new ImageIcon(img);
    }
}
