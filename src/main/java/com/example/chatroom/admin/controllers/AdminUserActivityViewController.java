package com.example.chatroom.admin.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AdminUserActivityViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<ActiveUser> activeTable;
    @FXML private TableColumn<ActiveUser, String> colUsername, colFullname, colOpens, colPeople, colGroups, colCreated;

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> sortCombo, activityFilterCombo;
    @FXML private TextField activityValueField;

    @FXML private SearchBarController searchBarController;

    private TableDataManager<ActiveUser> tableManager;

    private ObservableList<ActiveUser> masterData;
    private FilteredList<ActiveUser> filteredData;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        headerController.focusButton("userActivity");

        setupColumns();
        setupCombos();
        masterData = FXCollections.observableArrayList();

        tableManager = new TableDataManager<>(activeTable, masterData);

        tableManager.addSortOption("Name (A-Z)", Comparator.comparing(u -> u.fullname.toLowerCase()));
        tableManager.addSortOption("Name (Z-A)", Comparator.comparing((ActiveUser u) -> u.fullname.toLowerCase()).reversed());
        tableManager.addSortOption("Created Date (Newest)", (u1, u2) -> u2.createdAt.compareTo(u1.createdAt));
        tableManager.addSortOption("Created Date (Oldest)", (u1, u2) -> u1.createdAt.compareTo(u2.createdAt));
        tableManager.setupSortController(sortCombo);

        bindFilters();

        loadUserActivityData();

        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> loadUserActivityData());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> loadUserActivityData());
    }

    private void setupColumns() {
        setupColumn(colUsername, a -> a.username);
        setupColumn(colFullname, a -> a.fullname);
        setupColumn(colOpens, a -> String.valueOf(a.opens));
        setupColumn(colPeople, a -> String.valueOf(a.people));
        setupColumn(colGroups, a -> String.valueOf(a.groups));
        setupColumn(colCreated, a -> a.createdAt);
    }

    private void setupColumn(TableColumn<ActiveUser, String> col, java.util.function.Function<ActiveUser, String> extractor) {
        col.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        col.setReorderable(false);
    }

    private void setupCombos() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Name (A-Z)",
                "Name (Z-A)",
                "Created Date (Newest)",
                "Created Date (Oldest)"
        ));

        activityFilterCombo.setItems(FXCollections.observableArrayList(
                "Opens (=)",
                "Opens (>)",
                "Opens (<)",
                "People (=)",
                "People (>)",
                "People (<)",
                "Groups (=)",
                "Groups (>)",
                "Groups (<)"
        ));
    }

    private void loadUserActivityData() {
        LocalDate start = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusMonths(1);
        LocalDate end = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ConfigController.getServerIp()
                        + ":8080/api/users/activity?start=" + start + "&end=" + end))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> {
                            try {
                                JSONArray arr = new JSONArray(response.body());
                                masterData.clear();
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);
                                    masterData.add(new ActiveUser(
                                            obj.getString("username"),
                                            obj.getString("fullname"),
                                            obj.getInt("opens"),
                                            obj.getInt("people"),
                                            obj.getInt("groups"),
                                            obj.getString("createdAt")
                                    ));
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    }
                });
    }

    private void bindFilters() {
        searchBarController.getSearchField().textProperty().addListener((o, oldV, newV) -> applyFilters());
        activityFilterCombo.setOnAction(e -> applyFilters());
        activityValueField.textProperty().addListener((o, oldV, newV) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchBarController.getSearchField().getText() != null ?
                searchBarController.getSearchField().getText().toLowerCase() : "";

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String actFilter = activityFilterCombo.getValue();
        String actValueText = activityValueField.getText();

        Integer actValue = null;
        try { actValue = actValueText.isEmpty() ? null : Integer.parseInt(actValueText); }
        catch (Exception ignore) {}

        Integer finalActValue = actValue;

        tableManager.setFilterPredicate(u -> {
            boolean matchesSearch = u.username.toLowerCase().contains(search)
                    || u.fullname.toLowerCase().contains(search);

            LocalDate created = LocalDate.parse(u.createdAt, formatter);
            boolean matchesDate = true;
            if (start != null) matchesDate &= !created.isBefore(start);
            if (end != null) matchesDate &= !created.isAfter(end);

            boolean matchesActivity = true;
            if (actFilter != null && finalActValue != null) {
                int val = switch (actFilter.split(" ")[0]) {
                    case "Opens" -> u.opens;
                    case "People" -> u.people;
                    case "Groups" -> u.groups;
                    default -> 0;
                };
                if (actFilter.contains("(=)")) matchesActivity = val == finalActValue;
                else if (actFilter.contains("(>)")) matchesActivity = val > finalActValue;
                else if (actFilter.contains("(<)")) matchesActivity = val < finalActValue;
            }

            return matchesSearch && matchesDate && matchesActivity;
        });
    }

    public static class ActiveUser {
        String username, fullname, createdAt;
        int opens, people, groups;

        public ActiveUser(String u, String f, int o, int p, int g, String c) {
            username = u;
            fullname = f;
            opens = o;
            people = p;
            groups = g;
            createdAt = c;
        }
    }
}