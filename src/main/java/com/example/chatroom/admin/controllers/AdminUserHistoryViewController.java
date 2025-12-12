package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminUserHistoryViewController {

    @FXML private TableView<UserHistory> historyTable;
    @FXML private TableColumn<UserHistory, String> colDate;
    @FXML private TableColumn<UserHistory, String> colIP;
    @FXML private Button btnClose;

    private ObservableList<UserHistory> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cài đặt cột cho bảng
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().time));
        colIP.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().ip));

        historyTable.setItems(dataList);
    }

    // Hàm này được gọi từ AdminUserViewController để nạp dữ liệu
    public void loadData(String username) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            // Gọi API lấy lịch sử của riêng user này
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + username + "/login-history"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            parseData(response.body());
                        }
                    }))
                    .exceptionally(e -> { e.printStackTrace(); return null; });

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void parseData(String jsonBody) {
        try {
            dataList.clear();
            JSONArray array = new JSONArray(jsonBody);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String rawTime = obj.getString("loginTime");
                String prettyTime = rawTime.replace("T", " ").split("\\.")[0];

                // Lấy IP (nếu chưa có dữ liệu cũ thì để trống)
                String ip = obj.optString("ipAddress", "Unknown");

                dataList.add(new UserHistory(prettyTime, ip));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    // Class dữ liệu cho bảng
    public static class UserHistory {
        String time, ip;
        public UserHistory(String time, String ip) {
            this.time = time;
            this.ip = ip;
        }
    }
}