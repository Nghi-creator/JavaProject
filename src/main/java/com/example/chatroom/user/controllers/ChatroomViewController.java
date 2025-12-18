package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.*;
import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.core.dto.MessageDto;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class ChatroomViewController {

    @FXML private HeaderController headerController;
    @FXML private VBox chatListVBox;      // Left panel VBox
    @FXML private VBox messagesVBox;      // Center chat messages VBox
    @FXML private Text chatHeading;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    private Integer currentUserId;         // Set after login
    private ConversationDto selectedConversation;

    @FXML
    private void initialize() {
        headerController.focusButton("chat");
    }

    /**
     * Fetch conversations from the server and populate the chat list
     */
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

    /**
     * Populate the left chat list panel dynamically
     */
    private void populateChatList(String json) {
        List<ConversationDto> conversations = parseConversations(json);

        Platform.runLater(() -> {
            chatListVBox.getChildren().clear();

            for (ConversationDto convo : conversations) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                    Node cardNode = loader.load();
                    NameCardController controller = loader.getController();

                    // Determine display name
                    String displayName = convo.getName();
                    if ((displayName == null || displayName.isBlank())
                            && Objects.equals(convo.getType(), "PRIVATE")
                            && convo.getMembers() != null) {
                        // Pick the other member's name
                        for (var member : convo.getMembers()) {
                            if (!member.getId().equals(currentUserId)) {
                                displayName = member.getFullName() != null ? member.getFullName() : member.getUsername();
                                break;
                            }
                        }
                    }
                    if (displayName == null || displayName.isBlank()) {
                        displayName = "DM";
                    }

                    controller.setData(displayName, null);

                    String finalDisplayName = displayName;
                    cardNode.setOnMouseClicked(event -> selectConversation(convo, finalDisplayName));

                    chatListVBox.getChildren().add(cardNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Handle conversation selection
     */
    private void selectConversation(ConversationDto convo, String heading) {
        this.selectedConversation = convo;
        chatHeading.setText(heading);
        loadMessages(convo.getId());
    }

    /**
     * Fetch messages for the selected conversation
     */
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

    /**
     * Populate the center messages area
     */
    private void populateMessages(String json) {
        List<MessageDto> messages = parseMessages(json);

        Platform.runLater(() -> {
            messagesVBox.getChildren().clear();

            for (MessageDto msg : messages) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/Message.fxml"));
                    Node msgNode = loader.load();
                    MessageController controller = loader.getController();
                    controller.setTitle(msg.getSenderName() != null ? msg.getSenderName() : msg.getSenderId().toString());
                    controller.setContent(msg.getContent());
                    controller.setTimeStamp(msg.getSentAt().format(formatter));

                    messagesVBox.getChildren().add(msgNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * JSON parsing helpers
     */
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

    /**
     * Opens group info popup
     */
    public void displayGroupInfo(MouseEvent mouseEvent) {
        SceneSwitcher.openPopup("/user/ui/fxml/GroupInfoView.fxml", "User Info");
    }

    /**
     * Set the current logged-in user ID (call after login)
     */
    public void setCurrentUserId(Integer userId) {
        this.currentUserId = userId;
        loadUserConversations();
    }
}
