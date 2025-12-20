package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminLoginHistoryViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<History> historyTable;
    @FXML private TableColumn<History, String> colTime, colUsername, colFullname;
    @FXML private BorderPane rootPane; // Use this to hide header if needed

    private ObservableList<History> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Only setup header if we are in the main view (not a popup)
        if (headerController != null) {
            headerController.focusButton("loginHistory");
        }

        setupTable();

        // Default: Load all history (System View)
        // If opened as popup, this might be overwritten immediately by loadUserHistory
        loadData("/api/users/login-history");
    }

    // --- NEW: Method called when opening as a Popup for 1 user ---
    public void loadUserHistory(String username) {
        // 1. Hide the Admin Header (since it's a popup window)
        if (rootPane != null) {
            rootPane.setTop(null);
        }

        // 2. Add a "Close" button at the bottom for convenience
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("admin-action-button");
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());
        if (rootPane != null) {
            rootPane.setBottom(closeBtn);
        }

        // 3. Fetch specific data
        loadData("/api/users/" + username + "/login-history");
    }

    private void setupTable() {
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().time));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username));
        colFullname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullname));
        historyTable.setItems(masterData);
    }

    private void loadData(String endpoint) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080" + endpoint))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(this::parseAndPopulateTable)
                    .exceptionally(e -> { e.printStackTrace(); return null; });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void parseAndPopulateTable(String responseBody) {
        Platform.runLater(() -> {
            try {
                masterData.clear();
                JSONArray jsonArray = new JSONArray(responseBody);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String rawTime = obj.getString("loginTime");
                    String prettyTime = rawTime.replace("T", " ").split("\\.")[0];

                    masterData.add(new History(
                            prettyTime,
                            obj.getString("username"),
                            obj.getString("fullName")
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // Inner Class
    public static class History {
        String time, username, fullname;
        public History(String t, String u, String f) { this.time = t; this.username = u; this.fullname = f; }
    }
}