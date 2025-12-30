package com.friendzone.client.video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class VideoReceiver implements Runnable {
    private DatagramSocket socket;
    private int port;
    private JLabel displayLabel;
    private boolean running = true;

    public VideoReceiver(int port, JLabel displayLabel) {
        this.port = port;
        this.displayLabel = displayLabel;
        try {
            this.socket = new DatagramSocket(null);
            this.socket.setReuseAddress(true);
            this.socket.bind(new java.net.InetSocketAddress(port));
            System.out.println("VideoReceiver initialized successfully on port " + port);
        } catch (Exception e) {
            System.err.println("Error initializing VideoReceiver on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (socket == null) {
            System.err.println("VideoReceiver: Socket not initialized");
            return;
        }
        
        System.out.println("Video Receiver Listening on port " + port + " (waiting for packets from remote...)");
        byte[] buffer = new byte[65535]; // Max UDP size
        int frameCount = 0;
        long startTime = System.currentTimeMillis();
        boolean warnedNoVideo = false;
        
        // Set timeout để có thể check running flag
        try {
            socket.setSoTimeout(1000); // 1 second timeout
        } catch (Exception e) {
            System.err.println("Failed to set socket timeout: " + e.getMessage());
        }
        
        while (running) {
            // Warn if no video after 5 seconds
            if (!warnedNoVideo && frameCount == 0 && System.currentTimeMillis() - startTime > 5000) {
                warnedNoVideo = true;
                System.out.println("⚠️ VideoReceiver: No video received after 5 seconds");
                System.out.println("   Possible reasons:");
                System.out.println("   1. Remote peer doesn't have camera");
                System.out.println("   2. Firewall blocking UDP port " + port);
                System.out.println("   3. Network/NAT issue");
                javax.swing.SwingUtilities.invokeLater(() -> {
                    displayLabel.setText("<html><center>Đang chờ video...<br><small>Đối phương có thể chưa bật camera</small></center></html>");
                });
            }
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blocking call with timeout
                
                frameCount++;
                if (frameCount == 1) {
                    System.out.println("VideoReceiver: First packet received from " + 
                                     packet.getAddress() + ":" + packet.getPort() + 
                                     ", size: " + packet.getLength() + " bytes");
                }
                if (frameCount % 30 == 0) { // Log every 30 frames (2 seconds at 15fps)
                    System.out.println("VideoReceiver: Received " + frameCount + " frames, last size: " + packet.getLength() + " bytes");
                }
                
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                BufferedImage image = ImageIO.read(bais);
                
                if (image != null) {
                    if (frameCount == 1) {
                        System.out.println("VideoReceiver: Successfully decoded first image: " + 
                                         image.getWidth() + "x" + image.getHeight());
                    }
                    
                    // Update UI on EDT
                    final BufferedImage finalImage = image;
                    final int currentFrame = frameCount; // Make final copy for lambda
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            // Scale image to fit label if needed
                            int labelWidth = displayLabel.getWidth();
                            int labelHeight = displayLabel.getHeight();
                            
                            if (labelWidth > 0 && labelHeight > 0) {
                                // Calculate scale to fit
                                double scaleW = (double) labelWidth / finalImage.getWidth();
                                double scaleH = (double) labelHeight / finalImage.getHeight();
                                double scale = Math.min(scaleW, scaleH);
                                
                                int scaledWidth = (int) (finalImage.getWidth() * scale);
                                int scaledHeight = (int) (finalImage.getHeight() * scale);
                                
                                java.awt.Image scaledImage = finalImage.getScaledInstance(
                                    scaledWidth, scaledHeight, java.awt.Image.SCALE_FAST
                                );
                                
                                ImageIcon icon = new ImageIcon(scaledImage);
                                displayLabel.setIcon(icon);
                                displayLabel.setText(""); // Clear text khi có video
                            } else {
                                // Label chưa có size, dùng original
                                ImageIcon icon = new ImageIcon(finalImage);
                                displayLabel.setIcon(icon);
                                displayLabel.setText("");
                            }
                            
                            displayLabel.revalidate();
                            displayLabel.repaint();
                            
                            if (currentFrame == 1) {
                                System.out.println("VideoReceiver: First frame displayed on UI");
                            }
                        } catch (Exception e) {
                            System.err.println("Error updating UI: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } else {
                    if (frameCount <= 5) { // Only log first few failures
                        System.err.println("VideoReceiver: Failed to decode image from packet (size: " + packet.getLength() + ")");
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                // Timeout is expected when waiting for packets - don't spam logs
                // Continue waiting
            } catch (Exception e) {
                if (running) {
                    System.err.println("VideoReceiver error: " + e.getMessage());
                }
            }
        }
        
        System.out.println("VideoReceiver stopped. Total frames received: " + frameCount);
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }
}
