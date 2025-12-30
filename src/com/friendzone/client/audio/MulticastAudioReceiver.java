package com.friendzone.client.audio;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Nhận audio từ multicast group và phát ra loa
 * Tự động nhận từ BẤT KỲ sender nào trong group
 */
public class MulticastAudioReceiver {
    
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private SourceDataLine speakers;
    private volatile boolean running = false;
    private Thread receiverThread;
    
    // Phải giống với sender
    private static final String MULTICAST_GROUP = "230.0.0.1";
    
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        8000.0F,
        16,
        1,
        2,
        8000.0F,
        false
    );
    
    public MulticastAudioReceiver(int port) {
        this.port = port;
    }
    
    public void start() {
        try {
            // Tạo multicast socket và join group
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);  // ← Điểm khác biệt: JOIN GROUP để nhận
            
            // Mở speakers
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(AUDIO_FORMAT);
            speakers.start();
            
            running = true;
            receiverThread = new Thread(this::receiveAudio);
            receiverThread.setDaemon(true);
            receiverThread.start();
            
            System.out.println("Multicast Audio Receiver joined: " + MULTICAST_GROUP + ":" + port);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void receiveAudio() {
        byte[] buffer = new byte[1024];
        
        while (running) {
            try {
                // Nhận từ multicast group
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);  // Blocking call
                
                // Phát ra loa
                speakers.write(packet.getData(), 0, packet.getLength());
                
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.leaveGroup(group);  // Leave group khi thoát
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (speakers != null) {
            speakers.drain(); // Wait for buffer to play out
            speakers.stop();
            speakers.flush(); // Clear any remaining data
            speakers.close();
        }
        
        System.out.println("Multicast Audio Receiver stopped");
    }
    
    public boolean isRunning() {
        return running;
    }
}
