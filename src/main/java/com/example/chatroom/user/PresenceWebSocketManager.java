package com.example.chatroom.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class PresenceWebSocketManager {

    private static PresenceWebSocketManager instance;
    private WebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Timer heartbeatTimer;

    private PresenceWebSocketManager() { }

    public static PresenceWebSocketManager getInstance() {
        if (instance == null) instance = new PresenceWebSocketManager();
        return instance;
    }

    public void connect(String serverIp) {
        if (ChatApp.currentUser == null) throw new IllegalStateException("No logged-in user found.");

        disconnect(); // close previous session if any

        try {
            String uri = "ws://" + serverIp + ":8080/presence";
            client = new WebSocketClient(new URI(uri)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Presence WebSocket connected for " + ChatApp.currentUser.getUsername());
                    startHeartbeat();
                }

                @Override
                public void onMessage(String message) {
                    // Optional: handle server acks
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Presence WebSocket disconnected: " + reason);
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
            try { client.close(); } catch (Exception e) { e.printStackTrace(); }
            finally { client = null; }
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
                    heartbeat.put("username", ChatApp.currentUser.getUsername());
                    client.send(heartbeat.toString());
                    System.out.println("Presence heartbeat sent at " + System.currentTimeMillis());
                }
            }
        }, 0, 5000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }
}
