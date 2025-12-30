package com.friendzone.client.audio;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Gửi audio tới nhiều receivers cùng lúc qua Multicast
 * Tiết kiệm bandwidth hơn so với gửi riêng lẻ (unicast)
 */
public class MulticastAudioSender {
    
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private TargetDataLine microphone;
    private volatile boolean running = false;
    private Thread senderThread;
    
    // Multicast group address (Class D: 224.0.0.0 - 239.255.255.255)
    private static final String MULTICAST_GROUP = "230.0.0.1";
    
    // Audio format: 8kHz, 16-bit, mono (phù hợp cho voice call)
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        8000.0F,  // Sample rate
        16,       // Bits per sample
        1,        // Mono
        2,        // Frame size
        8000.0F,  // Frame rate
        false     // Little endian
    );
    
    public MulticastAudioSender(int port) {
        this.port = port;
    }
    
    public void start() {
        try {
            // Tạo multicast socket
            socket = new MulticastSocket();
            group = InetAddress.getByName(MULTICAST_GROUP);
            
            // Mở microphone
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(AUDIO_FORMAT);
            microphone.start();
            
            running = true;
            senderThread = new Thread(this::sendAudio);
            senderThread.setDaemon(true);
            senderThread.start();
            
            System.out.println("Multicast Audio Sender started: " + MULTICAST_GROUP + ":" + port);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendAudio() {
        byte[] buffer = new byte[1024];
        
        while (running) {
            try {
                // Đọc từ microphone
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // Gửi tới multicast group (TẤT CẢ members nhận được)
                    DatagramPacket packet = new DatagramPacket(
                        buffer, bytesRead, group, port
                    );
                    socket.send(packet);
                }
                
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        
        if (microphone != null) {
            microphone.stop();
            microphone.flush(); // Clear buffer
            microphone.close();
        }
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        System.out.println("Multicast Audio Sender stopped");
    }
    
    public boolean isRunning() {
        return running;
    }
}
