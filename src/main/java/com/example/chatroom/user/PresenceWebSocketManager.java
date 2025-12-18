package com.example.chatroom.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PresenceWebSocketManager {

    private static PresenceWebSocketManager instance;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Timer heartbeatTimer;
    private WebSocketClient client;

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<Set<String>>> listeners = ConcurrentHashMap.newKeySet();

    private PresenceWebSocketManager() {}

    public static PresenceWebSocketManager getInstance() {
        if (instance == null) instance = new PresenceWebSocketManager();
        return instance;
    }

    public void addListener(Consumer<Set<String>> listener) {
        listeners.add(listener);
        listener.accept(Set.copyOf(onlineUsers));
    }

    private void notifyListeners() {
        Set<String> snapshot = Set.copyOf(onlineUsers);
        listeners.forEach(l -> l.accept(snapshot));
    }

    public void connect(String serverIp) {
        if (ChatApp.currentUser == null) throw new IllegalStateException("No logged-in user found.");
        disconnect();

        try {
            String uri = "ws://" + serverIp + ":8080/presence";
            client = new WebSocketClient(new URI(uri)) {
                @Override
                public void onOpen(org.java_websocket.handshake.ServerHandshake handshakedata) {
                    System.out.println("Presence WS connected for " + ChatApp.currentUser.getUsername());
                    startHeartbeat();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode node = objectMapper.readTree(message);
                        String type = node.get("type").asText();

                        if ("online_users".equals(type)) {
                            onlineUsers.clear();
                            node.get("users").forEach(u -> onlineUsers.add(u.asText()));
                            Platform.runLater(PresenceWebSocketManager.this::notifyListeners);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
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

    private void startHeartbeat() {
        if (heartbeatTimer != null) return;
        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (client != null && client.isOpen()) {
                    try {
                        ObjectNode heartbeat = objectMapper.createObjectNode();
                        heartbeat.put("type", "heartbeat");
                        heartbeat.put("username", ChatApp.currentUser.getUsername());
                        client.send(heartbeat.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

    public void disconnect() {
        stopHeartbeat();
        if (client != null) {
            try { client.close(); } catch (Exception e) { e.printStackTrace(); }
            finally { client = null; }
        }
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }
}
