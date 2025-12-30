package com.friendzone.server;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {
    private static final int PORT = 12345;
    
    // Thread Pool: Fixed size to prevent overload
    private static final int MAX_THREADS = 100; // Max concurrent clients
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    
    // Map to store online clients: UserID -> ClientHandler
    public static final Map<Long, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public static void addClient(long userId, ClientHandler handler) {
        onlineClients.put(userId, handler);
    }

    public static void removeClient(long userId) {
        onlineClients.remove(userId);
    }

    public static void main(String[] args) {
        System.out.println("Starting Friendzone Server on port " + PORT + "...");
        System.out.println("Thread Pool: Fixed size = " + MAX_THREADS + " threads");
        
        // Start heartbeat checker thread
        startHeartbeatChecker();
        
        // Create SSL ServerSocket
        try (ServerSocket serverSocket = createSSLServerSocket(PORT)) {
            
            System.out.println("Server started! Waiting for connections...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());
                
                // Submit to thread pool (limited to MAX_THREADS)
                threadPool.submit(new ClientHandler(clientSocket));
                
                // Log active threads
                if (threadPool instanceof java.util.concurrent.ThreadPoolExecutor) {
                    java.util.concurrent.ThreadPoolExecutor tpe = (java.util.concurrent.ThreadPoolExecutor) threadPool;
                    System.out.println("📊 Active threads: " + tpe.getActiveCount() + "/" + MAX_THREADS);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Heartbeat checker: Pings all clients every 30s and removes dead ones
     */
    private static void startHeartbeatChecker() {
        Thread heartbeatThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    
                    System.out.println("🔍 Heartbeat check - Online clients: " + onlineClients.size());
                    
                    // Send PING to all clients
                    for (ClientHandler client : onlineClients.values()) {
                        client.sendPing();
                    }
                    
                    // Wait 5 seconds for PONG responses
                    Thread.sleep(5000);
                    
                    // Remove dead clients
                    java.util.List<Long> deadClients = new java.util.ArrayList<>();
                    for (Map.Entry<Long, ClientHandler> entry : onlineClients.entrySet()) {
                        if (!entry.getValue().isAlive()) {
                            deadClients.add(entry.getKey());
                            System.out.println("❌ Dead client detected: " + entry.getKey());
                        }
                    }
                    
                    // Remove and broadcast
                    for (Long userId : deadClients) {
                        removeClient(userId);
                    }
                    
                    if (!deadClients.isEmpty()) {
                        broadcastOnlineUsers();
                    }
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("HeartbeatChecker");
        heartbeatThread.start();
        System.out.println("✅ Heartbeat checker started");
    }
    
    /**
     * Create SSL ServerSocket with keystore
     */
    private static ServerSocket createSSLServerSocket(int port) throws Exception {
        // Load keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("server.keystore")) {
            keyStore.load(fis, "friendzone123".toCharArray());
        }
        
        // Initialize key manager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "friendzone123".toCharArray());
        
        // Initialize SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        
        // Create SSL server socket
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
        
        // Enable all cipher suites
        sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());
        
        System.out.println("🔒 SSL/TLS enabled on port " + port);
        return sslServerSocket;
    }
    
    public static void broadcastOnlineUsers() {
        java.util.List<java.util.Map<String, String>> users = new java.util.ArrayList<>();
        for (ClientHandler client : onlineClients.values()) {
            if (client.getCurrentUser() != null) {
                java.util.Map<String, String> u = new java.util.HashMap<>();
                u.put("id", String.valueOf(client.getCurrentUser().getId()));
                u.put("nickname", client.getCurrentUser().getNickname());
                users.add(u);
            }
        }
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String jsonUsers = gson.toJson(users);
        
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("users", jsonUsers);
        
        String msg = gson.toJson(new com.friendzone.model.NetworkMessage("ONLINE_USERS", data));
        
        for (ClientHandler client : onlineClients.values()) {
            client.sendJson(msg);
        }
    }
}
