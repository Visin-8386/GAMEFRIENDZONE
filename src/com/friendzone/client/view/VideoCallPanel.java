package com.friendzone.client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.friendzone.client.audio.AudioReceiver;
import com.friendzone.client.audio.AudioSender;
import com.friendzone.client.controller.ClientSocket;
import com.friendzone.client.video.VideoReceiver;
import com.friendzone.client.video.VideoSender;

public class VideoCallPanel extends JPanel {
    private JLabel remoteVideoLabel; // Video của đối phương
    private JLabel localVideoLabel;  // Video của bản thân (nhỏ)
    private JLabel statusLabel;
    private JButton endCallButton;
    private JButton muteButton;
    private VideoSender videoSender;
    private VideoReceiver videoReceiver;
    private AudioSender audioSender;
    private AudioReceiver audioReceiver;
    private long callId;
    private long otherUserId;
    private Runnable onCallEnd;
    private ClientSocket socket;
    private boolean isVideoCall = true; // true = video call, false = voice call
    private boolean isMuted = false;
    
    // Colors
    private static final Color BG_COLOR = new Color(20, 20, 30); // Deep Dark Blue/Black
    private static final Color PANEL_BG = new Color(30, 30, 45); // Dark Grey-Blue
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(46, 204, 113); // Green
    private static final Color MUTE_COLOR = new Color(52, 152, 219); // Blue
    private static final Color END_CALL_COLOR = new Color(231, 76, 60); // Red
    
    public VideoCallPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        
        // Panel chứa video
        JPanel videoPanel = new JPanel(new BorderLayout());
        videoPanel.setBackground(Color.BLACK); // Video background should remain black
        
        // Video đối phương (to)
        remoteVideoLabel = new JLabel("Đang chờ video từ đối phương...", SwingConstants.CENTER);
        remoteVideoLabel.setForeground(Color.WHITE);
        remoteVideoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        remoteVideoLabel.setPreferredSize(new Dimension(640, 480));
        remoteVideoLabel.setMinimumSize(new Dimension(320, 240));
        remoteVideoLabel.setBackground(new Color(30, 30, 30));
        remoteVideoLabel.setOpaque(true);
        remoteVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        remoteVideoLabel.setVerticalAlignment(SwingConstants.CENTER);
        videoPanel.add(remoteVideoLabel, BorderLayout.CENTER);
        
        // Video bản thân (nhỏ, góc phải dưới)
        localVideoLabel = new JLabel("Camera của bạn", SwingConstants.CENTER);
        localVideoLabel.setForeground(Color.WHITE);
        localVideoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        localVideoLabel.setPreferredSize(new Dimension(160, 120));
        localVideoLabel.setBackground(new Color(50, 50, 50));
        localVideoLabel.setOpaque(true);
        localVideoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        JPanel localPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        localPanel.setOpaque(false);
        localPanel.add(localVideoLabel);
        videoPanel.add(localPanel, BorderLayout.SOUTH);
        
        add(videoPanel, BorderLayout.CENTER);
        
        // Panel điều khiển
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBackground(PANEL_BG);
        
        statusLabel = new JLabel("Đang kết nối...");
        statusLabel.setForeground(ACCENT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Nút Mute/Unmute
        muteButton = new JButton("Tắt mic");
        styleButton(muteButton, MUTE_COLOR);
        muteButton.addActionListener(e -> toggleMute());
        
        endCallButton = new JButton("Kết thúc");
        styleButton(endCallButton, END_CALL_COLOR);
        endCallButton.addActionListener(e -> endCall());
        
        controlPanel.add(statusLabel);
        controlPanel.add(muteButton);
        controlPanel.add(endCallButton);
        
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
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
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            muteButton.setText("Bật mic");
            muteButton.setBackground(END_CALL_COLOR);
            // Dừng gửi audio
            if (audioSender != null) {
                audioSender.stop();
                audioSender = null;
            }
        } else {
            muteButton.setText("Tắt mic");
            muteButton.setBackground(MUTE_COLOR);
            // Bắt đầu gửi audio lại - cần lưu thông tin targetIp, targetPort
        }
    }
    
    public void setSocket(ClientSocket socket) {
        this.socket = socket;
    }
    
    public void setCallInfo(long callId, long otherUserId) {
        this.callId = callId;
        this.otherUserId = otherUserId;
    }
    
    public void setVideoCall(boolean isVideo) {
        this.isVideoCall = isVideo;
    }
    
    public void setCallType(String callType) {
        this.isVideoCall = "VIDEO".equals(callType);
    }
    
    public void setOnCallEnd(Runnable callback) {
        this.onCallEnd = callback;
    }
    
    public void startCall(String targetIp, int targetPort, int listenPort) {
        statusLabel.setText("🔴 Đang gọi với " + targetIp);
        isMuted = false;
        muteButton.setText("Tắt mic");
        muteButton.setBackground(MUTE_COLOR);
        
        // Audio port = video port + 1000 (để tránh conflict)
        int audioTargetPort = targetPort + 1000;
        int audioListenPort = listenPort + 1000;
        
        if (isVideoCall) {
            // Video call - cần camera + audio
            remoteVideoLabel.setText("Đang kết nối video...");
            
            // Video Receiver - lắng nghe video từ đối phương
            videoReceiver = new VideoReceiver(listenPort, remoteVideoLabel);
            new Thread(videoReceiver).start();
            
            // Video Sender - gửi video của mình đến đối phương
            videoSender = new VideoSender(targetIp, targetPort, localVideoLabel);
            new Thread(videoSender).start();
            
            // Audio cũng được bật cho video call
            audioReceiver = new AudioReceiver(audioListenPort);
            new Thread(audioReceiver).start();
            
            audioSender = new AudioSender(targetIp, audioTargetPort);
            new Thread(audioSender).start();
            
            statusLabel.setText("Đang trong cuộc gọi video");
        } else {
            // Voice call - chỉ audio, không cần camera
                remoteVideoLabel.setText("<html><center><br><br><br>" +
                "<span style='font-size:18px'>Cuộc gọi thoại</span><br>" +
                "<span style='font-size:14px'>Đang kết nối...</span>" +
                "</center></html>");
            localVideoLabel.setText("Mic");
            
            // Audio Receiver - lắng nghe audio từ đối phương
            audioReceiver = new AudioReceiver(audioListenPort);
            new Thread(audioReceiver).start();
            
            // Audio Sender - gửi audio của mình đến đối phương
            audioSender = new AudioSender(targetIp, audioTargetPort);
            new Thread(audioSender).start();
            
            statusLabel.setText("Đang trong cuộc gọi thoại");
            
            // Cập nhật UI khi đã kết nối
            javax.swing.SwingUtilities.invokeLater(() -> {
                remoteVideoLabel.setText("<html><center><br><br><br>" +
                    "<span style='font-size:18px'>Đang gọi...</span><br>" +
                    "<span style='font-size:14px; color:#2ecc71'>Đã kết nối</span>" +
                    "</center></html>");
            });
        }
    }
    
    private void endCall() {
        // Gửi thông báo kết thúc đến server
        if (socket != null) {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("callId", String.valueOf(callId));
            data.put("peerId", String.valueOf(otherUserId));
            data.put("reason", "USER_ENDED");
            socket.send("VIDEO_CALL_END", data);
        }
        
        stopCall();
    }
    
    public void stopCall() {
        // Dừng video
        if (videoSender != null) {
            videoSender.stop();
            videoSender = null;
        }
        if (videoReceiver != null) {
            videoReceiver.stop();
            videoReceiver = null;
        }
        
        // Dừng audio
        if (audioSender != null) {
            audioSender.stop();
            audioSender = null;
        }
        if (audioReceiver != null) {
            audioReceiver.stop();
            audioReceiver = null;
        }
        
        remoteVideoLabel.setIcon(null);
        remoteVideoLabel.setText("Cuộc gọi đã kết thúc");
        localVideoLabel.setIcon(null);
        localVideoLabel.setText("");
        statusLabel.setText("Đã kết thúc");
        isMuted = false;
        
        if (onCallEnd != null) {
            onCallEnd.run();
        }
    }
    
    public long getCallId() {
        return callId;
    }
    
    public long getOtherUserId() {
        return otherUserId;
    }
}
