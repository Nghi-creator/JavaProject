package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.*;
import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.core.dto.MessageDto;
import com.example.chatroom.user.websocket.ChatWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ChatroomViewController {

    @FXML private HeaderController headerController;
    @FXML private VBox chatListVBox;
    @FXML private VBox messagesVBox;
    @FXML private Text chatHeading;
    @FXML private TextField messageInput;
    @FXML private VBox memberListVBox;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Integer currentUserId;
    private ChatWebSocketClient webSocketClient;
    private ConversationDto selectedConversation;

    private final Map<Integer, NameCardController> chatListCards = new ConcurrentHashMap<>();
    private final Map<Integer, NameCardController> memberListCards = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> pendingStatusUpdates = new ConcurrentHashMap<>();

    @FXML
    private void initialize() {
        headerController.focusButton("chat");
    }

    public void setCurrentUserId(Integer userId) {
        this.currentUserId = userId;
        loadUserConversations();
    }

    public void setWebSocketClient(ChatWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        this.webSocketClient.setStatusCallback(this::updateUserStatus);
        if (!this.webSocketClient.isOpen()) {
            this.webSocketClient.connect();
        }
    }

    private void loadUserConversations() {
        if (currentUserId == null) return;

        String serverIp = ConfigController.getServerIp();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/conversations?userId=" + currentUserId))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::populateChatList)
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private void populateChatList(String json) {
        List<ConversationDto> conversations = parseConversations(json);

        Platform.runLater(() -> {
            chatListVBox.getChildren().clear();

            for (ConversationDto convo : conversations) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                    Node cardNode = loader.load();
                    NameCardController controller = loader.getController();

                    String displayName = convo.getName();
                    if ((displayName == null || displayName.isBlank()) &&
                            Objects.equals(convo.getType(), "PRIVATE") &&
                            convo.getMembers() != null) {
                        for (var member : convo.getMembers()) {
                            if (!member.getId().equals(currentUserId)) {
                                displayName = member.getFullName() != null ? member.getFullName() : member.getUsername();
                                break;
                            }
                        }
                    }
                    if (displayName == null || displayName.isBlank()) displayName = "DM";

                    controller.setData(displayName, null);

                    if ("PRIVATE".equals(convo.getType())) {
                        for (var member : convo.getMembers()) {
                            if (!member.getId().equals(currentUserId)) {
                                System.out.println("Adding chat card for user: " + member.getId());
                                chatListCards.put(member.getId(), controller);

                                // Apply any pending status update
                                Boolean pending = pendingStatusUpdates.remove(member.getId().longValue());
                                if (pending != null) controller.setStatus(pending ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);
                            }
                        }
                    } else controller.setStatus(StatusIconController.Status.DISABLED);

                    String finalDisplayName = displayName;
                    cardNode.setOnMouseClicked(event -> selectConversation(convo, finalDisplayName));

                    chatListVBox.getChildren().add(cardNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void selectConversation(ConversationDto convo, String heading) {
        this.selectedConversation = convo;
        chatHeading.setText(heading);
        populateMemberList(convo.getMembers());
        loadMessages(convo.getId());
    }

    private void loadMessages(int conversationId) {
        if (currentUserId == null) return;

        String serverIp = ConfigController.getServerIp();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages/" + conversationId + "?userId=" + currentUserId))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::populateMessages)
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private void populateMessages(String json) {
        List<MessageDto> messages = parseMessages(json);

        Platform.runLater(() -> {
            messagesVBox.getChildren().clear();

            for (MessageDto msg : messages) addMessageToVBox(msg);
        });
    }

    private List<ConversationDto> parseConversations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConversationDto>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<MessageDto> parseMessages(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<MessageDto>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void populateMemberList(List<ConversationDto.MemberDto> members) {
        Platform.runLater(() -> {
            memberListVBox.getChildren().clear();
            for (var member : members) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                    Node memberNode = loader.load();
                    NameCardController controller = loader.getController();
                    String displayName = member.getFullName() != null ? member.getFullName() : member.getUsername();
                    controller.setData(displayName, null);
                    memberListCards.put(member.getId(), controller);

                    // Apply pending status if any
                    Boolean pending = pendingStatusUpdates.remove(member.getId().longValue());
                    if (pending != null) controller.setStatus(pending ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);

                    memberNode.setOnMouseClicked(event -> {
                        System.out.println("Clicked on member: " + displayName);
                    });

                    memberListVBox.getChildren().add(memberNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void sendMessage() {
        if (messageInput.getText().isBlank() || selectedConversation == null) return;

        String content = messageInput.getText();
        messageInput.clear();
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String serverIp = ConfigController.getServerIp();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages" +
                        "?conversationId=" + selectedConversation.getId() +
                        "&senderId=" + currentUserId +
                        "&content=" + encodedContent))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenRun(() -> loadMessages(selectedConversation.getId()))
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private void addMessageToVBox(MessageDto msg) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/Message.fxml"));
            Node msgNode = loader.load();
            MessageController controller = loader.getController();
            controller.setTitle(msg.getSenderName() != null ? msg.getSenderName() : msg.getSenderId().toString());
            controller.setContent(msg.getContent());
            controller.setTimeStamp(msg.getSentAt().format(formatter));
            messagesVBox.getChildren().add(msgNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserStatus(long userId, boolean online) {
        Platform.runLater(() -> {
            NameCardController chatCard = chatListCards.get((int) userId);
            if (chatCard != null) chatCard.setStatus(online ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);

            NameCardController memberCard = memberListCards.get((int) userId);
            if (memberCard != null) memberCard.setStatus(online ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);

            // Save for later if card does not exist yet
            if (chatCard == null && memberCard == null) pendingStatusUpdates.put(userId, online);
        });
    }

    public void displayGroupInfo(MouseEvent mouseEvent) {
        SceneSwitcher.openPopup("/user/ui/fxml/GroupInfoView.fxml", "User Info");
    }
}
