package com.example.chatroom.user.websocket;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.function.BiConsumer;

public class ChatWebSocketClient extends WebSocketClient {

    private BiConsumer<Long, Boolean> statusCallback;
    public BiConsumer<JSONObject, Void> messageCallback;

    public ChatWebSocketClient(URI serverUri, BiConsumer<Long, Boolean> statusCallback, BiConsumer<JSONObject, Void> messageCallback) {
        super(serverUri);
        this.statusCallback = statusCallback;
        this.messageCallback = messageCallback;
    }

    public void setStatusCallback(BiConsumer<Long, Boolean> statusCallback) {
        this.statusCallback = statusCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket server");
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "STATUS":
                    long userId = json.getLong("userId");
                    boolean online = json.getBoolean("online");
                    Platform.runLater(() -> statusCallback.accept(userId, online));
                    break;

                case "ONLINE_SNAPSHOT":
                    JSONArray array = json.getJSONArray("users");
                    for (int i = 0; i < array.length(); i++) {
                        long onlineUserId = array.getLong(i);
                        Platform.runLater(() -> statusCallback.accept(onlineUserId, true));
                    }
                    break;

                case "MESSAGE":
                    Platform.runLater(() -> messageCallback.accept(json, null));
                    break;

                default:
                    System.out.println("Unknown WebSocket message type: " + type);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket disconnected: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void sendMessage(long conversationId, String content, String senderName) {
        if (this.isOpen()) {
            JSONObject json = new JSONObject();
            json.put("type", "MESSAGE");
            json.put("conversationId", conversationId);
            json.put("content", content);
            json.put("senderName", senderName);
            send(json.toString());
        }
    }
}
