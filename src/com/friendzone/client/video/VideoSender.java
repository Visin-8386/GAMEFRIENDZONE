package com.friendzone.client.video;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.github.sarxos.webcam.Webcam;

/**
 * Gửi video từ webcam qua UDP
 */
public class VideoSender implements Runnable {
    private Webcam webcam;
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;
    private boolean running = true;
    private JLabel previewLabel;
    
    // Video settings
    private static final Dimension VIDEO_SIZE = new Dimension(320, 240);
    private static final int MAX_PACKET_SIZE = 60000; // 60KB per packet
    private static final int FPS = 15; // 15 frames per second
    private static final float JPEG_QUALITY = 0.6f;

    public VideoSender(String targetIp, int targetPort) {
        this(targetIp, targetPort, null);
    }
    
    public VideoSender(String targetIp, int targetPort, JLabel previewLabel) {
        this.previewLabel = previewLabel;
        try {
            this.targetAddress = InetAddress.getByName(targetIp);
            this.targetPort = targetPort;
            this.socket = new DatagramSocket();
            
            // Khởi tạo webcam
            this.webcam = Webcam.getDefault();
            if (this.webcam != null) {
                this.webcam.setViewSize(VIDEO_SIZE);
                this.webcam.open();
                System.out.println("Webcam opened: " + webcam.getName());
                
                if (previewLabel != null) {
                    SwingUtilities.invokeLater(() -> previewLabel.setText("Camera đã kích hoạt"));
                }
            } else {
                System.err.println("No webcam found!");
                if (previewLabel != null) {
                    SwingUtilities.invokeLater(() -> previewLabel.setText("Không tìm thấy camera"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing VideoSender: " + e.getMessage());
            e.printStackTrace();
            if (previewLabel != null) {
                SwingUtilities.invokeLater(() -> previewLabel.setText("Lỗi camera: " + e.getMessage()));
            }
        }
    }

    @Override
    public void run() {
        if (webcam == null || !webcam.isOpen()) {
            System.err.println("VideoSender: Webcam not available");
            return;
        }
        
        System.out.println("Video Sender started -> " + targetAddress + ":" + targetPort);
        long frameDelay = 1000 / FPS; // Delay giữa các frame
        int frameCount = 0;
        
        while (running) {
            try {
                // Check if webcam still open
                if (!webcam.isOpen()) {
                    System.err.println("VideoSender: Webcam closed unexpectedly");
                    break;
                }
                
                long startTime = System.currentTimeMillis();
                
                // Capture frame từ webcam
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    frameCount++;
                    
                    // Log every 30 frames (2 seconds at 15fps)
                    if (frameCount % 30 == 0) {
                        System.out.println("VideoSender: Sent " + frameCount + " frames");
                    }
                    
                    // Hiển thị preview
                    if (previewLabel != null) {
                        SwingUtilities.invokeLater(() -> {
                            previewLabel.setIcon(new ImageIcon(image));
                            previewLabel.setText("");
                        });
                    }
                    
                    // Compress thành JPEG
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();
                    
                    // Gửi qua UDP
                    if (imageBytes.length <= MAX_PACKET_SIZE) {
                        DatagramPacket packet = new DatagramPacket(
                            imageBytes, imageBytes.length, targetAddress, targetPort
                        );
                        socket.send(packet);
                    } else {
                        System.err.println("Frame too large: " + imageBytes.length + " bytes (skipped)");
                    }
                } else {
                    System.err.println("VideoSender: Failed to capture image from webcam");
                }
                
                // Maintain FPS
                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = frameDelay - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                
            } catch (InterruptedException e) {
                System.out.println("VideoSender: Interrupted");
                break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("Error sending video: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("VideoSender stopped. Total frames sent: " + frameCount);
    }

    public void stop() {
        System.out.println("VideoSender.stop() called");
        running = false;
        
        if (webcam != null && webcam.isOpen()) {
            try {
                webcam.close();
                System.out.println("Webcam closed");
            } catch (Exception e) {
                System.err.println("Error closing webcam: " + e.getMessage());
            }
        }
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}