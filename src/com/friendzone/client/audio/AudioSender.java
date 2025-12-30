package com.friendzone.client.audio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Gửi audio từ microphone qua UDP
 */
public class AudioSender implements Runnable {
    private TargetDataLine microphone;
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;
    private boolean running = true;
    
    // Audio formats to try (fallback if one doesn't work)
    private static final AudioFormat[] FORMATS = {
        // 8kHz mono (best for voice over network)
        new AudioFormat(8000.0f, 16, 1, true, false),
        // 16kHz mono (better quality)
        new AudioFormat(16000.0f, 16, 1, true, false),
        // 44.1kHz mono (CD quality, fallback)
        new AudioFormat(44100.0f, 16, 1, true, false),
        // 8kHz stereo (last resort)
        new AudioFormat(8000.0f, 16, 2, true, false)
    };
    
    private AudioFormat usedFormat;
    
    public AudioSender(String targetIp, int targetPort) {
        try {
            this.targetAddress = InetAddress.getByName(targetIp);
            this.targetPort = targetPort;
            this.socket = new DatagramSocket();
            
            // Try each format until one works
            for (AudioFormat format : FORMATS) {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    try {
                        microphone = (TargetDataLine) AudioSystem.getLine(info);
                        microphone.open(format);
                        microphone.start();
                        usedFormat = format;
                        System.out.println("AudioSender started -> " + targetIp + ":" + targetPort + 
                                         " using format: " + format.getSampleRate() + "Hz");
                        return;
                    } catch (Exception e) {
                        // Try next format
                    }
                }
            }
            
            System.err.println("Không tìm thấy microphone format phù hợp!");
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo AudioSender: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        if (microphone == null) {
            System.err.println("Microphone không khả dụng");
            return;
        }
        
        // Buffer size cho ~50ms audio (8000 samples/sec * 2 bytes * 0.05 sec = 800 bytes)
        byte[] buffer = new byte[800];
        
        while (running) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    DatagramPacket packet = new DatagramPacket(
                        buffer, bytesRead, targetAddress, targetPort
                    );
                    socket.send(packet);
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("Lỗi gửi audio: " + e.getMessage());
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        if (microphone != null) {
            try {
                microphone.stop();
                microphone.flush(); // Clear buffer
                microphone.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("AudioSender stopped");
    }
}
