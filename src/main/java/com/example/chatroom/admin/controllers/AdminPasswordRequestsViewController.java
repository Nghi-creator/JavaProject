package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminPasswordRequestsViewController {

    @FXML private TableView<PasswordRequest> requestTable;
    @FXML private TableColumn<PasswordRequest, String> colUsername;
    @FXML private TableColumn<PasswordRequest, String> colNewPassword; // <--- NEW COLUMN
    @FXML private TableColumn<PasswordRequest, String> colDate;
    @FXML private TableColumn<PasswordRequest, Void> colAction;

    private ObservableList<PasswordRequest> requests = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colNewPassword.setCellValueFactory(new PropertyValueFactory<>("newPassword")); // <--- NEW
        colDate.setCellValueFactory(new PropertyValueFactory<>("requestDate"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnReset = new Button("Approve");
            {
                btnReset.getStyleClass().add("admin-action-button");
                btnReset.setOnAction(event -> {
                    PasswordRequest req = getTableView().getItems().get(getIndex());
                    handleApproveAction(req);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnReset);
            }
        });

        requestTable.setItems(requests);
        fetchRequests(); // <--- Fetch real data
    }

    private void fetchRequests() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/password-requests"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            requests.clear();
                            JSONArray arr = new JSONArray(response.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                requests.add(new PasswordRequest(
                                        obj.getInt("id"),
                                        obj.getString("username"),
                                        obj.getString("newPassword"), // <--- Read password
                                        obj.getString("requestDate")
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleApproveAction(PasswordRequest req) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/password-requests/" + req.getId() + "/approve"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            requests.remove(req);
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper Class
    public static class PasswordRequest {
        private int id;
        private String username;
        private String newPassword;
        private String requestDate;

        public PasswordRequest(int id, String username, String newPassword, String requestDate) {
            this.id = id;
            this.username = username;
            this.newPassword = newPassword;
            this.requestDate = requestDate;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getNewPassword() { return newPassword; }
        public String getRequestDate() { return requestDate; }
    }
}