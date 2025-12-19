package com.example.chatroom.user.websocket;

import com.example.chatroom.user.ChatApp;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.function.BiConsumer;

public class ChatWebSocketClient extends WebSocketClient {

    private BiConsumer<Integer, Boolean> statusCallback;
    public BiConsumer<JSONObject, Void> messageCallback;

    public ChatWebSocketClient(URI serverUri, BiConsumer<Integer, Boolean> statusCallback, BiConsumer<JSONObject, Void> messageCallback) {
        super(serverUri);
        this.statusCallback = statusCallback;
        this.messageCallback = messageCallback;
    }

    public void setStatusCallback(BiConsumer<Integer, Boolean> statusCallback) {
        this.statusCallback = statusCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket server");
    }

    @Override
    public void onMessage(String message) {
//        System.out.println("WS RAW -> " + message);
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "STATUS": {
                    int userId = json.getInt("userId");
                    boolean online = json.getBoolean("online");

                    if (online) ChatApp.onlineUsers.add(userId);
                    else ChatApp.onlineUsers.remove(userId);

                    Platform.runLater(() -> statusCallback.accept(userId, online));
                    break;
                }

                case "ONLINE_SNAPSHOT": {
                    JSONArray array = json.getJSONArray("users");

                    for (int i = 0; i < array.length(); i++) {
                        int id = array.getInt(i);
                        ChatApp.onlineUsers.add(id);
                        Platform.runLater(() -> statusCallback.accept(id, true));
                    }
                    break;
                }

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
