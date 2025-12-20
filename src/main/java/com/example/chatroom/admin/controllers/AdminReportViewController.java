package com.example.chatroom.admin.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.core.utils.TableDataManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class AdminReportViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private SearchBarController searchBarController;
    @FXML private TableView<Report> reportTable;
    @FXML private TableColumn<Report, String> colTime, colReporter, colReported, colReason, colAction;

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> sortCombo;

    private ObservableList<Report> reportList = FXCollections.observableArrayList();
    private TableDataManager<Report> tableManager;

    @FXML
    public void initialize() {
        if (headerController != null) headerController.focusButton("spamReports");

        setupColumn(colTime, d -> d.time);
        setupColumn(colReporter, d -> d.reporter);
        setupColumn(colReported, d -> d.reported);
        setupColumn(colReason, d -> d.reason);
        setupActionColumn();

        tableManager = new TableDataManager<>(reportTable, reportList);

        tableManager.addSortOption("Time (Newest)", (r1, r2) -> r2.time.compareTo(r1.time));
        tableManager.addSortOption("Time (Oldest)", Comparator.comparing(r -> r.time));
        tableManager.addSortOption("Reporter (A-Z)", Comparator.comparing(r -> r.reporter.toLowerCase()));
        tableManager.setupSortController(sortCombo);
        sortCombo.getSelectionModel().select("Time (Newest)");

        if (searchBarController != null) {
            searchBarController.getSearchField().textProperty().addListener((o, ov, nv) -> updateTableFilters());
        }
        startDatePicker.valueProperty().addListener((o, ov, nv) -> updateTableFilters());
        endDatePicker.valueProperty().addListener((o, ov, nv) -> updateTableFilters());

        loadReports();
    }

    private void updateTableFilters() {
        String query = searchBarController != null ? searchBarController.getSearchField().getText().toLowerCase() : "";
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        tableManager.setFilterPredicate(report -> {
            boolean matchText = report.reporter.toLowerCase().contains(query)
                    || report.reported.toLowerCase().contains(query);

            boolean matchDate = true;
            if (start != null || end != null) {
                LocalDate reportDate = parseDate(report.time);
                if (reportDate != null) {
                    if (start != null && reportDate.isBefore(start)) matchDate = false;
                    if (end != null && reportDate.isAfter(end)) matchDate = false;
                }
            }
            return matchText && matchDate;
        });
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) return null;
            String cleanDate = dateStr.split("[ T]")[0];
            return LocalDate.parse(cleanDate);
        } catch (Exception e) {
            return null;
        }
    }

    private void setupActionColumn() {
        colAction.setCellFactory(new Callback<>() {
            @Override public TableCell<Report, String> call(TableColumn<Report, String> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); return; }

                        Report report = getTableView().getItems().get(getIndex());
                        HBox box = new HBox(10);
                        box.setAlignment(Pos.CENTER_LEFT); 

                        Button btnLock = new Button("Lock User");
                        btnLock.getStyleClass().add("admin-danger-button");
                        btnLock.setOnAction(e -> handleLock(report));

                        Button btnDismiss = new Button("Dismiss");
                        btnDismiss.getStyleClass().add("admin-action-button");
                        btnDismiss.setOnAction(e -> handleDismiss(report));

                        box.getChildren().addAll(btnLock, btnDismiss);

                        setAlignment(Pos.CENTER_LEFT);
                        setGraphic(box);
                    }
                };
            }
        });
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

    private void handleLock(Report report) { sendAction("/api/reports/" + report.id + "/lock", report); }
    private void handleDismiss(Report report) { sendAction("/api/reports/" + report.id, report); }

    private void sendAction(String endpoint, Report report) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + endpoint));

            if (!endpoint.endsWith("/lock")) builder.DELETE();
            else builder.POST(HttpRequest.BodyPublishers.noBody());

            client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            reportList.remove(report);
                            if (endpoint.endsWith("/lock")) new Alert(Alert.AlertType.INFORMATION, "User Locked.").show();
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupColumn(TableColumn<Report, String> column, Callback<Report, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
    }

    @FXML private void goToUsers(ActionEvent event) { /* ... */ }

    public static class Report {
        int id;
        String time, reporter, reported, reason;
        public Report(int id, String t, String r1, String r2, String re) {
            this.id = id; this.time = t; this.reporter = r1; this.reported = r2; this.reason = re;
        }
    }
}