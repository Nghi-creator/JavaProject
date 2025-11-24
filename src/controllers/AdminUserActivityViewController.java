package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class AdminUserActivityViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<ActiveUser> activeTable;
    @FXML private TableColumn<ActiveUser, String> colUsername, colFullname, colOpens, colPeople, colGroups, colCreated;

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> sortCombo, activityFilterCombo;
    @FXML private TextField activityValueField;

    @FXML private SearchBarController searchBarController;

    private ObservableList<ActiveUser> masterData;
    private FilteredList<ActiveUser> filteredData;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        headerController.focusButton("userActivity");

        setupColumns();
        setupCombos();
        loadDummyData();

        filteredData = new FilteredList<>(masterData, p -> true);

        bindFilters();

        SortedList<ActiveUser> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(activeTable.comparatorProperty());
        activeTable.setItems(sorted);
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
        col.setSortable(false);
        col.setReorderable(false);
    }

    private void setupCombos() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Name (A-Z)",
                "Created Date (Newest)"
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

    private void loadDummyData() {
        masterData = FXCollections.observableArrayList(
                new ActiveUser("john", "John Doe", 15, 4, 2, "2023-01-10"),
                new ActiveUser("jane", "Jane Smith", 25, 6, 1, "2023-03-22"),
                new ActiveUser("bobby", "Bobby G", 3, 1, 0, "2023-02-11")
        );
    }

    private void bindFilters() {
        searchBarController.searchField.textProperty().addListener((o, oldV, newV) -> applyFilters());
        startDatePicker.valueProperty().addListener((o, oldV, newV) -> applyFilters());
        endDatePicker.valueProperty().addListener((o, oldV, newV) -> applyFilters());
        activityFilterCombo.setOnAction(e -> applyFilters());
        activityValueField.textProperty().addListener((o, oldV, newV) -> applyFilters());
        sortCombo.setOnAction(e -> applySort());
    }

    private void applyFilters() {
        String search = searchBarController.searchField.getText() != null ?
                searchBarController.searchField.getText().toLowerCase() : "";

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String actFilter = activityFilterCombo.getValue();
        String actValueText = activityValueField.getText();

        Integer actValue = null;
        try { actValue = actValueText.isEmpty() ? null : Integer.parseInt(actValueText); }
        catch (Exception ignore) {}

        Integer finalActValue = actValue;

        filteredData.setPredicate(u -> {
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

    private void applySort() {
        if (sortCombo.getValue() == null) return;

        Comparator<ActiveUser> comp = switch (sortCombo.getValue()) {
            case "Name (A-Z)" -> Comparator.comparing(u -> u.fullname.toLowerCase());
            case "Created Date (Newest)" -> (u1, u2) -> u2.createdAt.compareTo(u1.createdAt);
            default -> null;
        };

        if (comp != null) FXCollections.sort(masterData, comp);
    }

    // ============ ActiveUser Model =============
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
