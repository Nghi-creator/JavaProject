package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.*;
import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.core.dto.MessageDto;
import com.example.chatroom.user.ChatApp;
import com.example.chatroom.user.websocket.ChatWebSocketClient;
import com.example.chatroom.core.utils.AESUtil; // Import AESUtil for encryption
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.json.JSONObject;

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
import java.util.ArrayList;

public class ChatroomViewController {

    @FXML private HeaderController headerController;
    @FXML private VBox chatListVBox;
    @FXML private VBox messagesVBox;
    @FXML private Text chatHeading;
    @FXML private TextField messageInput;
    @FXML private VBox memberListVBox;
    @FXML private MenuButton optionsButton;
    @FXML private Text memberCount;

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

    @FXML private TextField chatSearchInput;
    @FXML private ScrollPane messagesScrollPane;

    private List<Node> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;

    @FXML private TextField globalSearchInput;
    private List<MessageDto> globalSearchResults = new ArrayList<>();
    private int currentGlobalSearchIndex = -1;

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
        this.webSocketClient.messageCallback = (json, v) -> handleIncomingMessage(json);

//        if (!this.webSocketClient.isOpen()) {
//            this.webSocketClient.connect();
//        }
    }

    private void handleIncomingMessage(JSONObject json) {
        long convId = json.getLong("conversationId");
        if (selectedConversation != null && selectedConversation.getId() == convId) {
            MessageDto msg = new MessageDto();
            msg.setSenderId(json.getInt("senderId"));
            msg.setSenderName(json.getString("senderName"));

            // --- DECRYPT INCOMING LIVE MESSAGE ---
            String rawContent = json.getString("content");
            String displayContent = rawContent;

            if (Boolean.TRUE.equals(selectedConversation.getIsEncrypted()) && selectedConversation.getSecretKey() != null) {
                displayContent = AESUtil.decrypt(rawContent, selectedConversation.getSecretKey());
            }
            msg.setContent(displayContent);
            // -------------------------------------

            msg.setSentAt(java.time.LocalDateTime.now());
            addMessageToVBox(msg);
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
                    cardNode.setUserData(convo);
                    NameCardController controller = loader.getController();

                    controller.setConversationContext(convo);

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

                    // --- VISUAL INDICATOR FOR ENCRYPTED CHATS ---
                    if (Boolean.TRUE.equals(convo.getIsEncrypted())) {
                        displayName = "🔒 " + displayName;
                    }
                    // --------------------------------------------

                    controller.setData(displayName, null);

                    if ("PRIVATE".equals(convo.getType())) {
                        for (var member : convo.getMembers()) {
                            if (!member.getId().equals(currentUserId)) {
                                chatListCards.put(member.getId(), controller);

//                                Boolean pending = pendingStatusUpdates.remove(member.getId());
//                                if (pending != null) controller.setStatus(pending ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);

                                boolean isOnline = ChatApp.onlineUsers.contains(member.getId());
                                System.out.println(isOnline);
                                controller.setStatus(
                                        isOnline
                                                ? StatusIconController.Status.ONLINE
                                                : StatusIconController.Status.OFFLINE
                                );

                            }
                        }
                    } else controller.setStatus(StatusIconController.Status.DISABLED);

                    String finalDisplayName = displayName;
                    cardNode.setOnMouseClicked(event -> selectConversation(convo, finalDisplayName, null));

                    chatListVBox.getChildren().add(cardNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void selectConversation(ConversationDto convo, String heading, Runnable afterLoad) {
        this.selectedConversation = convo;
        chatHeading.setText(heading);

        optionsButton.getItems().clear();
        if ("PRIVATE".equals(convo.getType()) || isCurrentUserAdmin(convo)) {
            MenuItem deleteItem = new MenuItem("Delete Conversation");
            deleteItem.setOnAction(event -> deleteConversation(convo));
            optionsButton.getItems().add(deleteItem);
        }

        chatHeading.setOnMouseClicked(event -> {
            if ("PRIVATE".equals(convo.getType())) {
                SceneSwitcher.openPopup("/user/ui/fxml/UserInfoView.fxml", heading);
            } else {
                SceneSwitcher.openPopup("/user/ui/fxml/GroupInfoView.fxml", heading);
            }
        });

        memberCount.setText(String.format("MEMBERS (%d)", convo.getMembers().size()));
        populateMemberList(convo.getMembers());

        // Only load messages if no callback provided (normal case) OR force load if callback exists
        loadMessages(convo.getId(), afterLoad);
    }

    private void loadMessages(int conversationId, Runnable afterLoad) {
        if (currentUserId == null) return;

        String serverIp = ConfigController.getServerIp();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages/" + conversationId + "?userId=" + currentUserId))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> {
                    populateMessages(json);
                    if (afterLoad != null) afterLoad.run();
                })
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private void populateMessages(String json) {
        List<MessageDto> messages = parseMessages(json);

        Platform.runLater(() -> {
            messagesVBox.getChildren().clear();

            for (MessageDto msg : messages) {
                // --- DECRYPT HISTORY MESSAGES ---
                if (selectedConversation != null &&
                        Boolean.TRUE.equals(selectedConversation.getIsEncrypted()) &&
                        selectedConversation.getSecretKey() != null) {

                    String decrypted = AESUtil.decrypt(msg.getContent(), selectedConversation.getSecretKey());
                    msg.setContent(decrypted);
                }
                // --------------------------------
                addMessageToVBox(msg);
            }

            scrollToBottom();
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
//            if (members == null) return;
            for (var member : members) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                    Node memberNode = loader.load();
                    NameCardController controller = loader.getController();
                    String displayName = member.getFullName() != null ? member.getFullName() : member.getUsername();
                    controller.setData(displayName, null);
                    memberListCards.put(member.getId(), controller);

//                    Boolean pending = pendingStatusUpdates.remove(member.getId());
//                    if (pending != null) {
//                        controller.setStatus(pending ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE);
//                    } else {
//                        controller.setStatus(StatusIconController.Status.OFFLINE);
//                    }

                    boolean isOnline = ChatApp.onlineUsers.contains(member.getId());
                    controller.setStatus(
                            isOnline
                                    ? StatusIconController.Status.ONLINE
                                    : StatusIconController.Status.OFFLINE
                    );


                    memberNode.setOnMouseClicked(event -> System.out.println("Clicked on member: " + displayName));

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

        String rawContent = messageInput.getText();
        messageInput.clear();

        // --- ENCRYPT BEFORE SENDING ---
        String contentToSend = rawContent;
        if (Boolean.TRUE.equals(selectedConversation.getIsEncrypted()) && selectedConversation.getSecretKey() != null) {
            contentToSend = AESUtil.encrypt(rawContent, selectedConversation.getSecretKey());
        }
        // ------------------------------

        // 1. Save to DB via HTTP (Send Encrypted Content)
        String encodedContent = URLEncoder.encode(contentToSend, StandardCharsets.UTF_8);
        String serverIp = ConfigController.getServerIp();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages" +
                        "?conversationId=" + selectedConversation.getId() +
                        "&senderId=" + currentUserId +
                        "&content=" + encodedContent))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(e -> { e.printStackTrace(); return null; });

        // 2. Send via WebSocket for live updates (Send Encrypted Content)
        webSocketClient.sendMessage(
                selectedConversation.getId(),
                contentToSend,
                getCurrentUserDisplayName()
        );
    }

    private String getCurrentUserDisplayName() {
        // Implement your logic to get current user display name
        return "You"; // placeholder
    }

    private void addMessageToVBox(MessageDto msg) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/Message.fxml"));
            Node msgNode = loader.load();
            msgNode.setUserData(msg);
            MessageController controller = loader.getController();
            controller.setTitle(msg.getSenderName() != null ? msg.getSenderName() : msg.getSenderId().toString());
            controller.setContent(msg.getContent());
            controller.setTimeStamp(msg.getSentAt().format(formatter));
            controller.setStatus(StatusIconController.Status.DISABLED);

            // --- KEEP FRIEND'S DELETE LOGIC ---
            controller.setMessage(msg, () -> deleteMessage(msg));
            // ----------------------------------

            messagesVBox.getChildren().add(msgNode);
            scrollToBottom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserStatus(int userId, boolean online) {
//        System.out.println("UI UPDATE -> user=" + userId + " online=" + online);

        if (online) ChatApp.onlineUsers.add(userId);
        else ChatApp.onlineUsers.remove(userId);

        Platform.runLater(() -> {
            NameCardController chatCard = chatListCards.get((int) userId);
            if (chatCard != null) {
                chatCard.setStatus(
                        online ? StatusIconController.Status.ONLINE
                                : StatusIconController.Status.OFFLINE
                );
            }

            NameCardController memberCard = memberListCards.get((int) userId);
            if (memberCard != null) {
                memberCard.setStatus(
                        online ? StatusIconController.Status.ONLINE
                                : StatusIconController.Status.OFFLINE
                );
            }
        });
    }

    public void displayGroupInfo(MouseEvent mouseEvent) {
        SceneSwitcher.openPopup("/user/ui/fxml/GroupInfoView.fxml", "User Info");
    }

    private boolean isCurrentUserAdmin(ConversationDto convo) {
        if (!"GROUP".equals(convo.getType()) || convo.getMembers() == null) return false;
        return convo.getMembers().stream()
                .anyMatch(member -> member.getId().equals(currentUserId) && "ADMIN".equals(member.getRole()));
    }

    private void deleteConversation(ConversationDto convo) {
        if (currentUserId == null || convo == null) return;

        String serverIp = ConfigController.getServerIp();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/conversations/"
                        + convo.getId() + "?userId=" + currentUserId))
                .DELETE()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        chatListVBox.getChildren().removeIf(node -> {
                            Object userData = node.getUserData();
                            return userData instanceof ConversationDto && ((ConversationDto) userData).getId().equals(convo.getId());
                        });
                        if (selectedConversation != null && selectedConversation.getId().equals(convo.getId())) {
                            messagesVBox.getChildren().clear();
                            chatHeading.setText("");
                            selectedConversation = null;
                        }
                    });
                })
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    @FXML
    private void searchChatHistory() {
        String query = chatSearchInput.getText().trim().toLowerCase();

        // Clear previous highlights if the query is empty
        if (query.isEmpty()) {
            messagesVBox.getChildren().forEach(node -> node.setStyle(""));
            searchResults.clear();
            currentSearchIndex = -1;
            return;
        }

        searchResults.clear();
        currentSearchIndex = -1;

        for (Node node : messagesVBox.getChildren()) {
            if (node.getUserData() instanceof MessageDto msg) {
                if (msg.getContent().toLowerCase().contains(query)) {
                    searchResults.add(node);
                }
            }
        }

        if (!searchResults.isEmpty()) {
            currentSearchIndex = 0;
            scrollToMessage(searchResults.get(currentSearchIndex));
            highlightMessage(searchResults.get(currentSearchIndex));
        }
    }


    @FXML
    private void nextSearchResult() {
        if (searchResults.isEmpty()) return;
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
        scrollToMessage(searchResults.get(currentSearchIndex));
        highlightMessage(searchResults.get(currentSearchIndex));
    }

    @FXML
    private void prevSearchResult() {
        if (searchResults.isEmpty()) return;
        currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
        scrollToMessage(searchResults.get(currentSearchIndex));
        highlightMessage(searchResults.get(currentSearchIndex));
    }

    private void scrollToMessage(Node node) {
        Platform.runLater(() -> {
            double height = messagesVBox.getHeight();
            double y = node.getBoundsInParent().getMinY();
            double vValue = y / height;
            messagesScrollPane.setVvalue(vValue);
        });
    }

    private void highlightMessage(Node node) {
        messagesVBox.getChildren().forEach(n -> n.setStyle("")); // clear previous highlight
        node.setStyle("-fx-background-color: rgba(255, 255, 0, 0.3); -fx-background-radius: 5;");
    }

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    @FXML
    private void searchAllChats() {
        String query = globalSearchInput.getText().trim();
        if (query.isEmpty()) {
            globalSearchResults.clear();
            currentGlobalSearchIndex = -1;
            return;
        }

        String serverIp = ConfigController.getServerIp();
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages/search/all?userId="
                        + currentUserId + "&query=" + encodedQuery))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleGlobalSearchResults)
                .exceptionally(e -> { e.printStackTrace(); return null; });
    }

    private void handleGlobalSearchResults(String json) {
        try {
            List<MessageDto> results = objectMapper.readValue(json, new TypeReference<List<MessageDto>>() {});
            Platform.runLater(() -> {
                globalSearchResults = results;
                if (!results.isEmpty()) {
                    currentGlobalSearchIndex = 0;
                    jumpToMessage(results.get(0));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void jumpToMessage(MessageDto msg) {
        if (selectedConversation == null || !selectedConversation.getId().equals(msg.getConversationId())) {
            // Find conversation node and simulate click
            for (Node node : chatListVBox.getChildren()) {
                if (node.getUserData() instanceof ConversationDto convo && convo.getId().equals(msg.getConversationId())) {
                    // Pass a callback to highlight after messages load
                    selectConversation(convo, convo.getName(), () -> Platform.runLater(() -> scrollAndHighlight(msg)));
                    return;
                }
            }
        } else {
            Platform.runLater(() -> scrollAndHighlight(msg));
        }
    }


    private void scrollAndHighlight(MessageDto msg) {
        for (Node node : messagesVBox.getChildren()) {
            if (node.getUserData() instanceof MessageDto m && m.getId().equals(msg.getId())) {
                highlightMessage(node);
                scrollToMessage(node);
                break;
            }
        }
    }

    @FXML
    private void nextGlobalSearch() {
        if (globalSearchResults.isEmpty()) return;
        currentGlobalSearchIndex = (currentGlobalSearchIndex + 1) % globalSearchResults.size();
        jumpToMessage(globalSearchResults.get(currentGlobalSearchIndex));
    }

    @FXML
    private void prevGlobalSearch() {
        if (globalSearchResults.isEmpty()) return;
        currentGlobalSearchIndex = (currentGlobalSearchIndex - 1 + globalSearchResults.size()) % globalSearchResults.size();
        jumpToMessage(globalSearchResults.get(currentGlobalSearchIndex));
    }

    private void deleteMessage(MessageDto msg) {
        // Basic sanity check
        if (msg == null || currentUserId == null) return;

        // Optional: frontend permission check
        if (!msg.getSenderId().equals(currentUserId)) {
            System.out.println("Not allowed to delete this message");
            return;
        }

        String serverIp = ConfigController.getServerIp();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/messages/"
                        + msg.getId() + "?userId=" + currentUserId))
                .DELETE()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenRun(() -> Platform.runLater(() -> {
                    messagesVBox.getChildren().removeIf(node ->
                            node.getUserData() instanceof MessageDto m &&
                                    m.getId().equals(msg.getId())
                    );
                }))
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private String getLatestMessageText() {
        for (int i = messagesVBox.getChildren().size() - 1; i >= 0; i--) {
            Node node = messagesVBox.getChildren().get(i);
            if (node.getUserData() instanceof MessageDto msg) {
                String content = msg.getContent();
                if (content != null && !content.isBlank()) {
                    return content;
                }
            }
        }
        return null;
    }

    @FXML
    private void suggestMessage() {
        String lastMessage = getLatestMessageText();
        if (lastMessage == null || lastMessage.isBlank()) {
            return;
        }

        String serverIp = ConfigController.getServerIp();

        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("lastMessage", lastMessage)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/ai/suggest"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> {
                        try {
                            JSONObject obj = new JSONObject(json);
                            String suggestion = obj.optString("suggestion", "");

                            if (!suggestion.isBlank()) {
                                Platform.runLater(() ->
                                        messageInput.setText(suggestion)
                                );
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}