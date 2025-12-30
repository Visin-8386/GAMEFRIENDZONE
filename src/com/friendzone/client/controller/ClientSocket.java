package com.friendzone.client.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class ClientSocket {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private com.google.gson.Gson gson = new com.google.gson.Gson();
    private Consumer<com.friendzone.model.NetworkMessage> onMessageReceived;

    public void connect(String ip, int port) throws IOException {
        try {
            // Create SSL socket with truststore
            socket = createSSLSocket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("🔒 SSL connection established");
            
            // Start listening thread
            Thread listenerThread = new Thread(this::listen);
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (Exception e) {
            throw new IOException("Failed to connect with SSL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create SSL socket using client truststore
     */
    private Socket createSSLSocket(String ip, int port) throws Exception {
        // Load truststore with server certificate
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("client.truststore")) {
            trustStore.load(fis, "friendzone123".toCharArray());
        }
        
        // Initialize trust manager
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        // Initialize SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        
        // Create SSL socket
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);
        
        // Enable all cipher suites
        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
        
        return sslSocket;
    }

    public void send(String command, java.util.Map<String, String> data) {
        if (out != null) {
            com.friendzone.model.NetworkMessage msg = new com.friendzone.model.NetworkMessage(command, data);
            out.println(gson.toJson(msg));
        }
    }

    public void setMessageListener(Consumer<com.friendzone.model.NetworkMessage> listener) {
        this.onMessageReceived = listener;
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (onMessageReceived != null) {
                    try {
                        com.friendzone.model.NetworkMessage msg = gson.fromJson(line, com.friendzone.model.NetworkMessage.class);
                        
                        // Handle PING/PONG automatically (heartbeat)
                        if ("PING".equals(msg.getCommand())) {
                            // Respond with PONG immediately
                            send("PONG", new java.util.HashMap<>());
                            continue; // Don't pass to UI
                        }
                        
                        onMessageReceived.accept(msg);
                    } catch (Exception e) {
                        System.err.println("Error parsing JSON: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }
}
