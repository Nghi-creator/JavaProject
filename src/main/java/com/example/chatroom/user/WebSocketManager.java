package com.example.chatroom.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketManager {

    private static WebSocketManager instance;
    private WebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Timer heartbeatTimer;

    private WebSocketManager() { }

    public static WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }

    /**
     * Connects to the server using the already-logged-in user.
     */
    public void connect(String uri) {
        if (ChatApp.currentUser == null) {
            throw new IllegalStateException("No logged-in user found.");
        }

        try {
            client = new WebSocketClient(new URI(uri)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Connected to server: " + ChatApp.currentUser.getUsername());

                    // Immediately start heartbeat
                    startHeartbeat();
                }

                @Override
                public void onMessage(String message) {
                    // Handle server responses if needed
                    System.out.println("Received from server: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected: " + reason);
                    stopHeartbeat();
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        stopHeartbeat();
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                client = null;
            }
        }
    }


    private void startHeartbeat() {
        if (heartbeatTimer != null) return;

        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (client != null && client.isOpen()) {
                    ObjectNode heartbeat = objectMapper.createObjectNode();
                    heartbeat.put("type", "heartbeat");
                    heartbeat.put("username", ChatApp.currentUser.getUsername()); // send username so server knows who
                    client.send(heartbeat.toString());
                    System.out.println("Heartbeat sent at " + System.currentTimeMillis());
                }
            }
        }, 0, 5000); // every 5 seconds
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    public void sendMessage(String message) {
        if (client != null && client.isOpen()) {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "chat");
            json.put("username", ChatApp.currentUser.getUsername());
            json.put("message", message);
            client.send(json.toString());
        }
    }
}
