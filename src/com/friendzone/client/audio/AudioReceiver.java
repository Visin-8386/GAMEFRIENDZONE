package com.friendzone.client.audio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Nhận audio qua UDP và phát ra loa
 */
public class AudioReceiver implements Runnable {
    private DatagramSocket socket;
    private SourceDataLine speaker;
    private int port;
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
    
    public AudioReceiver(int port) {
        this.port = port;
        try {
            // Try binding to port with reuse option
            this.socket = new DatagramSocket(null);
            this.socket.setReuseAddress(true);
            this.socket.bind(new java.net.InetSocketAddress(port));
            
            // Try each format until one works
            for (AudioFormat format : FORMATS) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    try {
                        speaker = (SourceDataLine) AudioSystem.getLine(info);
                        speaker.open(format);
                        speaker.start();
                        usedFormat = format;
                        System.out.println("AudioReceiver listening on port " + port + 
                                         " using format: " + format.getSampleRate() + "Hz");
                        return;
                    } catch (Exception e) {
                        // Try next format
                    }
                }
            }
            
            System.err.println("Không tìm thấy audio format phù hợp!");
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo AudioReceiver: " + e.getMessage());
        }
    }
    
    public AudioFormat getUsedFormat() {
        return usedFormat;
    }
    
    @Override
    public void run() {
        if (speaker == null) {
            System.err.println("Speaker không khả dụng");
            return;
        }
        
        byte[] buffer = new byte[800]; // Khớp với AudioSender
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Phát audio nhận được ra loa
                speaker.write(packet.getData(), 0, packet.getLength());
            } catch (Exception e) {
                if (running) {
                    // Không in lỗi khi socket đóng bình thường
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        if (speaker != null) {
            try {
                // Drain buffer trước khi stop để tránh còn âm thanh rơ rớ
                speaker.drain();
                speaker.stop();
                speaker.flush();
                speaker.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("AudioReceiver stopped");
    }
}
