package com.example.chatroom.user;

import com.example.chatroom.core.dto.MessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class WebSocketManager {

    private static WebSocketManager instance;
    private WebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Current conversation context
    private Integer conversationId;
    private VBox messagesVBox;
    private Consumer<MessageDto> onMessageReceived;

    private WebSocketManager() { }

    public static WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }

    /**
     * Connects to a specific conversation
     */
    public void connect(String serverIp, int conversationId, VBox messagesVBox, Consumer<MessageDto> onMessageReceived) {
        if (ChatApp.currentUser == null) throw new IllegalStateException("No logged-in user found.");

        disconnect(); // close previous session if any

        this.conversationId = conversationId;
        this.messagesVBox = messagesVBox;
        this.onMessageReceived = onMessageReceived;

        try {
            String uri = "ws://" + serverIp + ":8080/chat/" + conversationId;
            client = new WebSocketClient(new URI(uri)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Connected to conversation " + conversationId);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        // Assume server sends full MessageDto JSON
                        MessageDto msg = objectMapper.readValue(message, MessageDto.class);

                        // Dispatch to JavaFX UI thread
                        Platform.runLater(() -> {
                            if (messagesVBox != null) {
                                onMessageReceived.accept(msg); // call controller callback
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected: " + reason);
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
        if (client != null) {
            try { client.close(); } catch (Exception e) { e.printStackTrace(); }
            finally { client = null; }
        }
    }

    /**
     * Sends a chat message to the current conversation
     */
    public void sendMessage(String message) {
        if (client != null && client.isOpen()) {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "chat");
            json.put("username", ChatApp.currentUser.getUsername());
            json.put("userId", ChatApp.currentUser.getId());
            json.put("message", message);
            client.send(json.toString());
        }
    }
}
