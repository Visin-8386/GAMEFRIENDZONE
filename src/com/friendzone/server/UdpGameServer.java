package com.friendzone.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UdpGameServer implements Runnable {
    private static final int PORT = 54321;
    private DatagramSocket socket;
    private boolean running = true;
    
    // Map<String, PlayerState> where key is "IP:Port"
    private Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private int heartY = 0;
    private int heartX = 300; // Fixed X for simple demo

    public UdpGameServer() {
        try {
            socket = new DatagramSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("UDP Game Server started on port " + PORT);
        
        // Start Game Loop Thread
        new Thread(this::gameLoop).start();
        
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String msg = new String(packet.getData(), 0, packet.getLength());
                processPacket(msg, packet.getAddress(), packet.getPort());
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void gameLoop() {
        while (running) {
            try {
                // Update Logic
                heartY += 5;
                if (heartY > 600) {
                    heartY = 0;
                    heartX = (int) (Math.random() * 600); // Randomize X
                }
                
                // Broadcast State
                broadcastState();
                
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void processPacket(String msg, InetAddress address, int port) {
        String key = address.getHostAddress() + ":" + port;
        
        // Register player if new
        players.putIfAbsent(key, new PlayerState(address, port));
        
        // MOVE_BASKET|x
        String[] parts = msg.split("\\|");
        if (parts[0].equals("MOVE_BASKET") && parts.length > 1) {
            try {
                int x = Integer.parseInt(parts[1]);
                players.get(key).x = x;
            } catch (NumberFormatException e) {}
        }
    }
    
    private void broadcastState() {
        // Simple JSON-like state: STATE|heartX|heartY|p1X|p2X...
        StringBuilder sb = new StringBuilder("STATE|" + heartX + "|" + heartY);
        
        for (PlayerState p : players.values()) {
            sb.append("|").append(p.x);
        }
        
        byte[] data = sb.toString().getBytes();
        
        for (PlayerState p : players.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, p.address, p.port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static class PlayerState {
        InetAddress address;
        int port;
        int x;
        
        public PlayerState(InetAddress address, int port) {
            this.address = address;
            this.port = port;
            this.x = 0;
        }
    }
}
