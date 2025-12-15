package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminReportViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<Report> reportTable;
    @FXML private TableColumn<Report, String> colTime, colReporter, colReported, colReason, colAction;

    // Filters (Optional, keeping them empty/hidden for now to focus on core logic)
    @FXML private ComboBox<String> timeFilter, userFilter;

    private ObservableList<Report> reportList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        headerController.focusButton("spamReports");

        setupColumn(colTime, d -> d.time);
        setupColumn(colReporter, d -> d.reporter);
        setupColumn(colReported, d -> d.reported);
        setupColumn(colReason, d -> d.reason);

        // SETUP ACTIONS COLUMN
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Report, String> call(TableColumn<Report, String> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Report report = getTableView().getItems().get(getIndex());

                            HBox box = new HBox(10);

                            Button btnLock = new Button("Lock User");
                            btnLock.getStyleClass().add("admin-danger-button");
                            btnLock.setOnAction(e -> handleLock(report));

                            Button btnDismiss = new Button("Dismiss");
                            btnDismiss.getStyleClass().add("admin-action-button"); // Blue/Neutral button
                            btnDismiss.setOnAction(e -> handleDismiss(report));

                            box.getChildren().addAll(btnLock, btnDismiss);
                            setGraphic(box);
                        }
                    }
                };
            }
        });

        reportTable.setItems(reportList);
        loadReports();
    }

    private void loadReports() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/reports"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            reportList.clear();
                            JSONArray arr = new JSONArray(res.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                reportList.add(new Report(
                                        obj.getInt("id"),
                                        obj.getString("time"),
                                        obj.getString("reporter"),
                                        obj.getString("reported"),
                                        obj.getString("reason")
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleLock(Report report) {
        sendAction("/api/reports/" + report.id + "/lock", report);
    }

    private void handleDismiss(Report report) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/reports/" + report.id))
                    .DELETE()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> Platform.runLater(() -> {
                        if (res.statusCode() == 200) reportList.remove(report);
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendAction(String endpoint, Report report) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080" + endpoint))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            // If locked successfully, we usually remove the report too
                            reportList.remove(report);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "User Locked & Report Resolved.");
                            alert.show();
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupColumn(TableColumn<Report, String> column, Callback<Report, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
    }

    // Navigation (Keep as is)
    @FXML private void goToUsers(ActionEvent event) { /* ... */ }

    public static class Report {
        int id;
        String time, reporter, reported, reason;
        public Report(int id, String t, String r1, String r2, String re) {
            this.id = id; this.time = t; this.reporter = r1; this.reported = r2; this.reason = re;
        }
    }
}